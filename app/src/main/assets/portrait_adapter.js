/**
 * Bondage Club Android - Viewport & Responsive Centering Adapter
 * Ensures flexible canvas scaling (portrait & landscape) centered vertically and horizontally,
 * matching standard mobile browser behavior without gluing to the top notch.
 */
(function() {
    'use strict';

    // 1. Inject or update responsive viewport meta
    let meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
        meta = document.createElement('meta');
        meta.name = 'viewport';
        document.head.appendChild(meta);
    }
    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';

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

        /* Flexible Canvas Centering (Matches standard mobile browsers) */
        #MainCanvas {
            position: absolute !important;
            top: 0 !important;
            bottom: 0 !important;
            left: 0 !important;
            right: 0 !important;
            margin: auto !important;
            max-width: 100vw !important;
            max-height: 100vh !important;
            display: block !important;
            object-fit: contain !important;
        }

        /* Prevent chat input from hiding under Android IME virtual keyboard */
        #chat-room-div, #chat-room-main {
            box-sizing: border-box !important;
            max-width: 100vw !important;
        }

        #InputChat {
            font-size: 16px !important; /* Prevents auto-zoom on iOS/Android WebViews */
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

    // 3. Dynamic layout observer and canvas scaler (centers in portrait and landscape)
    function adjustCanvasLayout() {
        const canvas = document.getElementById('MainCanvas');
        if (!canvas) return;

        // Check if an external portrait addon is active
        const hasPortraitAddon = window.BCPortraitMode || 
            window.bcPortraitActive || 
            document.querySelector('.portrait-mode') || 
            document.querySelector('#portrait-wrapper');

        if (hasPortraitAddon) {
            // Respect user installed portrait addon layout
            return;
        }

        // Maintain browser-standard auto centering
        canvas.style.position = 'absolute';
        canvas.style.top = '0';
        canvas.style.bottom = '0';
        canvas.style.left = '0';
        canvas.style.right = '0';
        canvas.style.margin = 'auto';

        const screenWidth = window.innerWidth;
        const screenHeight = window.innerHeight;
        const nativeAspect = 2.0; // Bondage Club native 2:1 aspect ratio

        if (screenWidth / screenHeight < nativeAspect) {
            // Screen is portrait or narrower than 2:1 -> width bound, centered vertically
            const targetHeight = screenWidth / nativeAspect;
            canvas.style.width = screenWidth + 'px';
            canvas.style.height = targetHeight + 'px';
        } else {
            // Screen is landscape or wider than 2:1 -> height bound, centered horizontally
            const targetWidth = screenHeight * nativeAspect;
            canvas.style.width = targetWidth + 'px';
            canvas.style.height = screenHeight + 'px';
        }
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

    window.addEventListener('resize', adjustCanvasLayout);
    window.addEventListener('orientationchange', adjustCanvasLayout);
    if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', adjustCanvasLayout);
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

    const canvasCheckInterval = setInterval(function() {
        if (document.getElementById('MainCanvas')) {
            adjustCanvasLayout();
            adaptPromptDialogs();
            clearInterval(canvasCheckInterval);
        }
    }, 250);

    // Watch for dynamic DOM modals or error alerts
    const domObserver = new MutationObserver(function() {
        adaptPromptDialogs();
    });
    const targetNode = document.body || document.documentElement;
    if (targetNode) {
        domObserver.observe(targetNode, { childList: true, subtree: true });
    }
    setInterval(adaptPromptDialogs, 1000);

})();
