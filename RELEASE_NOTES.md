### Fixes & Improvements:

- **Feature (Android 13+ Notification Permissions & Background Chat Alert):** The app now prompts for runtime notification permissions on Android 13+ (API 33+), and automatically displays incoming chat and whisper message counts directly in the keepalive notification when running in the background.
- **Improvement (Interactive Update Dialog):** Application update alerts have been converted into an interactive dialog, allowing one-tap navigation to download the latest APK release from GitHub.
- **Improvement (Offline Addon Fallback & Config Integrity):** Offline addons now automatically activate if remote CDNs are unreachable, and strictly respect user addon preferences from `BCDesktop_Addons_Config` so disabled addons remain disabled. Deprecated wardrobe pagination scripts have been excluded.
- **Improvement (Touch Drag & Mobile Gestures for Addons):** Added full touchscreen touch-dragging (`touchstart`, `touchmove`, `touchend`) to floating widgets in the In-Game Translator and DevTools Debugger, and enabled Cheat Menu access inside chatrooms using a convenient 600ms long-press or two-finger tap gesture.
- **Fix (Anti-Focus-Steal & Soft Keyboard Protection):** Guarded `window.ElementFocus` and `HTMLElement.prototype.focus` so that `#InputChat` never steals focus or abruptly dismisses the virtual keyboard while typing into addon inputs or search fields.
- **Fix (WebView ReferenceError Guard):** Added a native property fallback for `window.pidx` to prevent crashes on Chromium WebView engines.
- **Improvement (Memory Leak Prevention & Detached DOM Purge):** Enhanced background memory throttling script to clean up disconnected DOM references (`bcFocusMemory`, `bcLastMousedown`, `bcLastFocusedLayer`) and trigger runtime garbage collection when unfocused.
- **Feature (Local version.txt Configuration & Lock Support):** Added support for loading game versions from a `version.txt` file (with `:lock` flag support) stored in external or internal application storage before falling back to automatic server detection.
- **Improvement (Automated Addon Sync Build Pipeline):** Integrated an automatic Gradle sync task to ensure all offline addon fallback assets are kept in sync with the upstream repository during every build.
- **Feature (Server Redirection Chain Auto-Detection):** The game version updater intelligently inspects active server directives, following automatic meta-redirections (such as the server migration from R132 to R133) so you are always loaded into the officially active game release.
- **Improvement (Context-Aware Pinch-to-Zoom):** Smooth pinch-to-zoom (up to 5x magnification) across standard landscape screens with automatic zoom locking when entering vertical portrait rooms.
- **Improvement (Smart Back Navigation & External Link Handling):** When following external links, wiki pages, or subpages within the app, pressing the Android back button smoothly navigates back through web history directly to the game. Once on the active Bondage Club game canvas, the double-tap back confirmation activates to ensure active game sessions are never accidentally closed.
- **Fix (WebKit Crash Auto-Recovery):** Built-in watchdog smoothly recovers and reloads frozen WebKit render processes with rapid crash-loop protection (up to 3 recoveries within 30 seconds).

### Downloads:

- **BC-Android-Standard-Release.apk (~0.88 MB):** Official client package with full app icon and branding.
- **BC-Android-Discreet-Release.apk (~0.88 MB):** Stealth build featuring minimal "BC" app name and completely transparent icon for discrete homescreen placement.
