/**
 * Bondage Club Android - Desktop Bridge
 * Ports smart focus, clipboard bridge, and event handling from BC Desktop.
 */
(function() {
    'use strict';

    // Guard against undefined pidx ReferenceError
    if (typeof window.pidx === 'undefined') {
        try {
            Object.defineProperty(window, 'pidx', { get: () => 0, set: () => {}, configurable: true });
        } catch (e) {}
    }

    // Clipboard bridge to Android native ClipboardManager
    const origClipboard = navigator.clipboard;
    const mockClipboard = {
        writeText: function(text) {
            if (window.AndroidBridge && window.AndroidBridge.copyText) {
                window.AndroidBridge.copyText(text);
            }
            return Promise.resolve();
        },
        readText: function() {
            if (origClipboard && origClipboard.readText) {
                return origClipboard.readText();
            }
            return Promise.resolve('');
        }
    };

    try {
        Object.defineProperty(navigator, 'clipboard', {
            value: mockClipboard,
            configurable: true
        });
    } catch (e) {}

    const originalExecCommand = document.execCommand;
    document.execCommand = function(commandId, showUI, value) {
        if (commandId && commandId.toLowerCase() === 'copy') {
            let text = '';
            const activeEl = document.activeElement;
            if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
                try {
                    if (activeEl.selectionStart !== null && activeEl.selectionEnd !== null) {
                        text = activeEl.value.substring(activeEl.selectionStart, activeEl.selectionEnd);
                    } else {
                        text = activeEl.value;
                    }
                } catch (err) {
                    text = activeEl.value;
                }
            } else {
                text = window.getSelection().toString();
            }
            if (text && window.AndroidBridge && window.AndroidBridge.copyText) {
                window.AndroidBridge.copyText(text);
            }
        }
        return originalExecCommand.apply(this, arguments);
    };

    // Helper to identify Bondage Club internal game domains
    function isGameDomain(url) {
        if (!url || typeof url !== 'string') return false;
        const clean = url.trim().toLowerCase();
        return clean.indexOf("bondage-asia.com") !== -1 ||
               clean.indexOf("bondageprojects.com") !== -1 ||
               clean.indexOf("bondageprojects.elementfx.com") !== -1 ||
               clean.indexOf("bondage-europe.com") !== -1;
    }

    // Intercept window.open to open external links with Android app chooser ("Open with:")
    const origWindowOpen = window.open;
    window.open = function(url, target, features) {
        if (url && typeof url === 'string') {
            if (!isGameDomain(url) && window.AndroidBridge && typeof window.AndroidBridge.openExternalUrl === 'function') {
                window.AndroidBridge.openExternalUrl(url);
                return null;
            }
        }
        return origWindowOpen ? origWindowOpen.apply(this, arguments) : null;
    };

    // Defer URL.revokeObjectURL for 3 seconds to allow asynchronous fetch(blobUrl) to read blob data
    if (window.URL && typeof window.URL.revokeObjectURL === 'function') {
        const origRevokeObjectURL = window.URL.revokeObjectURL;
        window.URL.revokeObjectURL = function(url) {
            setTimeout(function() {
                try {
                    origRevokeObjectURL.call(window.URL, url);
                } catch (e) {}
            }, 3000);
        };
    }

    // Intercept download requests (blob:, data:, and <a download>)
    function handleDownloadUrl(url, filename, mimeType) {
        if (!url || typeof url !== 'string') return false;

        // 1. Blob URL: fetch blob inside web context, convert to Base64, and pass to native bridge
        if (url.startsWith('blob:')) {
            fetch(url)
                .then(function(res) { return res.blob(); })
                .then(function(blob) {
                    const reader = new FileReader();
                    reader.onloadend = function() {
                        if (window.AndroidBridge && typeof window.AndroidBridge.saveBase64File === 'function') {
                            window.AndroidBridge.saveBase64File(reader.result, filename || 'download', blob.type || mimeType || '');
                        }
                    };
                    reader.readAsDataURL(blob);
                })
                .catch(function(err) {
                    console.error("Failed to read blob for download", err);
                });
            return true;
        }

        // 2. Data URL: directly pass to native bridge
        if (url.startsWith('data:')) {
            if (window.AndroidBridge && typeof window.AndroidBridge.saveBase64File === 'function') {
                window.AndroidBridge.saveBase64File(url, filename || 'download', mimeType || '');
            }
            return true;
        }

        // 3. HTTP / HTTPS with explicit download attribute:
        if (filename && (url.startsWith('http://') || url.startsWith('https://'))) {
            if (window.AndroidBridge && typeof window.AndroidBridge.downloadHttpFile === 'function') {
                window.AndroidBridge.downloadHttpFile(url, filename, mimeType || '');
                return true;
            }
        }

        return false;
    }

    // Hook HTMLAnchorElement.prototype.click to catch programmatic downloads (e.g. wardrobe/log exports)
    const origAnchorClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function() {
        const href = this.href;
        const download = this.getAttribute('download');
        if (download !== null || (href && (href.startsWith('blob:') || href.startsWith('data:')))) {
            if (handleDownloadUrl(href, download || '', '')) {
                return;
            }
        }
        return origAnchorClick ? origAnchorClick.apply(this, arguments) : null;
    };

    // Intercept clicks on external <a> links to prevent unloading the game canvas
    document.addEventListener('click', function(e) {
        let el = e.target;
        while (el && el.tagName !== 'A') {
            el = el.parentElement;
        }
        if (el && el.href) {
            const href = el.href;
            const download = el.getAttribute('download');
            if (download !== null || href.startsWith('blob:') || href.startsWith('data:')) {
                if (handleDownloadUrl(href, download || '', '')) {
                    e.preventDefault();
                    e.stopPropagation();
                    return;
                }
            }

            if (!isGameDomain(href) && (href.startsWith('http://') || href.startsWith('https://'))) {
                e.preventDefault();
                e.stopPropagation();
                if (window.AndroidBridge && typeof window.AndroidBridge.openExternalUrl === 'function') {
                    window.AndroidBridge.openExternalUrl(href);
                }
            }
        }
    }, true);

    function isInteractive(el) {
        if (!el) return false;
        const tag = el.tagName;
        return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || tag === 'BUTTON' || el.isContentEditable;
    }

    // Hook window.ElementFocus so InputChat does not steal focus from addon search/text inputs
    let patchInterval = setInterval(function() {
        if (typeof window.ElementFocus === 'function' && !window.ElementFocus._isHooked) {
            const origElementFocus = window.ElementFocus;
            window.ElementFocus = function(ID) {
                const targetEl = typeof ID === 'string' ? document.getElementById(ID) : ID;
                const active = document.activeElement;
                const isUserInAnotherInput = active && isInteractive(active) && active !== targetEl;

                if (ID === 'InputChat' && isUserInAnotherInput) {
                    return;
                }
                origElementFocus(ID);
            };
            window.ElementFocus._isHooked = true;
            clearInterval(patchInterval);
        }
    }, 200);

    // Soft focus management: prevents canvas taps from unexpectedly losing input focus
    let origFocus = HTMLElement.prototype.focus;
    HTMLElement.prototype.focus = function() {
        try {
            const isLayeringInput = this.id && this.id.startsWith('layering-input-');
            const active = document.activeElement;
            const currentIsLayering = active && active.id && active.id.startsWith('layering-input-');
            if (currentIsLayering && !isLayeringInput) return;

            const isUserInAnotherInput = active && isInteractive(active) && active !== this;
            if (this.id === 'InputChat' && isUserInAnotherInput && !isLayeringInput) {
                return;
            }
        } catch (e) {}
        origFocus.call(this);
    };

    document.addEventListener('focusin', function(e) {
        if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) {
            window.bcFocusMemory = e.target;
        }
    }, true);

    // Provide focus crawler helper for Android
    window.bcRunFocusCrawler = function() {
        const inputs = document.querySelectorAll('input:not([type=hidden]), textarea');
        for (let i = 0; i < inputs.length; i++) {
            const el = inputs[i];
            if (!el.disabled && !el.readOnly && el.offsetParent !== null) {
                if (el.id !== 'InputChat' && el.id !== 'TextAreaChatLog') {
                    return el;
                }
            }
        }
        return document.getElementById('InputChat');
    };

    // Automatic Relog Handler (reconnects into the room instantly without manual password entry)
    function hookAutoRelogin() {
        const captureCredentials = function() {
            try {
                const accInput = document.getElementById('InputName');
                const passInput = document.getElementById('InputPassword');
                const acc = (accInput && accInput.value) ? accInput.value.trim() : (window.Player ? window.Player.AccountName : '');
                const pass = passInput ? passInput.value : '';
                if (acc && pass) {
                    sessionStorage.setItem('_bc_session_creds', JSON.stringify({ account: acc, pass: pass }));
                }
            } catch (e) {}
        };

        document.addEventListener('submit', captureCredentials, true);
        document.addEventListener('click', function(e) {
            if (e.target && (e.target.id === 'Login' || e.target.id === 'relog-login')) {
                captureCredentials();
            }
        }, true);

        let hookAttempts = 0;
        const hookInterval = setInterval(function() {
            hookAttempts++;
            if (typeof window.LoginDoLogin === 'function' && !window._bcLoginHooked) {
                window._bcLoginHooked = true;
                const origDoLogin = window.LoginDoLogin;
                window.LoginDoLogin = function(account, password) {
                    if (account && password) {
                        try {
                            sessionStorage.setItem('_bc_session_creds', JSON.stringify({ account: account, pass: password }));
                        } catch (e) {}
                    }
                    return origDoLogin.apply(this, arguments);
                };
                clearInterval(hookInterval);
            }
            if (hookAttempts > 30) clearInterval(hookInterval);
        }, 500);

        setInterval(function() {
            try {
                if (window.CurrentScreen === 'Relog' && !window._bcRelogActive) {
                    const raw = sessionStorage.getItem('_bc_session_creds');
                    if (raw) {
                        const creds = JSON.parse(raw);
                        if (creds && creds.pass) {
                            window._bcRelogActive = true;
                            const passInput = document.getElementById('InputPassword');
                            const loginBtn = document.getElementById('relog-login');
                            if (passInput) passInput.value = creds.pass;
                            if (loginBtn) {
                                loginBtn.click();
                            } else if (typeof window.LoginDoLogin === 'function') {
                                const acc = (window.Player && window.Player.AccountName) ? window.Player.AccountName : creds.account;
                                window.LoginDoLogin(acc, creds.pass);
                            }
                            setTimeout(function() {
                                window._bcRelogActive = false;
                            }, 4000);
                        }
                    }
                }
            } catch (e) {}
        }, 1000);
    }
    hookAutoRelogin();

    // Force Page Visibility API to report visible state permanently
    try {
        Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
        window.addEventListener('visibilitychange', function(e) {
            e.stopImmediatePropagation();
        }, true);
    } catch (e) {}

    // Silent WebAudio Keep-Alive to prevent Android & Chromium sandbox process freeze
    function initSilentAudioKeepAlive() {
        if (window._bcAudioKeepAliveActive) return;
        try {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (!AudioCtx) return;

            const ctx = new AudioCtx();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();

            gain.gain.value = 0.00001;
            osc.frequency.value = 1;
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();

            window._bcAudioCtx = ctx;
            window._bcAudioKeepAliveActive = true;

            setInterval(function() {
                if (window._bcAudioCtx && window._bcAudioCtx.state === 'suspended') {
                    window._bcAudioCtx.resume().catch(function() {});
                }
            }, 5000);
        } catch (e) {}
    }

    document.addEventListener('click', initSilentAudioKeepAlive, { once: true, passive: true });
    document.addEventListener('touchstart', initSilentAudioKeepAlive, { once: true, passive: true });
    initSilentAudioKeepAlive();

    // Suppress native Bondage Club fullscreen button (unnecessary in edge-to-edge immersive app)
    function suppressFullscreenButton() {
        try {
            if (window.Fullscreen && typeof window.Fullscreen.UpdateButton === 'function') {
                window.Fullscreen.Init = function() {};
            }
            const btn = document.getElementById('bc-fullscreen-button') || document.querySelector('.bc-fullscreen-button');
            if (btn) {
                btn.style.setProperty('display', 'none', 'important');
                btn.remove();
            }
        } catch (e) {}
    }
    suppressFullscreenButton();
    document.addEventListener('DOMContentLoaded', suppressFullscreenButton);
    setInterval(suppressFullscreenButton, 1000);
})();

