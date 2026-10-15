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

    window.addEventListener('resize', adjustCanvasLayout);
    window.addEventListener('orientationchange', adjustCanvasLayout);
    if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', adjustCanvasLayout);
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        adjustCanvasLayout();
    } else {
        document.addEventListener('DOMContentLoaded', adjustCanvasLayout);
    }

    const canvasCheckInterval = setInterval(function() {
        if (document.getElementById('MainCanvas')) {
            adjustCanvasLayout();
            clearInterval(canvasCheckInterval);
        }
    }, 250);

})();
