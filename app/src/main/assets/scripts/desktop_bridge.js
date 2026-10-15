/**
 * Bondage Club Android - Desktop Bridge
 * Ports smart focus, clipboard bridge, and event handling from BC Desktop.
 */
(function() {
    'use strict';

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

    // Soft focus management: prevents canvas taps from unexpectedly losing input focus
    let origFocus = HTMLElement.prototype.focus;
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
})();
