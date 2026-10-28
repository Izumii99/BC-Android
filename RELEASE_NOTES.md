### Fixes & Improvements:

- **Battery & Background Optimization:** Visual rendering and heavy canvas updates now pause cleanly whenever the app is minimized or the device is locked, eliminating background battery and CPU drain while keeping multiplayer room connections fully alive.
- **Smart Texture & Memory Lifecycle:** Safeguarded active room character textures against reload pauses and flickering, and tuned cache cleanups so memory from departed characters is swiftly freed without visual hitches.
- **Enhanced Split-Screen & Pop-Up Play:** Inactive split-screen windows now throttle rendering smoothly to conserve power while keeping chat and room characters visible and responsive.
- **Addon Stability & Visual Sync:** Hardened mod hook protection during app transitions and prevented background animation timers from running while unfocused.
- **Persistent Network Keep-Alive:** Maintained robust Wi-Fi and mobile data socket continuity to prevent accidental room disconnections during app switching.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
