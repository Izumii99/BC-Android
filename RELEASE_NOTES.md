### Fixes & Improvements:

- **Battery & Deep Doze Exemption:** Added an automatic one-time system battery optimization prompt (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), preventing Android Deep Doze and OEM background managers (HyperOS/MIUI, OneUI, ColorOS) from severing network sockets after 60 minutes.
- **Continuous 24/7 Socket Keep-Alive:** Implemented full hardware `WifiLock` (preventing Wi-Fi chip DTIM power-saving drops) and tuned the CPU WakeLock guard to 1.2 seconds, ensuring cellular 4G/5G radio transitions have sufficient time to transmit packets before the device sleeps.
- **Background Auto-Relogin:** Unblocked and exposed automatic relogin handlers in the background, enabling the client to automatically reconnect and re-enter rooms without requiring manual screen unlocking.
- **Socket.IO Protocol Compliance:** Upgraded keep-alive pulses to utilize native `sendPacket('ping')` frames, ensuring full compatibility with Socket.IO v4 servers without sending malformed event packets.
- **Ultra-Low Background Power Profile:** Throttled the game tick loop to 0.25 FPS (4-second intervals), froze active chat log DOM styling via `content-visibility: hidden`, and deactivated canvas compositing to eliminate GPU and CPU overhead while minimized.
- **Silent Debounced Notifications:** Batched chat message counter updates with a 10-second debounce window and low-priority silent delivery, preventing system notification interruptions and audio hardware wakeups.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting:** Accept the prompt on startup or go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
