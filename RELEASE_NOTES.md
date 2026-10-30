### Fixes & Improvements:

- **Adaptive Foreground Low-Power Mode (30/15 FPS):** Automatically paces canvas rendering to 30 FPS during active touch or dialog interactions and steps down to 15 FPS while reading or typing chat, significantly lowering battery consumption and device temperature while the app is open.
- **Lightweight Theme Chat Virtualization & DOM Optimization:** Added CSS chat log virtualization (`content-visibility: auto`) and eliminated redundant layout recalculations during active room conversations so the default lightweight theme stays fast and power-efficient.
- **Hardened Energy Saver Background Keep-Alive:** Upgraded background connectivity with persistent high-performance Wi-Fi locks, instant network-switch socket recovery, and zero-reflow background chat buffering so accounts stay connected in online rooms even when Android's "Energy Saver (Recommended)" mode is active.
- **Login Screen Notification Control (Off by Default):** System notifications, unread status bar badges, and vibrations are now completely disabled by default without startup permission prompts, with a dedicated Notification Settings button in the bottom-left corner of the Login screen to toggle alerts ON or OFF at any time.
- **Fix (Freeze After Screen Unlock):** Resolved an intermittent freeze where the game would fail to resume after turning the screen off and on, by properly flushing the render loop queue with deduplication, recovering lost GPU graphics context, and adding a self-healing touch guard that automatically fixes stale lifecycle states if they get stuck.
- **Smart Split-Screen & Activity-Aware Background Burst:** Players joining or changing outfits in a room now pre-load character appearances in split-screen or unfocused mode, with activity-aware animation bursts (BCAR, BC-LCE, BC-HSC) that automatically return to a 1 FPS deep-idle state when quiet to preserve battery and memory.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device's aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.95MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.95MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
