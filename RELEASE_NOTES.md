### Fixes & Improvements:

- **Improvement (Wardrobe Memory & Texture Purging):** Overhauled image cache management to prevent out-of-memory crashes during extended Wardrobe and outfit creation sessions. Introduced a dynamic 90-second purge delay in Appearance mode and an automatic gentle cache flush 12 seconds after exiting back to the room.
- **Improvement (Battery & Background Optimization):** Texture purging and layout polling routines automatically sleep when the app is in the background or minimized, ensuring zero idle CPU and battery drain.
- **Improvement (Addon Synchronization):** Synchronized latest addon enhancements, focus guards, and hot-path CPU optimizations from upstream BC-Desktop.
- **Battery & CPU/GPU (Zero-Render Background):** Completely pauses canvas rendering and throttles frame updates to 1 FPS when minimized, keeping your device cool and drastically extending battery life during idle sessions.
- **Battery & Network (Connection Keeper Overhaul):** Relaxed background pulse intervals and eliminated redundant radio locks so your phone can sleep deeper while maintaining active room socket connections.
- **Hardware Power Savings (Audio Deactivation):** Deactivated background silent audio hardware loops to avoid unnecessary battery drain from device sound processors.
- **Performance & Background Throttling:** Added smart background pauses to viewport layout calculations and addon visibility checks, preventing background CPU overhead.
- **Clipboard & Outfit Import:** Fixed an issue where importing outfits, wardrobe presets, and addon configurations from your clipboard failed, restoring seamless 1-tap clipboard import across all screens.
- **Chat Text Selection (Blocking):** Restored native touch-and-drag text selection (blocking) and highlight handles across chat logs and messages, allowing seamless copying and quoting.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting:** Go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
