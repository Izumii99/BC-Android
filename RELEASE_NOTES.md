### Fixes & Improvements:

- **Branding & Visual Identity (Launcher Icons):** Standard client builds now display the official desktop rope coil insignia across adaptive launcher icons and ongoing status bar indicators, providing seamless visual consistency between desktop and mobile installations.
- **Stealth & Privacy Integrity (Discreet Mode):** The Discreet build features a completely invisible app label (`\u3164` Hangul Filler) paired with fully transparent adaptive icons and silent notification channels, ensuring homescreen placements remain completely concealed without visual markers.
- **Background Session Persistence:** Integrated a low-overhead connection maintenance service (`ConnectionKeeperService`) that keeps WebSocket room connections and game timers active during app-switching and device lock states, preventing accidental room disconnects on aggressive battery-management engines (such as MIUI / HyperOS and OneUI).
- **BC-LCE & Portrait Layout Responsiveness:** Enhanced dynamic viewport adaptation for extended canvas and portrait mods (including BC-LCE and external portrait engines), resolving UI coordinate shifts and ensuring natural touch targets across all screen ratios.
- **Background Battery & RAM Optimization:** Redraws automatically throttle to 15 FPS when the application is minimized or the screen is locked, significantly reducing CPU heat and battery consumption. Inactive memory caches are trimmed via Android `onTrimMemory` hooks without dropping your active room session.
- **Zero-Bar Immersive Fullscreen:** Pure edge-to-edge display eliminating browser chrome, URL bars, navigation buttons, and settings panels.
- **Virtual Keyboard (IME) Soft Focus:** Chat and input dialogs smoothly resize the viewport (`adjustResize`) so that text input boxes and chat logs are never obstructed by the soft keyboard.
- **Crash Auto-Recovery Safeguard:** Built-in watchdog smoothly recovers and reloads frozen WebKit render processes with crash-loop protection (up to 3 recoveries within 30 seconds).
- **Double-Tap Back Gesture Guard:** Protects against accidental navigation gestures closing your game session in the middle of active gameplay.
- **Remote Addon Loader Synchronization:** Automatically fetches and loads the latest Addon Manager and scripts directly from the desktop repository on startup, delivering immediate updates without requiring APK reinstallation.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.86 MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.85 MB):** Stealth build featuring an invisible app name and completely transparent icon for discrete homescreen placement.

