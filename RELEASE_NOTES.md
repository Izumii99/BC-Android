### Fixes & Improvements:

- **Feature (Auto-Focus Text / Keyboard Toggle - Off by Default):** Disabled automatic text input focus on Android by default so applying restraints from the Item menu, searching items, or closing character dialogs no longer pops up the virtual keyboard or shifts the game screen position unless you directly tap a text input box. You can toggle this behavior anytime via the Login screen settings button or in the in-game QoL settings menu.
- **Fix (Crowded Room & Character Tap Delay):** Eliminated the long delay when tapping characters or opening menus in crowded rooms by prioritizing the selected character for immediate rendering while smoothly staggering background character outfit updates one per frame.
- **Improvement (Instant Canvas Touch Response & Dialog Layout Containment):** Added direct touch manipulation hints on the game canvas and isolated character dialog layouts so tapping characters and switching restraint menus responds immediately without layout stutter.
- **Fix (WebGL & GPU Memory Optimization):** Fixed a memory issue where discarded outfit textures were not released from graphics memory, significantly reducing RAM usage during long sessions and crowded room visits while keeping the lightweight theme fast and responsive.
- **Improvement (Universal Idle GPU & Battery Pacing):** Significantly lowered GPU usage when standing still in rooms, browsing menus, or running in split-screen/background, while keeping active animations, minigames, and interactions completely smooth.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
