### Fixes & Improvements:

- **Hardened Canvas Lifecycle & Addon Hook Safety:** Refactored background render interception to use an idempotent closure guard, preventing third-party addon hooks on `DrawProcess` from being clobbered during application pause and resume cycles.
- **Background Focus Isolation:** Intercepted `document.hasFocus()` while backgrounded or running in split-screen/multi-window, ensuring game canvas animations and addon tick loops properly idle when unfocused.
- **Garbage Collector Throttling:** Added a 60-second cooldown on background JavaScript garbage collection invocations to eliminate CPU spikes during rapid app-switching.
- **UX & Clean Startup:** Eliminated system battery optimization prompts on application startup, allowing players to launch and enter rooms immediately without unwanted menu redirects.
- **Continuous 24/7 Socket Keep-Alive:** Implemented full hardware `WifiLock` (preventing Wi-Fi chip DTIM power-saving drops) and tuned the CPU WakeLock guard to 1.2 seconds, ensuring cellular 4G/5G radio transitions have sufficient time to transmit packets before the device sleeps.
- **Rendering & Portrait Compatibility:** Synchronized clean native canvas rendering and clipboard bridging, ensuring full compatibility with portrait layout addons (BC-LCE) and smooth wardrobe asset loading.
- **Seamless Silent Background Session:** Completely silenced the background keep-alive notification and eliminated status bar clutter ("BC session is active" / "Active room session running in background"), maintaining a clean, distraction-free notification shade.
- **Interactive Post-Update Changelog:** The app now automatically displays an in-app "What's New" release notes dialog after updating so players immediately see new features and changes.
- **Background Auto-Relogin:** Unblocked and exposed automatic relogin handlers in the background, enabling the client to automatically reconnect and re-enter rooms without requiring manual screen unlocking.
- **Socket.IO Protocol Compliance:** Upgraded keep-alive pulses to utilize native `sendPacket('ping')` frames, ensuring full compatibility with Socket.IO v4 servers without sending malformed event packets.
- **Ultra-Low Background Power Profile:** Throttled the game tick loop to 0.25 FPS (4-second intervals), froze active chat log DOM styling via `content-visibility: hidden`, and deactivated canvas compositing to eliminate GPU and CPU overhead while minimized.

### How to Keep the App Connected in Background:

For the best background room experience without Android terminating your connection:
1. **Battery Setting (Optional):** If your device aggressive battery saver drops background network sockets, go to **App Info > Battery** -> Choose **Unrestricted** (or *Don't optimize*).
2. **Lock in Recent Apps (Samsung, Xiaomi, Oppo, Vivo):** Open your recent apps switcher and lock the app so the OS memory cleaner never kills it.
3. **Allow Background Activity:** Enable *Autostart* or *Run in background* if prompted by your phone brand.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
