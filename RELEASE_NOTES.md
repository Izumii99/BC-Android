### Fixes & Improvements:

- **Feature (Smart Split-Screen & Unfocused Character Pre-Loading):** Players joining or changing outfits in a room now load and build their character appearance while in split-screen or unfocused mode, ensuring characters are already visible without pop-in delays.
- **Improvement (Activity-Aware Background Animation Burst):** Automatic and manual JSON-driven animations from `BCAR`, `BC-LCE`, and `BC-HSC` now wake the unfocused renderer into a temporary ~15 FPS burst only while active and automatically return to 1 FPS deep-idle mode afterward, preserving low RAM, CPU, and GPU usage with full anti-leak protection.
- **Improvement (Smoother Foreground Animations):** Raised foreground character canvas animation responsiveness to 30+ FPS for fluid multi-slot expressions and poses.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
