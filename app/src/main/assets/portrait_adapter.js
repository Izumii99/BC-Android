/**
 * Bondage Club Android - Portrait & Viewport Adapter
 * Ensures optimal canvas scaling, touch interaction, and zero-overflow layout in portrait orientation.
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

    // 2. Base CSS to prevent unwanted scrolls and overscroll bounce
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

        /* Portrait-specific canvas and layout normalization */
        @media screen and (orientation: portrait) {
            #MainCanvas {
                max-width: 100vw !important;
                height: auto !important;
                display: block !important;
                margin: 0 auto !important;
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
        }
    `;
    if (!document.getElementById('bc-android-portrait-style')) {
        document.head.appendChild(style);
    }

    // 3. Dynamic layout observer and canvas scaler
    function adjustPortraitLayout() {
        const isPortrait = window.innerHeight > window.innerWidth;
        const canvas = document.getElementById('MainCanvas');

        // Check if an external portrait addon is active
        const hasPortraitAddon = window.BCPortraitMode || 
            window.bcPortraitActive || 
            document.querySelector('.portrait-mode') || 
            document.querySelector('#portrait-wrapper');

        if (hasPortraitAddon) {
            // Respect user installed portrait addon
            return;
        }

        if (isPortrait && canvas) {
            const screenWidth = window.innerWidth;
            const screenHeight = window.innerHeight;

            // Bondage Club native internal coordinate system is typically 2000x1000 (2:1 aspect ratio)
            const nativeAspect = (canvas.width && canvas.height) ? (canvas.width / canvas.height) : 2.0;
            const targetHeight = screenWidth / nativeAspect;

            if (targetHeight <= screenHeight) {
                canvas.style.width = screenWidth + 'px';
                canvas.style.height = targetHeight + 'px';
            }
        }
    }

    window.addEventListener('resize', adjustPortraitLayout);
    if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', adjustPortraitLayout);
    }

    // Run adjustment after DOM load and when canvas becomes available
    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        adjustPortraitLayout();
    } else {
        document.addEventListener('DOMContentLoaded', adjustPortraitLayout);
    }

    const canvasCheckInterval = setInterval(function() {
        if (document.getElementById('MainCanvas')) {
            adjustPortraitLayout();
            clearInterval(canvasCheckInterval);
        }
    }, 250);

})();
