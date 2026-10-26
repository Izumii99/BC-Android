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
            -webkit-tap-highlight-color: transparent;
        }

        /* Prevent canvas selection artifacts while preserving game touch responses */
        #MainCanvas {
            user-select: none !important;
            -webkit-user-select: none !important;
            -webkit-touch-callout: none !important;
        }

        /* Full support for holding and dragging (blocking) chat text and inputs on Android */
        #TextAreaChatLog,
        #TextAreaChatLog *,
        .ChatMessage,
        .ChatMessage *,
        [class*="ChatMessage"],
        [class*="ChatMessage"] *,
        #chat-room-div,
        #chat-room-div *,
        #chat-room-main,
        #chat-room-main *,
        #InputChat,
        input,
        textarea,
        [contenteditable="true"],
        .selectable,
        .selectable * {
            user-select: text !important;
            -webkit-user-select: text !important;
            -webkit-touch-callout: default !important;
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

    `;
    if (!document.getElementById('bc-android-portrait-style')) {
        document.head.appendChild(style);
    }

    /**
     * Checks if BC-LCE, MPL, or any external portrait layout manager is active.
     */
    function isExternalPortraitActive() {
        // Screens where portrait layout is never active (Lobby, Login, etc.)
        if (window.CurrentScreen === 'MainHall' || window.CurrentScreen === 'Login' || window.CurrentScreen === 'Introduction') {
            return false;
        }

        // 1. Direct DOM check for BC-LCE vertical containers (Chatroom, Room Search, Dressing Room)
        if (
            document.getElementById('lce-v-cr') ||
            document.getElementById('lce-v-cs') ||
            document.getElementById('lce-v-csh') ||
            document.getElementById('lce-v-dr') ||
            document.querySelector('#lce-stage[data-orient="portrait"]') ||
            document.querySelector('.portrait-mode') ||
            document.querySelector('#portrait-wrapper') ||
            document.getElementById('mpl-style') ||
            document.body?.classList?.contains('bclce-portrait') ||
            (document.documentElement.style && document.documentElement.style.getPropertyValue('--lce-cr-top'))
        ) {
            return true;
        }

        // 2. Check BC-LCE runtime state
        const lceState = window.Liko?.LCE?.Vertical?.getState?.();
        if (lceState && lceState.active && lceState.mode && lceState.mode !== 'none') {
            return true;
        }

        // 3. Check legacy or third-party portrait addon global flags
        if (window.BCPortraitMode || window.bcPortraitActive || window.BCMPL || window.MPL) {
            return true;
        }

        return false;
    }

    // 3. Dynamic layout observer and canvas scaler (centers in portrait and landscape)
    function adjustCanvasLayout() {
        if (window._bcIsBackground) return;
        const portraitActive = isExternalPortraitActive();
        updateZoomState(portraitActive);

        const canvas = document.getElementById('MainCanvas');
        if (!canvas) return;

        // Never restrict width with max-width: it breaks portrait addons that use 2000px canvas with clip-path
        canvas.style.removeProperty('max-width');
        canvas.style.removeProperty('max-height');

        // If an external portrait addon is active (e.g. BC-LCE vertical mode), yield completely
        if (portraitActive) {
            delete canvas.dataset.bcAdapterActive;
            canvas.style.removeProperty('right');
            canvas.style.removeProperty('bottom');
            canvas.style.removeProperty('display');
            canvas.style.removeProperty('object-fit');

            // If BC-LCE vertical chatroom is active, guarantee responsive edge-to-edge scaling
            const isLceCr = document.getElementById('lce-v-cr') || (window.Liko?.LCE?.Vertical?.getState?.()?.mode === 'chatroom');
            if (isLceCr) {
                const vw = window.innerWidth;
                const vh = window.innerHeight;

                // When in portrait orientation (fullscreen phone or tall floating window):
                // Ensure the 1:1 character canvas dynamically expands to 100% width (no side black bars/pillarboxing),
                // while preserving at least minChatH for the chatroom controls and log below.
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
                const top = 0; // Seamlessly anchor to the top of window

                canvas.style.setProperty('position', 'fixed', 'important');
                canvas.style.setProperty('top', top + 'px', 'important');
                canvas.style.setProperty('left', left + 'px', 'important');
                canvas.style.setProperty('transform', 'none', 'important');
                canvas.style.setProperty('width', width + 'px', 'important');
                canvas.style.setProperty('height', height + 'px', 'important');
                canvas.style.setProperty('clip-path', 'inset(0 50% 0 0)', 'important');
                canvas.style.setProperty('margin', '0', 'important');
                canvas.style.setProperty('z-index', '1', 'important');

                // Keep BC-LCE's chatroom container directly docked beneath the character
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

        // Apply browser-standard auto centering for vanilla BC
        canvas.dataset.bcAdapterActive = 'true';
        canvas.style.removeProperty('clip-path');
        canvas.style.removeProperty('transform');
        canvas.style.setProperty('position', 'absolute', 'important');
        canvas.style.setProperty('top', '0', 'important');
        canvas.style.setProperty('bottom', '0', 'important');
        canvas.style.setProperty('left', '0', 'important');
        canvas.style.setProperty('right', '0', 'important');
        canvas.style.setProperty('margin', 'auto', 'important');
        canvas.style.setProperty('display', 'block', 'important');
        canvas.style.setProperty('object-fit', 'contain', 'important');

        const screenWidth = window.innerWidth;
        const screenHeight = window.innerHeight;
        const nativeAspect = 2.0; // Bondage Club native 2:1 aspect ratio

        let targetWidth, targetHeight;
        if (screenWidth / screenHeight < nativeAspect) {
            // Screen is portrait or narrower than 2:1 -> width bound, centered vertically
            targetWidth = screenWidth;
            targetHeight = screenWidth / nativeAspect;
        } else {
            // Screen is landscape or wider than 2:1 -> height bound, centered horizontally
            targetWidth = screenHeight * nativeAspect;
            targetHeight = screenHeight;
        }

        const targetWStr = targetWidth + 'px';
        const targetHStr = targetHeight + 'px';
        if (canvas.style.getPropertyValue('width') !== targetWStr) {
            canvas.style.setProperty('width', targetWStr, 'important');
        }
        if (canvas.style.getPropertyValue('height') !== targetHStr) {
            canvas.style.setProperty('height', targetHStr, 'important');
        }
    }

    // 4. Intercept and adapt desktop-proportioned banners, alerts, and recovery prompts
    function adaptPromptDialogs() {
        if (window._bcIsBackground) return;
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
        if (window._bcIsBackground) return;
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

    let safetyInterval = null;

    function startForegroundWatchers() {
        const targetNode = document.documentElement || document.body;
        if (targetNode) {
            try {
                domObserver.observe(targetNode, { childList: true, subtree: true });
            } catch (e) {}
        }
        if (!safetyInterval) {
            safetyInterval = setInterval(function() {
                if (window._bcIsBackground) return;
                adjustCanvasLayout();
                adaptPromptDialogs();
            }, 2500);
        }
    }

    function stopForegroundWatchers() {
        domObserver.disconnect();
        if (safetyInterval) {
            clearInterval(safetyInterval);
            safetyInterval = null;
        }
    }

    window.addEventListener('bc:visibility', function(e) {
        if (e.detail && e.detail.background) {
            stopForegroundWatchers();
        } else {
            startForegroundWatchers();
            scheduleLayoutUpdate();
        }
    });

    startForegroundWatchers();
})();
