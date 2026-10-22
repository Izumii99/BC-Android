### Fixes & Improvements:

- **Improvement (Memory & Performance):** Integrated automated texture cache maintenance directly into the Android bridge. Assets and textures browsed in the Wardrobe are gently pruned after 90 seconds, dramatically reducing memory pressure on mobile Unified Memory (RAM/VRAM).
- **Improvement (Anti-Crash & Post-Wardrobe Cleanup):** Added a 12-second safe delay after leaving Wardrobe before discarding unused clothing textures, completely preventing silent app terminations caused by the Android Low Memory Killer (LMK).
- **Fix (Rendering Stability):** WebGL canvas context and active character rendering loops are fully protected during room transitions without lockups or visual artifacts.
- **Feature (Ecosystem Parity):** Synchronized texture retention and memory lifecycle directly with BC-Desktop v7.3.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting:** Go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
