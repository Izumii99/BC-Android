### Fixes & Improvements:

- **Portrait-First Zero-Bar Fullscreen:** Pure edge-to-edge immersive sticky mode. Completely eliminates browser chrome, URL bars, navigation buttons, and settings panels. Custom portrait adapter intelligently scales the native 2:1 game canvas to fit mobile phone screens without distortion or horizontal overflow, while seamlessly deferring to portrait addons if active.
- **Background Battery & RAM Optimization:** Engineered specifically for long-term sessions and idle backgrounding. When switching apps or locking your screen, graphics redraws automatically throttle to 15 FPS to cut CPU heat and battery drain. Inactive memory caches are actively trimmed via Android's `onTrimMemory` hooks while preserving your active room session, timers, and WebSocket connections.
- **Virtual Keyboard (IME) & Soft Focus:** Tapping chat boxes activates the keyboard cleanly with dynamic viewport resizing so inputs are never obstructed by the keyboard. Native Android clipboard bridge enables seamless 1-tap copying and pasting of chat text, appearance codes, and wardrobe exports.
- **Crash Auto-Recovery:** Built-in safeguard recovers and reloads frozen or memory-exhausted WebKit render processes with rapid crash-loop protection (up to 3 recoveries within 30 seconds).
- **Auto Game Version & Release Detection:** Automatically probes server endpoints concurrently on startup to load the latest live game version (R132+) without manual client updates. Silently checks GitHub for newer APK releases.
- **Double-Tap Back Guard:** Prevents accidental navigation gestures from closing your game session while in the middle of active gameplay or roleplay.

### Downloads:

- **BC-Android-Standard-Release.apk (~4MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~4MB):** Stealth build featuring an invisible app name and completely transparent icon for discrete homescreen placement.
