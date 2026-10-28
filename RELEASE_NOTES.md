### Fixes & Improvements:

- **Procedural Canvas Deferral:** Deferred heavy multi-layer character canvas recomposition (`CharacterAppearanceBuildCanvas`) while in split-screen background or stopped states, eliminating CPU and battery spikes caused by continuous mod animations (e.g., tail wagging) and automatic room activities.
- **Seamless Multi-Window & Floating Pop-Up Experience:** Both accounts in split-screen or floating pop-up windows now remain fully visible and responsive. The inactive window automatically throttles to ~15 FPS upon launch without blacking out the canvas or hiding chat logs, allowing players to read messages and observe rooms on both accounts simultaneously with minimal battery drain.
- **Zero-Drain True Background Minimization:** When the app is minimized to the home screen or the device is locked, game rendering is completely paused (0 FPS) and canvas resources are cleared, eliminating background CPU and GPU power drain.
- **Addon Stability & Hook Protection:** Reinforced the game rendering pipeline to prevent third-party mods and visual addons from losing their hooks during app switching, split-screen transitions, or window resizing.
- **Unfocused Resource Optimization:** Window focus is properly isolated during multi-window play, allowing background canvas animations and addon tick loops to sleep smoothly when an account is not actively being touched.
- **Garbage Collection Throttling:** Regulated internal memory cleanup cycles to prevent micro-stutters during rapid multitasking and app-switching.
- **Streamlined Startup:** Removed automatic system battery optimization prompts on launch so players can jump straight into rooms without unnecessary dialogs.
- **24/7 Persistent Socket Keep-Alive:** Enhanced Wi-Fi and mobile data radio management, preventing socket dropouts when switching between Wi-Fi and cellular networks or during prolonged background idling.
- **Silent Background Session:** Eliminated status bar notification clutter for background keep-alive, keeping your notification shade clean and distraction-free.
- **Portrait Layout Compatibility:** Synchronized rendering buffers to ensure smooth performance with portrait-mode UI mods (such as BC-LCE) and wardrobe asset loading.
- **In-App What's New Dialog:** Players are now automatically presented with the latest release notes directly inside the app after updating.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.

