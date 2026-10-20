### Fixes & Improvements:

- **Fix (Background Persistence):** Resolved an issue where background room sessions were disconnected after 2 to 3 minutes due to internal Chromium Blink renderer freezing. The new custom WebView pipeline bypasses visibility drop signals so the web engine treats the app as continuously open on screen.
- **Fix (Sandbox Process Freezing):** Eliminated OS-level background task freezing by Android's Cached App Freezer using a silent, inaudible WebAudio keep-alive channel, ensuring the isolated WebView sandbox process remains awake.
- **Improvement (Network & Wi-Fi Stability):** Integrated a high-performance Wi-Fi low-latency lock (`WifiLock`) and increased pulse frequency to 12 seconds, preventing mobile carrier and home Wi-Fi routers from dropping inactive WebSocket NAT ports.
- **Improvement (Automatic Reconnection):** Background heartbeats now include low-level Engine.IO transport pings (`'2'`) and trigger auto-reconnection and room re-login immediately in the background if a brief network outage occurs.
- **Improvement (Memory Management):** Removed aggressive memory clearing during pause events, preventing the system memory coordinator from treating active chat sessions as disposable while still maintaining a cool 15 FPS throttle.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
