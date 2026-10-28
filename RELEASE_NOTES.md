### Fixes & Improvements:

- **Performance (Fix R133 ImageCache Per-Frame Error Allocations):** Integrated a fast-path cache guard for Bondage Club R133's image cache engine (`ImageCache.prototype.get`), eliminating over 1,500 redundant `new Error()` stack-trace captures and Promise allocations per second on already-cached assets during 2D canvas drawing.
- **Performance (Fix R133 Top Menu DOM Attribute Thrashing):** Bypassed redundant `img.src` attribute writes in `ElementButton.SetImage` during per-frame chatroom top-menu visual sync (`ChatRoomTopMenuSync`), preventing WebView Chromium Blink from re-parsing identical icon URLs and allocating Oilpan/CppGC objects 480 times per second.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
