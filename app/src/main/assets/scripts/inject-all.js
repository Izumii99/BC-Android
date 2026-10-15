// ==UserScript==
// @name         BC Android Remote Loader
// @namespace    http://tampermonkey.net/
// @version      1.0
// @description  Fetches the latest scripts from Izumii99/BC-Desktop on GitHub and injects them automatically
// @author       Izumii99
// @match        https://*.bondageprojects.elementfx.com/*
// @match        https://*.bondage-europe.com/*
// @match        https://*.bondageprojects.com/*
// @match        https://*.bondage-asia.com/*
// @grant        none
// ==/UserScript==

(function () {
    "use strict";

    if (window._bcRemoteLoaderInjected) return;
    window._bcRemoteLoaderInjected = true;

    const managerURL = "https://cdn.jsdelivr.net/gh/Izumii99/BC-Desktop@main/Scripts/addon-manager.js";

    function injectManager() {
        let target = document.head || document.documentElement;
        if (!target) {
            setTimeout(injectManager, 10);
            return;
        }

        let script = document.createElement("script");
        script.src = managerURL + "?v=" + Date.now();
        script.async = false;
        script.onerror = () => {
            console.warn("BC Android: Remote Addon Manager could not be reached. Triggering offline fallback bundle.");
            if (window.AndroidBridge && window.AndroidBridge.loadOfflineAddons) {
                window.AndroidBridge.loadOfflineAddons();
            }
        };
        target.appendChild(script);
        console.log("BC Android: Loaded Addon Manager remotely from Izumii99/BC-Desktop");
    }

    injectManager();
})();
