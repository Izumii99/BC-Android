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
            if (window.AndroidBridge && typeof window.AndroidBridge.copyText === 'function') {
                window.AndroidBridge.copyText(text);
            }
            return Promise.resolve();
        },
        readText: function() {
            if (window.AndroidBridge && typeof window.AndroidBridge.getClipboardText === 'function') {
                try {
                    const text = window.AndroidBridge.getClipboardText();
                    return Promise.resolve(text || '');
                } catch (e) {
                    console.error("AndroidBridge.getClipboardText failed", e);
                }
            } else if (window.AndroidBridge && typeof window.AndroidBridge.readText === 'function') {
                try {
                    const text = window.AndroidBridge.readText();
                    return Promise.resolve(text || '');
                } catch (e) {
                    console.error("AndroidBridge.readText failed", e);
                }
            }
            if (origClipboard && typeof origClipboard.readText === 'function') {
                return origClipboard.readText().catch(function() {
                    return '';
                });
            }
            return Promise.resolve('');
        }
    };

    try {
        Object.defineProperty(navigator, 'clipboard', {
            value: mockClipboard,
            configurable: true,
            writable: true
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
        } else if (commandId && commandId.toLowerCase() === 'paste') {
            if (window.AndroidBridge && typeof window.AndroidBridge.getClipboardText === 'function') {
                try {
                    const text = window.AndroidBridge.getClipboardText();
                    if (text) {
                        const activeEl = document.activeElement;
                        if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
                            const start = typeof activeEl.selectionStart === 'number' ? activeEl.selectionStart : activeEl.value.length;
                            const end = typeof activeEl.selectionEnd === 'number' ? activeEl.selectionEnd : activeEl.value.length;
                            const val = activeEl.value || '';
                            activeEl.value = val.substring(0, start) + text + val.substring(end);
                            activeEl.selectionStart = activeEl.selectionEnd = start + text.length;
                            activeEl.dispatchEvent(new Event('input', { bubbles: true }));
                            activeEl.dispatchEvent(new Event('change', { bubbles: true }));
                            return true;
                        } else {
                            try {
                                const dt = new DataTransfer();
                                dt.setData('text/plain', text);
                                const pasteEvt = new ClipboardEvent('paste', {
                                    bubbles: true,
                                    cancelable: true,
                                    clipboardData: dt
                                });
                                (activeEl || document).dispatchEvent(pasteEvt);
                                return true;
                            } catch (e) {}
                        }
                    }
                } catch (e) {}
            }
        }
        return originalExecCommand.apply(this, arguments);
    };

    // Keyboard Ctrl+V / Cmd+V listener to dispatch paste ClipboardEvent across the canvas and active screen
    window.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && (e.key === 'v' || e.key === 'V' || e.code === 'KeyV')) {
            const activeEl = document.activeElement;
            const isInput = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');
            if (!isInput && window.AndroidBridge && typeof window.AndroidBridge.getClipboardText === 'function') {
                try {
                    const text = window.AndroidBridge.getClipboardText();
                    if (text) {
                        const dt = new DataTransfer();
                        dt.setData('text/plain', text);
                        const pasteEvt = new ClipboardEvent('paste', {
                            bubbles: true,
                            cancelable: true,
                            clipboardData: dt
                        });
                        (activeEl || document).dispatchEvent(pasteEvt);
                    }
                } catch (e) {}
            }
        }
    }, true);

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
            fetch(url) // nosemgrep
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

        let relogInterval = null;
        function checkAutoRelog() {
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
        }

        // Expose globally for Android pulse invocations
        window._bcCheckAutoRelog = checkAutoRelog;

        function startRelogWatcher(intervalMs) {
            if (relogInterval) clearInterval(relogInterval);
            relogInterval = setInterval(checkAutoRelog, intervalMs || 1000);
        }

        startRelogWatcher(1000);

        window.addEventListener('bc:visibility', function(e) {
            if (e.detail && e.detail.background) {
                startRelogWatcher(15000);
            } else {
                startRelogWatcher(1000);
            }
        });
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

    // Silent WebAudio Keep-Alive stub (active flag stubbed to avoid activating hardware audio)
    function initSilentAudioKeepAlive() {
        if (window._bcAudioKeepAliveActive) return;
        window._bcAudioKeepAliveActive = true;
    }

    document.addEventListener('click', initSilentAudioKeepAlive, { once: true, passive: true });
    document.addEventListener('touchstart', initSilentAudioKeepAlive, { once: true, passive: true });
    initSilentAudioKeepAlive();

    // Suppress native Bondage Club fullscreen button (unnecessary in edge-to-edge immersive app)
    let suppressFullscreenInterval = null;
    let suppressFullscreenAttempts = 0;
    function suppressFullscreenButton() {
        suppressFullscreenAttempts++;
        try {
            if (window.Fullscreen && typeof window.Fullscreen.UpdateButton === 'function') {
                window.Fullscreen.Init = function() {};
            }
            const btn = document.getElementById('bc-fullscreen-button') || document.querySelector('.bc-fullscreen-button');
            if (btn || suppressFullscreenAttempts > 15) {
                if (btn) {
                    btn.style.setProperty('display', 'none', 'important');
                    btn.remove();
                }
                if (suppressFullscreenInterval) {
                    clearInterval(suppressFullscreenInterval);
                    suppressFullscreenInterval = null;
                }
            }
        } catch (e) {}
    }
    suppressFullscreenButton();
    document.addEventListener('DOMContentLoaded', suppressFullscreenButton);
    suppressFullscreenInterval = setInterval(suppressFullscreenButton, 1000);

    // Safe VRAM & texture optimization via BC's native ImageCache
    (function setupSafeVramOptimization() {
        const NORMAL_PURGE_DELAY = 3 * 60 * 1000;
        const WARDROBE_PURGE_DELAY = 90 * 1000;

        function applyPurgeDelay() {
            if (typeof window.ImageCachePurgeDelay !== 'undefined') {
                window.ImageCachePurgeDelay = (typeof window.CurrentScreen !== 'undefined' && window.CurrentScreen === 'Appearance')
                    ? WARDROBE_PURGE_DELAY
                    : NORMAL_PURGE_DELAY;
            }
        }

        function cleanUpDepartedCharacter(targetChar) {
            if (!targetChar || targetChar === window.Player || (targetChar.IsPlayer && targetChar.IsPlayer())) return;
            try {
                const memberNum = targetChar.MemberNumber;
                if (window.CurrentCharacter === targetChar) {
                    if (typeof window.DialogLeave === 'function') {
                        try { window.DialogLeave(); } catch (e) {}
                    }
                    window.CurrentCharacter = null;
                }
                if (memberNum && window._chatQol_PetsuitRemoteAnims) {
                    if (typeof window._chatQol_PetsuitRemoteAnims.delete === 'function' && window._chatQol_PetsuitRemoteAnims.has(memberNum)) {
                        const entry = window._chatQol_PetsuitRemoteAnims.get(memberNum);
                        if (entry && entry.timer) clearTimeout(entry.timer);
                        window._chatQol_PetsuitRemoteAnims.delete(memberNum);
                    } else if (window._chatQol_PetsuitRemoteAnims[memberNum]) {
                        clearInterval(window._chatQol_PetsuitRemoteAnims[memberNum]);
                        delete window._chatQol_PetsuitRemoteAnims[memberNum];
                    }
                }
                targetChar.MustDraw = false;
                if (targetChar.Canvas) {
                    targetChar.Canvas.width = 0;
                    targetChar.Canvas.height = 0;
                    targetChar.Canvas = null;
                }
                if (targetChar.CanvasBlink) {
                    targetChar.CanvasBlink.width = 0;
                    targetChar.CanvasBlink.height = 0;
                    targetChar.CanvasBlink = null;
                }
                if (typeof window.AnimationPurge === 'function') {
                    try { window.AnimationPurge(targetChar, true); } catch (e) {}
                }
                if (typeof window.CharacterDelete === 'function') {
                    try { window.CharacterDelete(targetChar); } catch (e) {}
                } else if (Array.isArray(window.Character)) {
                    const idx = window.Character.indexOf(targetChar);
                    if (idx >= 0) window.Character.splice(idx, 1);
                }
            } catch (e) {}
        }

        function attachRoomHooks() {
            if (typeof window.ChatRoomSyncMemberLeave === 'function' && !window.__bcMemberLeaveHooked) {
                window.__bcMemberLeaveHooked = true;
                const _origLeave = window.ChatRoomSyncMemberLeave;
                window.ChatRoomSyncMemberLeave = function(data) {
                    const targetId = data && typeof data.SourceMemberNumber === 'number' ? data.SourceMemberNumber : null;
                    const char = targetId != null
                        ? ((window.ChatRoomCharacter || []).find(c => c && c.MemberNumber === targetId) ||
                           (window.Character || []).find(c => c && c.MemberNumber === targetId))
                        : null;
                    try {
                        return _origLeave.apply(this, arguments);
                    } finally {
                        if (char) cleanUpDepartedCharacter(char);
                    }
                };
            }
            if (typeof window.ChatRoomLeave === 'function' && !window.__bcRoomLeaveHooked) {
                window.__bcRoomLeaveHooked = true;
                const _origRoomLeave = window.ChatRoomLeave;
                window.ChatRoomLeave = function(clearCharacters) {
                    return _origRoomLeave.call(this, true);
                };
            }
        }
        attachRoomHooks();

        let vramPruneInterval = null;
        let screenMonitorInterval = null;
        let lastScreen = null;
        let screenTimer = null;

        function pruneVram() {
            try {
                if (window._bcIsBackground) return;
                attachRoomHooks();
                applyPurgeDelay();

                // Scavenger: remove any orphaned ghost characters in window.Character who are not in the room
                if (Array.isArray(window.Character)) {
                    const inChatRoom = (typeof window.ServerPlayerIsInChatRoom === 'function' && window.ServerPlayerIsInChatRoom()) ||
                                       window.CurrentScreen === 'ChatRoom' ||
                                       (window.ChatRoomData != null && Array.isArray(window.ChatRoomCharacter) && window.ChatRoomCharacter.length > 0);

                    const activeMemberNums = new Set();
                    if (window.Player && window.Player.MemberNumber) activeMemberNums.add(window.Player.MemberNumber);
                    if (Array.isArray(window.ChatRoomCharacter)) {
                        for (let i = 0; i < window.ChatRoomCharacter.length; i++) {
                            const m = window.ChatRoomCharacter[i]?.MemberNumber;
                            if (m) activeMemberNums.add(m);
                        }
                    }

                    const ghosts = window.Character.filter(c => {
                        if (!c || c === window.Player || (c.IsPlayer && c.IsPlayer())) return false;
                        if (c === window.CurrentCharacter) return false;
                        const isOnline = (c.AccountName && c.AccountName.startsWith('Online-')) || (typeof c.MemberNumber === 'number' && c.MemberNumber > 0);
                        if (!isOnline) return false;
                        if (inChatRoom) {
                            return !activeMemberNums.has(c.MemberNumber);
                        }
                        if (window.CurrentScreen === 'ChatSearch' || window.CurrentScreen === 'MainHall') {
                            return true;
                        }
                        return false;
                    });

                    for (let i = 0; i < ghosts.length; i++) {
                        cleanUpDepartedCharacter(ghosts[i]);
                    }
                }

                if (typeof window.GLDrawImageCache !== 'undefined' && window.GLDrawImageCache && typeof window.GLDrawImageCache.purge === 'function') {
                    window.GLDrawImageCache.purge(false);
                }
                if (typeof window.DrawImageCache !== 'undefined' && window.DrawImageCache && typeof window.DrawImageCache.purge === 'function') {
                    window.DrawImageCache.purge(false);
                }
                if (typeof window.CurrentScreen !== 'undefined' && window.CurrentScreen === 'Appearance' && typeof window.gc === 'function') {
                    try { window.gc(); } catch (e) {}
                }
            } catch (e) {}
        }

        function monitorScreenTransitions() {
            try {
                if (window._bcIsBackground) return;
                applyPurgeDelay();
                if (typeof window.CurrentScreen !== 'undefined') {
                    if (lastScreen === 'Appearance' && window.CurrentScreen !== 'Appearance') {
                        if (screenTimer) clearTimeout(screenTimer);
                        screenTimer = setTimeout(function() {
                            if (window._bcIsBackground) return;
                            if (typeof window.CurrentScreen !== 'undefined' && window.CurrentScreen === 'Appearance') return;
                            if (typeof window.GLDrawImageCache !== 'undefined' && window.GLDrawImageCache && typeof window.GLDrawImageCache.purge === 'function') {
                                window.GLDrawImageCache.purge(true);
                            }
                            if (typeof window.DrawImageCache !== 'undefined' && window.DrawImageCache && typeof window.DrawImageCache.purge === 'function') {
                                window.DrawImageCache.purge(true);
                            }
                            if (typeof window.gc === 'function') {
                                try { window.gc(); } catch (e) {}
                            }
                        }, 12000);
                    }
                    lastScreen = window.CurrentScreen;
                }
            } catch (e) {}
        }

        function startVramWatchers() {
            if (!vramPruneInterval) {
                vramPruneInterval = setInterval(pruneVram, 45 * 1000);
            }
            if (!screenMonitorInterval) {
                screenMonitorInterval = setInterval(monitorScreenTransitions, 1000);
            }
        }

        function stopVramWatchers() {
            if (vramPruneInterval) {
                clearInterval(vramPruneInterval);
                vramPruneInterval = null;
            }
            if (screenMonitorInterval) {
                clearInterval(screenMonitorInterval);
                screenMonitorInterval = null;
            }
            if (screenTimer) {
                clearTimeout(screenTimer);
                screenTimer = null;
            }
        }

        startVramWatchers();

        window.addEventListener('bc:visibility', function(e) {
            if (e.detail && e.detail.background) {
                stopVramWatchers();
            } else {
                startVramWatchers();
            }
        });
    })();
})();

