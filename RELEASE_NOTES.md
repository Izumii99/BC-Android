### Fixes & Improvements:

- **Improvement (Smart Search Focus on Touch):** Tapping the search icon in item and wardrobe menus now automatically opens the keyboard so you can search immediately, while general menu navigation and applying restraints keep the keyboard closed.
- **Fix (Screenshot Cleaner & Custom Backgrounds):** Custom room backgrounds and character interaction menus are now properly preserved when dialogs or item menus are open.
- **Improvement (Smart Outfit Layer Pruning & Lower RAM):** Temporary clothing layer textures and masks are now automatically released as soon as a character finishes rendering onto the room canvas, significantly reducing memory usage in crowded rooms.
- **Fix (Offline Profile Preview Crash):** Viewing profiles of offline friends or players outside your current room no longer triggers a canvas drawing error.
- **Fix (Typing in Chat & Frame Rate Drops):** Typing messages on the soft keyboard is now recognized as active input, keeping frame rates smooth while chatting.
- **Feature (Persist Map View):** Added a "Persist Map View" option in Chat QoL that remembers if Map View was open in a room and automatically reopens it when rejoining or relogging.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
