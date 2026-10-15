// ==UserScript==
// @name         FUSAM & Liko Dual Loader (Local Tester)
// @namespace    http://tampermonkey.net/
// @version      1.0
// @description  Loads both FUSAM and Liko Plugin Collection Manager simultaneously for local testing
// @author       Izumii99
// @match        https://*.bondageprojects.elementfx.com/*
// @match        https://*.bondage-europe.com/*
// @match        https://*.bondageprojects.com/*
// @match        https://*.bondage-asia.com/*
// @grant        none
// @run-at       document-end
// ==/UserScript==

(function () {
    "use strict";
    if (window !== window.top) return;

    function injectMetaLoaders() {
        const target = document.head || document.documentElement;
        if (!target) {
            setTimeout(injectMetaLoaders, 50);
            return;
        }

        // 1. Inject FUSAM (Sidiousious's Addon Loader)
        if (window.FUSAM === undefined && !document.getElementById("fusam-local-loader")) {
            const fusamScript = document.createElement("script");
            fusamScript.id = "fusam-local-loader";
            fusamScript.type = "module";
            fusamScript.src = "https://sidiousious.gitlab.io/bc-addon-loader/fusam.js?_=" + Date.now();
            target.appendChild(fusamScript);
            console.log("[Local Tester] Injected FUSAM Loader");
        }

        // 2. Inject Liko Plugin Collection Manager (PCM)
        if (!window._likoPcmLoaded && !document.getElementById("liko-local-loader")) {
            window._likoPcmLoaded = true;
            const likoScript = document.createElement("script");
            likoScript.id = "liko-local-loader";
            likoScript.src = "https://awdrrawd.github.io/liko-Plugin-Repository/PCM_Loader.user.js?_=" + Date.now();
            target.appendChild(likoScript);
            console.log("[Local Tester] Injected Liko Plugin Collection Manager");
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", injectMetaLoaders);
    } else {
        injectMetaLoaders();
    }
})();
