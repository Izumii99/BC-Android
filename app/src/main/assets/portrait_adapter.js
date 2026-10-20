/**
 * Bondage Club Android - Viewport & Responsive Centering Adapter
 * Ensures flexible canvas scaling (portrait & landscape) centered vertically and horizontally,
 * matching standard mobile browser behavior without gluing to the top notch.
 */
(function() {
    'use strict';

    // 1. Dynamic Viewport & Zoom State Manager
    let currentZoomEnabled = null;

    function updateZoomState(portraitActive) {
        const shouldEnableZoom = !portraitActive;
        if (currentZoomEnabled === shouldEnableZoom) return;
        currentZoomEnabled = shouldEnableZoom;

        let meta = document.querySelector('meta[name="viewport"]');
        if (!meta) {
            meta = document.createElement('meta');
            meta.name = 'viewport';
            document.head.appendChild(meta);
        }

        if (shouldEnableZoom) {
            // Enable pinch-to-zoom when portrait mode is inactive (e.g. Lobby / 2:1 horizontal canvas)
            meta.content = 'width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=5.0, user-scalable=yes, viewport-fit=cover';
        } else {
            // Lock zoom when portrait mode is active (BC-LCE vertical chatroom or vertical room search UI)
            meta.content = 'width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';
            try {
                window.scrollTo(0, 0);
                if (document.documentElement) {
                    document.documentElement.scrollLeft = 0;
                    document.documentElement.scrollTop = 0;
                }
                if (document.body) {
                    document.body.scrollLeft = 0;
                    document.body.scrollTop = 0;
                }
            } catch (e) {}
        }

        if (window.AndroidBridge && typeof window.AndroidBridge.setZoomEnabled === 'function') {
            try {
                window.AndroidBridge.setZoomEnabled(shouldEnableZoom);
            } catch (e) {}
        }
    }

    // Initial zoom state evaluation
    updateZoomState(isExternalPortraitActive());

    // 2. Base CSS for flexible browser-style centering
    const style = document.createElement('style');
    style.id = 'bc-android-portrait-style';
    style.textContent = `
        html, body {
            width: 100% !important;
            height: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
            overflow: hidden !important;
            background-color: #0D0D0D !important;
            user-select: none;
            -webkit-user-select: none;
            -webkit-touch-callout: none;
            -webkit-tap-highlight-color: transparent;
        }

        /* Prevent chat input from hiding under Android IME virtual keyboard */
        #chat-room-div, #chat-room-main {
            box-sizing: border-box !important;
            max-width: 100vw !important;
        }

        #InputChat {
            font-size: 16px !important; /* Prevents auto-zoom on iOS/Android WebViews */
        }

        /* Suppress redundant fullscreen button overlay on Android zero-bar immersive display */
        #bc-fullscreen-button,
        .bc-fullscreen-button,
        [id*="fullscreen-button"],
        button[aria-label*="Fullscreen"],
        button[aria-label*="fullscreen"] {
            display: none !important;
            visibility: hidden !important;
            pointer-events: none !important;
            width: 0 !important;
            height: 0 !important;
            opacity: 0 !important;
            position: absolute !important;
            left: -9999px !important;
        }

        /* Addon Manager Mobile UI Fit */
        div[style*="zIndex: 1000000"], div[style*="z-index: 1000000"] {
            padding: 10px !important;
            box-sizing: border-box !important;
        }
        div[style*="zIndex: 1000000"] > div, div[style*="z-index: 1000000"] > div {
            max-width: 92vw !important;
            max-height: 80vh !important;
            width: 92vw !important;
        }
        div[style*="zIndex: 1000005"] > div, div[style*="z-index: 1000005"] > div {
            max-width: 90vw !important;
            max-height: 80vh !important;
        }

        /* Universal Constraints for Addon/Game Dialogs, Popups & Prompts */
        dialog,
        .modal,
        .dialog,
        div[role="dialog"],
        div[role="alertdialog"],
        div[class*="dialog"],
        div[class*="modal"],
        div[class*="popup"],
        div[class*="banner"] {
            max-width: 94vw !important;
            box-sizing: border-box !important;
        }

        dialog button,
        .modal button,
        .dialog button,
        div[role="dialog"] button,
        div[role="alertdialog"] button {
            min-height: 44px !important;
            padding: 10px 14px !important;
            font-size: 14px !important;
            box-sizing: border-box !important;
        }
    `;
    if (!document.getElementById('bc-android-portrait-style')) {
        document.head.appendChild(style);
    }

    /**
     * Checks if BC-LCE, MPL, or any external portrait layout manager is active.
     */
    function isExternalPortraitActive() {
        // 1. Jika sedang membuka menu karakter/item (CurrentCharacter != null) atau di luar ChatRoom/RoomSearch,
        // WAJIB kembali ke rasio 2:1 penuh agar menu kiri (Facial Expression) & kanan (Items/Dialog) tidak tumpang tindih!
        if (
            window.CurrentScreen === 'MainHall' ||
            window.CurrentScreen === 'Login' ||
            window.CurrentScreen === 'Introduction' ||
            window.CurrentScreen === 'Appearance' ||
            window.CurrentScreen === 'InformationSheet' ||
            (typeof window.CurrentCharacter !== 'undefined' && window.CurrentCharacter !== null)
        ) {
            return false;
        }

        // 2. Direct DOM check for BC-LCE vertical containers (hanya jika elemen benar-benar visible)
        const lceCr = document.getElementById('lce-v-cr');
        const lceCs = document.getElementById('lce-v-cs');
        const lceCsh = document.getElementById('lce-v-csh');
        const lceDr = document.getElementById('lce-v-dr');

        if (
            (lceCr && lceCr.offsetParent !== null) ||
            (lceCs && lceCs.offsetParent !== null) ||
            (lceCsh && lceCsh.offsetParent !== null) ||
            (lceDr && lceDr.offsetParent !== null) ||
            document.querySelector('#lce-stage[data-orient="portrait"]') ||
            document.querySelector('.portrait-mode') ||
            document.querySelector('#portrait-wrapper') ||
            document.getElementById('mpl-style') ||
            document.body?.classList?.contains('bclce-portrait')
        ) {
            return true;
        }

        // 3. Check BC-LCE runtime state
        const lceState = window.Liko?.LCE?.Vertical?.getState?.();
        if (lceState && lceState.active && lceState.mode && lceState.mode !== 'none') {
            return true;
        }

        // 4. Check legacy or third-party portrait addon global flags
        if (window.BCPortraitMode || window.bcPortraitActive || window.BCMPL || window.MPL) {
            return true;
        }

        return false;
    }

    // 3. Dynamic layout observer and canvas scaler (centers in portrait and landscape)
    function adjustCanvasLayout() {
        const portraitActive = isExternalPortraitActive();
        updateZoomState(portraitActive);

        const canvas = document.getElementById('MainCanvas');
        if (!canvas) return;

        // Pastikan atribut resolusi internal canvas (buffer drawing) TIDAK PERNAH berubah dari 2000x1000
        if (canvas.width !== 2000) canvas.width = 2000;
        if (canvas.height !== 1000) canvas.height = 1000;

        canvas.style.removeProperty('max-width');
        canvas.style.removeProperty('max-height');

        // Jika external portrait addon aktif DAN tidak sedang membuka menu karakter (CurrentCharacter == null)
        if (portraitActive) {
            delete canvas.dataset.bcAdapterActive;
            canvas.style.removeProperty('right');
            canvas.style.removeProperty('bottom');
            canvas.style.removeProperty('display');
            canvas.style.removeProperty('object-fit');

            const isLceCr = document.getElementById('lce-v-cr') || (window.Liko?.LCE?.Vertical?.getState?.()?.mode === 'chatroom');
            if (isLceCr) {
                const vw = window.innerWidth;
                const vh = window.innerHeight;

                let charSize;
                if (vh > vw) {
                    const minChatH = 180;
                    charSize = Math.min(vw, Math.max(120, vh - minChatH));
                } else {
                    charSize = Math.min(vw, Math.round(vh * 0.50));
                }

                const scale = charSize / 1000;
                const width = Math.round(2000 * scale);
                const height = Math.round(1000 * scale);
                const left = Math.round((vw - 1000 * scale) / 2);
                const top = 0;

                canvas.style.setProperty('position', 'fixed', 'important');
                canvas.style.setProperty('top', top + 'px', 'important');
                canvas.style.setProperty('left', left + 'px', 'important');
                canvas.style.setProperty('transform', 'none', 'important');
                canvas.style.setProperty('width', width + 'px', 'important');
                canvas.style.setProperty('height', height + 'px', 'important');
                canvas.style.setProperty('clip-path', 'inset(0 50% 0 0)', 'important');
                canvas.style.setProperty('margin', '0', 'important');
                canvas.style.setProperty('z-index', '1', 'important');

                const root = document.documentElement;
                const crTop = height;
                const crHeight = Math.max(0, vh - crTop);
                root.style.setProperty('--lce-cr-top', crTop + 'px');
                root.style.setProperty('--lce-cr-height', crHeight + 'px');

                const chatDiv = document.getElementById('chat-room-div');
                if (chatDiv) {
                    chatDiv.style.setProperty('top', crTop + 'px', 'important');
                    chatDiv.style.setProperty('height', crHeight + 'px', 'important');
                }
            }
            return;
        }

        // Mode Standar (2:1 seperti di Chrome):
        // Reset paksa clip-path dan transform agar tidak menyangkut dari state sebelumnya
        canvas.dataset.bcAdapterActive = 'true';
        canvas.style.setProperty('clip-path', 'none', 'important');
        canvas.style.setProperty('transform', 'none', 'important');
        canvas.style.setProperty('position', 'fixed', 'important');
        canvas.style.setProperty('margin', '0', 'important');
        canvas.style.setProperty('display', 'block', 'important');
        canvas.style.removeProperty('object-fit');
        canvas.style.removeProperty('right');
        canvas.style.removeProperty('bottom');

        const screenWidth = window.innerWidth;
        const screenHeight = window.innerHeight;
        const nativeAspect = 2.0; // Bondage Club native 2:1 (2000x1000)

        let targetWidth, targetHeight;
        if (screenWidth / screenHeight < nativeAspect) {
            targetWidth = screenWidth;
            targetHeight = Math.round(screenWidth / nativeAspect);
        } else {
            targetHeight = screenHeight;
            targetWidth = Math.round(screenHeight * nativeAspect);
        }

        const leftOffset = Math.round((screenWidth - targetWidth) / 2);
        const topOffset = Math.round((screenHeight - targetHeight) / 2);

        canvas.style.setProperty('width', targetWidth + 'px', 'important');
        canvas.style.setProperty('height', targetHeight + 'px', 'important');
        canvas.style.setProperty('left', leftOffset + 'px', 'important');
        canvas.style.setProperty('top', topOffset + 'px', 'important');
    }

    // 4. Intercept and adapt desktop-proportioned banners, alerts, and recovery prompts
    function adaptPromptDialogs() {
        const candidates = document.querySelectorAll(
            'body > div, body > table, div[style*="fixed"], div[style*="absolute"], div[class*="dialog"], div[class*="modal"]'
        );
        candidates.forEach(function(el) {
            if (el.id === 'MainCanvas' || el.id === 'chat-room-div' || el.id === 'chat-room-main' || el.id === 'bc-android-portrait-style') return;

            const text = (el.textContent || '').trim();
            const isErrorOrSkipPrompt = text.includes('skip loading addons') ||
                text.includes('Previous session had an error') ||
                (text.includes('Previous session') && text.includes('error'));

            if (isErrorOrSkipPrompt) {
                el.style.setProperty('position', 'fixed', 'important');
                el.style.setProperty('top', '24px', 'important');
                el.style.setProperty('left', '0', 'important');
                el.style.setProperty('right', '0', 'important');
                el.style.setProperty('margin', '0 auto', 'important');
                el.style.setProperty('width', 'min(500px, 92vw)', 'important');
                el.style.setProperty('max-width', '92vw', 'important');
                el.style.setProperty('box-sizing', 'border-box', 'important');
                el.style.setProperty('z-index', '10000005', 'important'); // Render cleanly above floating addon icons
                el.style.setProperty('background', '#1c1829', 'important');
                el.style.setProperty('color', '#f5f5f5', 'important');
                el.style.setProperty('border', '1px solid #7c3aed', 'important');
                el.style.setProperty('border-radius', '14px', 'important');
                el.style.setProperty('padding', '16px', 'important');
                el.style.setProperty('box-shadow', '0 12px 36px rgba(0,0,0,0.8)', 'important');
                el.style.setProperty('font-family', 'system-ui, -apple-system, sans-serif', 'important');
                el.style.setProperty('font-size', '14px', 'important');
                el.style.setProperty('line-height', '1.5', 'important');
                el.style.setProperty('text-align', 'center', 'important');

                // Adjust child tables to fluid layout
                const tables = el.querySelectorAll('table, tbody, tr, td');
                tables.forEach(function(t) {
                    t.style.setProperty('width', '100%', 'important');
                    t.style.setProperty('max-width', '100%', 'important');
                    t.style.setProperty('box-sizing', 'border-box', 'important');
                    t.style.setProperty('display', 'block', 'important');
                });

                // Arrange confirmation buttons side-by-side with touch targets
                const buttons = el.querySelectorAll('button, input[type="button"], input[type="submit"]');
                if (buttons.length > 0) {
                    const firstBtnParent = buttons[0].parentElement;
                    if (firstBtnParent) {
                        firstBtnParent.style.setProperty('display', 'flex', 'important');
                        firstBtnParent.style.setProperty('flex-direction', 'row', 'important');
                        firstBtnParent.style.setProperty('gap', '10px', 'important');
                        firstBtnParent.style.setProperty('width', '100%', 'important');
                        firstBtnParent.style.setProperty('margin-top', '14px', 'important');
                        firstBtnParent.style.setProperty('justify-content', 'center', 'important');
                    }
                    buttons.forEach(function(btn) {
                        btn.style.setProperty('flex', '1 1 0', 'important');
                        btn.style.setProperty('min-height', '44px', 'important');
                        btn.style.setProperty('min-width', '0', 'important');
                        btn.style.setProperty('padding', '10px 14px', 'important');
                        btn.style.setProperty('font-size', '15px', 'important');
                        btn.style.setProperty('font-weight', '600', 'important');
                        btn.style.setProperty('border-radius', '8px', 'important');
                        btn.style.setProperty('cursor', 'pointer', 'important');
                        btn.style.setProperty('box-sizing', 'border-box', 'important');
                        btn.style.setProperty('border', '1px solid #4c3a70', 'important');
                        btn.style.setProperty('background-color', '#2d2442', 'important');
                        btn.style.setProperty('color', '#ffffff', 'important');
                    });
                }
            }
        });
    }

    let layoutPending = false;
    function scheduleLayoutUpdate() {
        if (layoutPending) return;
        layoutPending = true;
        requestAnimationFrame(function() {
            layoutPending = false;
            adjustCanvasLayout();
            adaptPromptDialogs();
        });
    }

    window.addEventListener('resize', scheduleLayoutUpdate);
    window.addEventListener('orientationchange', scheduleLayoutUpdate);
    if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', scheduleLayoutUpdate);
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        adjustCanvasLayout();
        adaptPromptDialogs();
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            adjustCanvasLayout();
            adaptPromptDialogs();
        });
    }

    // Watch for dynamic DOM modals, error alerts, and addon style injections
    const domObserver = new MutationObserver(function() {
        scheduleLayoutUpdate();
    });
    const targetNode = document.documentElement || document.body;
    if (targetNode) {
        domObserver.observe(targetNode, { childList: true, subtree: true });
    }

    // Continuous safety heartbeat: ensures prompt adaptation and layout state stay in sync
    setInterval(function() {
        adjustCanvasLayout();
        adaptPromptDialogs();
    }, 500);

})();
