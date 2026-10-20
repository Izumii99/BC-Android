### Fixes & Improvements:

- **Persistent Whispers:** Private chat whispers (`/w`) now persist automatically across messages until cancelled (or cleared with Escape), with expanded emoticons and refined room chat interactions.
- **Isolated Translation UI:** Upgraded the translation widget into an isolated Shadow DOM container with automatic language detection, real-time spellcheck hints, and boundary-clamped dragging.
- **Clutter-Free Edge-to-Edge:** Automatically suppressed redundant in-game fullscreen overlay buttons for a cleaner view on mobile zero-bar immersive displays.
- **Addon Ecosystem Sync:** Refreshed offline fallback addon bundles synchronized directly with the latest upstream BC-Desktop distribution.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting:** Go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.

