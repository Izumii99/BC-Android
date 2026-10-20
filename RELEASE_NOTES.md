### Fixes & Improvements:

- **Fix (Background Room Persistence):** Resolved an issue where background room sessions were disconnected after 2 to 3 minutes due to internal Chromium Blink renderer freezing. The new custom WebView pipeline bypasses visibility drop signals so the web engine treats the app as continuously open on screen.
- **Fix (Soft Keyboard & Text Input Focus):** Fixed virtual keyboard (IME) responsiveness and input focus inside the game chatbox. Standard window visibility and touch focus lifecycles are now preserved while the app is in the foreground, ensuring smooth typing without losing cursor focus.
- **Fix (Process Sleep Prevention):** Eliminated OS-level background freezing caused by Android's Cached App Freezer using a silent, inaudible WebAudio keep-alive channel, ensuring the isolated WebView sandbox process remains awake.
- **Fix (Addon Manager Registry):** Permanently excluded retired standalone addons (such as legacy wardrobe pagination, which is now natively integrated into BCX) from remote discovery, and added automatic local storage cleanups for obsolete script entries.
- **Improvement (Network & Wi-Fi Stability):** Integrated a high-performance Wi-Fi low-latency lock (`WifiLock`) and a 12-second pulse frequency, preventing mobile carriers and Wi-Fi routers from prematurely closing idle WebSocket connections.
- **Improvement (Automatic Reconnection):** Background heartbeats now include low-level Engine.IO transport pings (`'2'`) and trigger auto-reconnection and room re-login immediately in the background if a brief network outage occurs.
- **Improvement (Transparent UI Asset):** Refined the floating Addon Manager button and shared icon asset (`Icon.png`) with clean edge transparency, removing the solid dark borders for a seamless in-game look.
- **Feature (File Download & Export Support):** Added full native file saving and download capabilities. Exporting wardrobe backups, appearance codes, BCX/addon configurations, chat logs, and photos now seamlessly saves directly into your device's public Downloads directory with instant confirmation toasts, supporting Blob URLs, Data URIs, and standard HTTP/HTTPS downloads.
- **Improvement (Memory Management):** Removed aggressive memory clearing during pause events, preventing the system memory coordinator from treating active chat sessions as disposable while still maintaining a cool 15 FPS throttle.

### Downloads:

- **BC-Android-Standard-Release.apk (~0.9MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.9MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.

