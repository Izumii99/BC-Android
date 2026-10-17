### Fixes & Improvements:

- **Feature (Server Redirection Chain Auto-Detection):** The game version updater now intelligently inspects active server directives, following automatic meta-redirections (such as the server migration from R132 to R133) so you are always loaded into the officially active game release without manual intervention or 404 landing pages.
- **Feature (Version Lock Safeguard):** Added support for locking specific game releases to safeguard against untested or breaking game changes when necessary.
- **Improvement (High-Availability Addon Loader):** Remote addon delivery has been upgraded with a three-tier fallback mechanism (GitHub Pages -> Raw GitHub -> jsDelivr CDN), ensuring addons and quality-of-life mods load instantly with near-zero latency even during CDN cache delays.
- **Improvement (R133 Addon Compatibility):** Updated all offline fallback addons (including Neko-Dark and Chat-QoL) with crash guards for the newly introduced immutable `bcModSdk` in Bondage Club R133.
- **Feature (Zero-Bar Fullscreen):** Pure edge-to-edge display with immersive sticky mode, eliminating browser chrome, URL bars, navigation buttons, and settings panels.
- **Feature (Context-Aware Pinch-to-Zoom):** Smooth pinch-to-zoom (up to 5x magnification) across standard landscape screens (such as the Lobby/Main Hall and non-portrait menus) for effortless readability of small buttons and text, with automatic zoom locking when entering vertical portrait rooms.
- **Feature (Native System File Chooser):** Full support for HTML `<input type="file">`, allowing direct import and export of appearance codes, wardrobe files, and custom backgrounds through your phone's native file picker (Recent, Downloads, and Gallery).
- **Feature (Discreet Stealth Flavor):** An optional private build featuring a minimal `"BC"` app label paired with a completely transparent launcher icon for discreet homescreen placement without package-name fallbacks.
- **Improvement (Background Session Persistence):** Integrated keep-alive service (`ConnectionKeeperService`) that maintains active WebSocket room connections and game timers during app-switching and device lock states, preventing accidental room disconnects.
- **Improvement (BC-LCE & Vertical Chatroom Layout):** Automatically anchors character views seamlessly above the chat box in portrait rooms without horizontal overflow or canvas displacement, dynamically adapting to full screen width.
- **Improvement (Battery & Memory Optimization):** Redraws automatically throttle to 15 FPS when minimized or locked to reduce CPU heat and battery drain, while trimming temporary caches via Android `onTrimMemory` hooks without dropping your active room session.
- **Improvement (Virtual Keyboard Auto-Resize):** Chat and input dialogs smoothly resize the viewport (`adjustResize`) so text input boxes and chat logs are never obstructed by the soft keyboard.
- **Fix (Accidental Room Exit):** Added double-tap confirmation on the Android back gesture to prevent accidentally leaving an active room session during gameplay.
- **Fix (WebKit Crash Auto-Recovery):** Built-in watchdog smoothly recovers and reloads frozen WebKit render processes with rapid crash-loop protection (up to 3 recoveries within 30 seconds).

### Downloads:

- **BC-Android-Standard-Release.apk (~0.88 MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.88 MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
