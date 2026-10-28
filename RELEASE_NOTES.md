### Fixes & Improvements:

- **Battery & Zero-Drain Background Minimization:** When the app is minimized to the home screen or your phone is locked, visual canvas rendering is completely paused (0 FPS), the canvas element is hidden from the GPU compositor, and animation timers are throttled to 0.25 FPS. This delivers maximum battery savings while keeping multiplayer WebSocket connections fully active.
- **Performance (Defer Procedural Canvas Rebuilds):** Deferred heavy character canvas recomposition while the app is in the background or running in inactive split-screen view. Continuous mod animations (such as tail wags and emote loops) no longer consume background CPU or drain battery, and characters refresh instantly in a single pass when you return to the app.
- **Smart Texture Protection & Memory Lifecycle:** Safeguarded active room character textures using direct layer tracking (`DrawnAssets`), completely eliminating texture flickering, blank avatars, and reload pauses. Unused assets from departed room members are cleanly reclaimed every 3 minutes.
- **Enhanced Split-Screen & Pop-Up Play:** Both accounts in split-screen or floating pop-up windows now remain fully visible and responsive. The inactive window automatically throttles to ~15 FPS to conserve power without blacking out the canvas or hiding chat logs, allowing you to observe both rooms effortlessly.
- **Addon Stability & Background Timer Guards:** Synchronized offline addon scripts (Chat QoL) to ignore incoming remote animation messages while in the background, eliminating redundant memory allocations and background script execution.
- **Persistent Network Keep-Alive:** Maintained robust Wi-Fi and mobile data radio continuity to prevent socket dropouts when switching between Wi-Fi and cellular networks or during prolonged background idling.
- **Silent Background Session:** Eliminated notification shade clutter for background keep-alive, keeping your status bar clean.
- **In-App What's New Dialog:** Players are now automatically presented with the latest release notes directly inside the app after updating.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
