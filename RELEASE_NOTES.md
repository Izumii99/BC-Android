### Fixes & Improvements:

- **Battery & CPU (Ultra-Low Background Power Profile):** Extended background frame interval to 4 seconds (0.25 FPS) and cut CPU wake time down to 500ms with streamlined 1-byte connection keepalives, allowing your phone to remain in deep sleep during long idle sessions.
- **Battery & GPU (DOM & Canvas Freezing):** Suspended canvas composition and layout calculation on incoming chat logs while minimized, keeping your device cool and preventing battery drain even in active, fast-moving chat rooms.
- **Background Resource Management:** Automatically disconnects DOM mutation observers and background interval timers when minimized, eliminating background JavaScript wakeups until you resume the app.
- **Notification Throttling & Silent Mode:** Chat message counters are batched with a 10-second debounce and delivered silently, preventing notification spam and CPU wakeups during busy room conversations.
- **Wardrobe Memory & Texture Management:** Safely prunes stale texture caches after extended appearance editing sessions to prevent out-of-memory crashes on devices with tight RAM budgets.
- **Clipboard & Import Reliability:** Seamless 1-tap clipboard import across all screens for wardrobe presets, outfits, and configuration strings.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting:** Go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
