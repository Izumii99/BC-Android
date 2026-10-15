(function () {
    "use strict";
    if (window !== window.top) return;
    if (window._nekoDebugLoaded) return;
    window._nekoDebugLoaded = true;

    // ── Panel ──────────────────────────────────────────────────────────────
    const panel = document.createElement("div");
    panel.id = "nk-debug-panel";
    Object.assign(panel.style, {
        position:   "fixed",
        top:        "4px",
        left:       "4px",
        zIndex:     "2147483647",
        background: "rgba(10,8,16,0.93)",
        border:     "1px solid #6b5090",
        borderRadius: "6px",
        padding:    "6px 8px",
        fontFamily: "monospace",
        fontSize:   "11px",
        color:      "#d4c8e8",
        maxWidth:   "420px",
        minWidth:   "220px",
        userSelect: "text",
        lineHeight: "1.5",
        pointerEvents: "auto",
        cursor:     "default",
        display:    "none",
    });

    const header = document.createElement("div");
    Object.assign(header.style, {
        display:        "flex",
        justifyContent: "space-between",
        alignItems:     "center",
        marginBottom:   "4px",
        cursor:         "move",
    });
    header.innerHTML = `<span style="color:#a898c0;font-weight:bold">neko-debug</span>`;

    const closeBtn = document.createElement("span");
    closeBtn.textContent = "X";
    Object.assign(closeBtn.style, { cursor: "pointer", color: "#7a6d94", marginLeft: "8px" });
    closeBtn.onclick = () => panel.style.display = "none";
    header.appendChild(closeBtn);

    const toggleBtn = document.createElement("span");
    toggleBtn.textContent = "pick";
    Object.assign(toggleBtn.style, {
        cursor: "pointer", color: "#7eb8f0", marginLeft: "8px",
        padding: "1px 5px", border: "1px solid #2e4a7a", borderRadius: "3px",
    });
    header.appendChild(toggleBtn);

    const scanBtn = document.createElement("span");
    scanBtn.textContent = "scan";
    Object.assign(scanBtn.style, {
        cursor: "pointer", color: "#fbbf24", fontWeight: "bold", marginLeft: "8px",
        padding: "1px 5px", border: "1px solid #7a5c1a", borderRadius: "3px",
    });
    header.appendChild(scanBtn);

    const watchBtn = document.createElement("span");
    watchBtn.textContent = "watch";
    Object.assign(watchBtn.style, {
        cursor: "pointer", color: "#a8d4a8", marginLeft: "8px",
        padding: "1px 5px", border: "1px solid #3a6a3a", borderRadius: "3px",
    });
    header.appendChild(watchBtn);

    const output = document.createElement("pre");
    Object.assign(output.style, {
        margin: "0", whiteSpace: "pre-wrap", wordBreak: "break-all",
        maxHeight: "320px", overflowY: "auto", color: "#d4c8e8",
    });
    output.textContent = "Click [pick] then click any element.\n[scan] = find heart/paw in DOM\n[watch] = log all new nodes";

    panel.appendChild(header);
    panel.appendChild(output);

    // ── Drag ───────────────────────────────────────────────────────────────
    let dragging = false, ox = 0, oy = 0;
    header.addEventListener("mousedown", e => {
        if (e.target === closeBtn || e.target === toggleBtn) return;
        dragging = true; ox = e.clientX - panel.offsetLeft; oy = e.clientY - panel.offsetTop;
    });
    document.addEventListener("mousemove", e => {
        if (!dragging) return;
        panel.style.left = (e.clientX - ox) + "px";
        panel.style.top  = (e.clientY - oy) + "px";
    });
    document.addEventListener("mouseup", () => { dragging = false; });

    // ── Pick mode ──────────────────────────────────────────────────────────
    let picking = false;
    let highlight = null;

    function makeHighlight() {
        const h = document.createElement("div");
        Object.assign(h.style, {
            position: "fixed", pointerEvents: "none", zIndex: "2147483646",
            outline: "2px solid #9858cc", background: "rgba(152,88,204,0.08)",
            transition: "all 0.05s",
        });
        document.body.appendChild(h);
        return h;
    }

    function describeEl(el) {
        const cs = getComputedStyle(el);
        const rect = el.getBoundingClientRect();

        const tag = el.tagName.toLowerCase();
        const id  = el.id ? "#" + el.id : "";
        const cls = el.className && typeof el.className === "string"
            ? "." + el.className.trim().split(/\s+/).join(".") : "";

        const inline = el.getAttribute("style") || "(none)";

        // -- Theme context --
        const inChatLog = !!el.closest("#TextAreaChatLog");
        const THEME_CLASSES = ["nk-online","nk-offline","nk-voice","nk-enter","nk-disconnect","nk-emote","bcn-notification","bcn-own-message","ChatMessageWhisper","ChatMessageBeep","ChatMessageEmote","ChatMessageAction","ChatMessageActivity"];
        const matched = THEME_CLASSES.filter(c => el.classList.contains(c));

        let themeNote = inChatLog ? "Inside #TextAreaChatLog ✓" : "Outside #TextAreaChatLog";
        if (matched.length) themeNote += "\nMatched: " + matched.join(", ");
        else themeNote += "\nNo theme class matched";

        const lines = [
            "TAG:    " + tag + id + cls,
            "RECT:   " + Math.round(rect.left) + "," + Math.round(rect.top) + " " + Math.round(rect.width) + "x" + Math.round(rect.height),
            "--- computed ---",
            "bg:     " + cs.backgroundColor,
            "color:  " + cs.color,
            "border: " + cs.border,
            "shadow: " + cs.boxShadow,
            "opacity:" + cs.opacity,
            "--- inline style ---",
            inline,
            "--- theme context ---",
            themeNote,
            "--- full selector ---",
            buildSelector(el),
        ];
        return lines.join("\n");
    }

    function buildSelector(el) {
        const parts = [];
        let cur = el;
        while (cur && cur !== document.body) {
            let s = cur.tagName.toLowerCase();
            if (cur.id) { s += "#" + cur.id; parts.unshift(s); break; }
            if (cur.className && typeof cur.className === "string") {
                s += "." + cur.className.trim().split(/\s+/).join(".");
            }
            parts.unshift(s);
            cur = cur.parentElement;
        }
        return parts.join(" > ");
    }

    function onMouseOver(e) {
        if (!picking || e.target === panel || panel.contains(e.target)) return;
        const r = e.target.getBoundingClientRect();
        highlight.style.left   = r.left   + "px";
        highlight.style.top    = r.top    + "px";
        highlight.style.width  = r.width  + "px";
        highlight.style.height = r.height + "px";
    }

    function onClick(e) {
        if (!picking) return;
        if (e.target === panel || panel.contains(e.target)) return;
        e.preventDefault(); e.stopPropagation();
        output.textContent = describeEl(e.target);
    }

    function startPick() {
        picking = true;
        toggleBtn.textContent = "stop";
        toggleBtn.style.color = "#fb7185";
        toggleBtn.style.fontWeight = "bold";
        toggleBtn.style.borderColor = "#6b2e3a";
        highlight = makeHighlight();
        document.addEventListener("mouseover", onMouseOver, true);
        document.addEventListener("click", onClick, true);
    }

    function stopPick() {
        picking = false;
        toggleBtn.textContent = "pick";
        toggleBtn.style.color = "#7eb8f0";
        toggleBtn.style.fontWeight = "normal";
        toggleBtn.style.borderColor = "#2e4a7a";
        document.removeEventListener("mouseover", onMouseOver, true);
        document.removeEventListener("click", onClick, true);
        if (highlight) { highlight.remove(); highlight = null; }
    }

    toggleBtn.addEventListener("click", () => picking ? stopPick() : startPick());

    // ── Scan: find heart/paw/badge in DOM ──────────────────────────────────
    const HEART_RE = /[❤♥🐾💕💗💖💓💞💘🩷🩵🫀]/u;
    const BADGE_KEYWORDS = ["badge", "relation", "heart", "paw", "love", "hati", "bcn-relation"];

    scanBtn.addEventListener("click", () => {
        const results = [];
        const all = document.querySelectorAll("*");
        for (const el of all) {
            if (el === panel || panel.contains(el)) continue;
            const tag = el.tagName.toLowerCase();
            if (tag === "script" || tag === "style") continue;

            const cls = typeof el.className === "string" ? el.className : "";
            const id  = el.id || "";
            const txt = el.childNodes.length <= 3 ? (el.textContent || "").trim().slice(0, 60) : "";

            const isBadge = BADGE_KEYWORDS.some(k => cls.toLowerCase().includes(k) || id.toLowerCase().includes(k));
            const hasHeart = HEART_RE.test(txt);

            if (isBadge || hasHeart) {
                const r = el.getBoundingClientRect();
                results.push(
                    `${tag}${id ? "#"+id : ""}${cls ? "."+cls.trim().split(/\s+/).join(".") : ""}\n` +
                    `  rect: ${Math.round(r.left)},${Math.round(r.top)} ${Math.round(r.width)}x${Math.round(r.height)}\n` +
                    `  txt:  ${txt || "(empty)"}\n` +
                    `  vis:  display=${getComputedStyle(el).display} opacity=${getComputedStyle(el).opacity}`
                );
            }
        }
        output.textContent = results.length
            ? `Found ${results.length} element(s):\n\n` + results.join("\n\n")
            : "No heart/paw/badge elements found in DOM.";
    });

    // ── Watch: log all new nodes in DOM ────────────────────────────────────
    let watchObs = null;
    watchBtn.addEventListener("click", () => {
        if (watchObs) {
            watchObs.disconnect();
            watchObs = null;
            watchBtn.textContent = "watch";
            watchBtn.style.color = "#a8d4a8";
            watchBtn.style.fontWeight = "normal";
            watchBtn.style.borderColor = "#3a6a3a";
            return;
        }
        watchBtn.textContent = "stop";
        watchBtn.style.color = "#fb7185";
        watchBtn.style.fontWeight = "bold";
        watchBtn.style.borderColor = "#6b2e3a";
        output.textContent = "Watching DOM for new nodes...\n";

        watchObs = new MutationObserver((mutations) => {
            for (const m of mutations) {
                for (const node of m.addedNodes) {
                    if (node.nodeType !== 1) continue;
                    if (node === panel || panel.contains(node)) continue;
                    const tag = node.tagName.toLowerCase();
                    const cls = typeof node.className === "string" ? node.className : "";
                    const id  = node.id || "";
                    const txt = (node.textContent || "").trim().slice(0, 80);
                    const line = `+[${tag}${id?"#"+id:""}${cls?"."+cls.trim().split(/\s+/).slice(0,3).join("."):""}] "${txt}"\n`;
                    output.textContent = line + output.textContent.slice(0, 2000);
                }
            }
        });
        watchObs.observe(document.body, { childList: true, subtree: true });
    });

    // ── Mount ─────────────────────────────────────────────────────────────
    function mount() {
        if (!document.body) { setTimeout(mount, 200); return; }
        document.body.appendChild(panel);
    }
    mount();
})();

(function () {
    "use strict";
    if (window !== window.top) return;
    if (window._nekoAppDebugLoaded) return;
    window._nekoAppDebugLoaded = true;

    // ── Panel ──────────────────────────────────────────────────────────────
    const panel = document.createElement("div");
    panel.id = "nk-app-debug-panel";
    Object.assign(panel.style, {
        position:   "fixed",
        top:        "40px",
        left:       "4px",
        zIndex:     "2147483647",
        background: "rgba(10,8,16,0.93)",
        border:     "1px solid #306090",
        borderRadius: "6px",
        padding:    "6px 8px",
        fontFamily: "monospace",
        fontSize:   "11px",
        color:      "#d4c8e8",
        width:      "550px",
        userSelect: "text",
        lineHeight: "1.5",
        pointerEvents: "auto",
        cursor:     "default",
        display:    "none",
    });

    const header = document.createElement("div");
    Object.assign(header.style, {
        display:        "flex",
        justifyContent: "space-between",
        alignItems:     "center",
        marginBottom:   "4px",
        cursor:         "move",
    });
    header.innerHTML = `<span style="color:#80b0d0;font-weight:bold">neko-appearance-debug</span>`;

    const closeBtn = document.createElement("span");
    closeBtn.textContent = "X";
    Object.assign(closeBtn.style, { cursor: "pointer", color: "#909090", marginLeft: "8px" });
    closeBtn.onclick = () => panel.style.display = "none";
    
    const btnContainer = document.createElement("div");

    const dumpBtn = document.createElement("span");
    dumpBtn.textContent = "Dump Appearance";
    Object.assign(dumpBtn.style, {
        cursor: "pointer", color: "#d4a84a", marginLeft: "8px",
        padding: "1px 5px", border: "1px solid #7a5c1a", borderRadius: "3px",
    });

    const traceDrawBtn = document.createElement("span");
    traceDrawBtn.textContent = "Trace Draws (1s)";
    Object.assign(traceDrawBtn.style, {
        cursor: "pointer", color: "#4ad4a8", marginLeft: "8px",
        padding: "1px 5px", border: "1px solid #1a7a5c", borderRadius: "3px",
    });
    
    btnContainer.appendChild(dumpBtn);
    btnContainer.appendChild(traceDrawBtn);
    btnContainer.appendChild(closeBtn);
    header.appendChild(btnContainer);

    const output = document.createElement("textarea");
    Object.assign(output.style, {
        width: "100%", height: "400px", background: "#000", color: "#fff",
        border: "1px solid #555", fontFamily: "monospace", fontSize: "11px",
        whiteSpace: "pre", overflow: "auto", resize: "vertical", boxSizing: "border-box",
        marginTop: "5px"
    });
    output.value = "Click [Dump Appearance] to see functions.\nClick [Trace Draws] to capture 1 frame of rendering coordinates.";

    panel.appendChild(header);
    panel.appendChild(output);

    // ── Drag ───────────────────────────────────────────────────────────────
    let dragging = false, ox = 0, oy = 0;
    header.addEventListener("mousedown", e => {
        if (e.target === closeBtn || e.target === dumpBtn) return;
        dragging = true; ox = e.clientX - panel.offsetLeft; oy = e.clientY - panel.offsetTop;
    });
    document.addEventListener("mousemove", e => {
        if (!dragging) return;
        panel.style.left = (e.clientX - ox) + "px";
        panel.style.top  = (e.clientY - oy) + "px";
    });
    document.addEventListener("mouseup", () => { dragging = false; });

    // ── Logic ──────────────────────────────────────────────────────────────
    dumpBtn.addEventListener("click", () => {
        let res = "=== BC Appearance Debug Dump ===\n\n";
        
        if (typeof window.AssetGroup !== "undefined") {
            res += `AssetGroup Count: ${window.AssetGroup.length}\n\n`;
        }
        
        // Target wardrobe rendering functions
        const targets = [
            "AppearanceRun",
            "AppearanceMenuBuild",
            "AppearanceMenuDraw",
            "AppearanceClick",
            "AppearanceMenuClick"
        ];
        
        for (const t of targets) {
            if (typeof window[t] === "function") {
                res += `---------- ${t} ----------\n`;
                res += window[t].toString() + "\n\n";
            }
        }

        res += "=== All Functions containing 'Appearance' ===\n";
        for (const key in window) {
            try {
                if (typeof window[key] === "function" && key.toLowerCase().includes("appearance")) {
                    res += `* ${key}\n`;
                }
            } catch(e) {}
        }


        output.value = res;
    });

    let isTracingDraws = false;
    traceDrawBtn.addEventListener("click", () => {
        if (isTracingDraws) return;
        isTracingDraws = true;
        traceDrawBtn.textContent = "Tracing...";
        
        let traceData = "=== Draw Call Trace (1 Frame) ===\n\n";
        let frameTraced = false;
        
        const origDrawButton = window.DrawButton;
        const origDrawImage = window.DrawImage;
        const origDrawImageResize = window.DrawImageResize;
        
        let btnCalls = 0;
        window.DrawButton = function(Left, Top, Width, Height, Label, Color, Image, HoveringText, Disabled) {
            if (isTracingDraws && !frameTraced) {
                traceData += `DrawBtn | X:${Math.round(Left)}, Y:${Math.round(Top)}, W:${Math.round(Width)}, H:${Math.round(Height)} | Lbl:"${Label||""}"\n`;
            }
            btnCalls++;
            return origDrawButton(Left, Top, Width, Height, Label, Color, Image, HoveringText, Disabled);
        };
        
        let imgCalls = 0;
        window.DrawImage = function(Image, X, Y) {
            if (isTracingDraws && !frameTraced) {
                if (!String(Image).includes("Backgrounds")) {
                    traceData += `DrawImg | X:${Math.round(X)}, Y:${Math.round(Y)} | Img:"${Image}"\n`;
                }
            }
            imgCalls++;
            return origDrawImage(Image, X, Y);
        };

        let imgResCalls = 0;
        window.DrawImageResize = function(Image, X, Y, W, H) {
            if (isTracingDraws && !frameTraced) {
                if (!String(Image).includes("Backgrounds")) {
                    traceData += `DrawImgRes | X:${Math.round(X)}, Y:${Math.round(Y)}, W:${Math.round(W)}, H:${Math.round(H)} | Img:"${Image}"\n`;
                }
            }
            imgResCalls++;
            return origDrawImageResize(Image, X, Y, W, H);
        };
        
        // Record for 300ms (~18 frames) and take the first frame
        setTimeout(() => {
            frameTraced = true; // stop recording
            setTimeout(() => {
                isTracingDraws = false;
                window.DrawButton = origDrawButton;
                window.DrawImage = origDrawImage;
                window.DrawImageResize = origDrawImageResize;
                traceDrawBtn.textContent = "Trace Draws (1s)";
                output.value = traceData + `\nTotal Calls recorded in 1s:\nDrawButton: ${btnCalls}\nDrawImage: ${imgCalls}\nDrawImageResize: ${imgResCalls}`;
            }, 100);
        }, 300);
    });

    // ── Mount ─────────────────────────────────────────────────────────────
    function mount() {
        if (!document.body) { setTimeout(mount, 200); return; }
        document.body.appendChild(panel);
    }
    mount();
})();

(function() {
    "use strict";
    if (window !== window.top) return;
    if (window._nekoFocusDebugLoaded) return;
    window._nekoFocusDebugLoaded = true;

    // Create debugger panel
    const panel = document.createElement("div");
    panel.id = "nk-focus-debug-panel";
    Object.assign(panel.style, {
        position: "fixed", top: "10px", right: "10px", width: "450px", height: "300px",
        background: "rgba(0,0,0,0.85)", color: "#00ff00", zIndex: "2147483647",
        fontFamily: "monospace", fontSize: "11px", overflowY: "auto", padding: "5px",
        border: "1px solid #00ff00", pointerEvents: "none",
        display: "none" // hidden by default
    });

    const closeBtn = document.createElement("div");
    closeBtn.textContent = "X";
    Object.assign(closeBtn.style, {
        position: "absolute", top: "5px", right: "5px", cursor: "pointer", 
        color: "white", pointerEvents: "auto", background: "red", 
        padding: "2px 5px", borderRadius: "3px"
    });
    closeBtn.onclick = () => panel.style.display = "none";
    panel.appendChild(closeBtn);

    const logContainer = document.createElement("div");
    panel.appendChild(logContainer);
    
    function mount() {
        if (!document.body) { setTimeout(mount, 200); return; }
        document.body.appendChild(panel);
    }
    mount();

    function log(msg) {
        const time = new Date().toISOString().split('T')[1].slice(0, 11);
        logContainer.innerHTML = `[${time}] ${msg}<br>` + logContainer.innerHTML.slice(0, 10000);
    }

    log("Focus Debugger Initialized...");

    // 1. Hook window.ElementFocus
    let patchInterval = setInterval(() => {
        if (typeof window.ElementFocus === "function" && !window.ElementFocus._isDebugHooked) {
            const orig = window.ElementFocus;
            window.ElementFocus = function(ID) {
                let stack = "";
                try { throw new Error(); } catch(e) { stack = (e.stack || "").split('\n')[2] || ""; }
                log(`<span style="color:#ff8800">ElementFocus('${ID}')</span><br>&nbsp;&nbsp;caller: <span style="color:#aaa">${stack.trim()}</span>`);
                orig.apply(this, arguments);
            };
            window.ElementFocus._isDebugHooked = true;
            clearInterval(patchInterval);
            log("Hooked window.ElementFocus!");
        }
    }, 100);

    // 2. Hook HTMLElement.prototype.focus
    const origFocus = HTMLElement.prototype.focus;
    HTMLElement.prototype.focus = function() {
        const id = this.id ? `#${this.id}` : '';
        const cls = typeof this.className === "string" && this.className ? `.${this.className.trim().split(/\s+/).join('.')}` : '';
        const tag = this.tagName ? this.tagName.toLowerCase() : 'unknown';
        
        let stack = "";
        try { throw new Error(); } catch(e) { stack = (e.stack || "").split('\n')[2] || ""; }
        
        log(`<span style="color:#0088ff">HTMLElement.focus() -> ${tag}${id}${cls}</span><br>&nbsp;&nbsp;caller: <span style="color:#aaa">${stack.trim()}</span>`);
        origFocus.apply(this, arguments);
    };
    log("Hooked HTMLElement.prototype.focus!");

    // 3. Monitor generic focus events
    window.addEventListener("focus", (e) => {
        let targetStr = "window";
        if (e.target && e.target.tagName) {
            targetStr = e.target.tagName.toLowerCase() + (e.target.id ? `#${e.target.id}` : '');
        }
        log(`<span style="color:#ff00ff">Event: focus</span> on ${targetStr}`);
    }, true);
})();

(function() {
    "use strict";
    if (window !== window.top) return;
    if (window._nekoMasterDebugBtnLoaded) return;
    window._nekoMasterDebugBtnLoaded = true;

    const wrapper = document.createElement("div");
    Object.assign(wrapper.style, {
        position: "fixed",
        bottom: "20px",
        left: "20px",
        zIndex: "2147483647",
        display: "flex",
        gap: "5px",
        alignItems: "center"
    });

    // Main Toggle Button
    const mainBtn = document.createElement("div");
    mainBtn.innerHTML = "🛠️";
    mainBtn.title = "Toggle DevTools Menu";
    Object.assign(mainBtn.style, {
        width: "40px", height: "40px",
        background: "rgba(0,0,0,0.8)", borderRadius: "10px",
        display: "flex", alignItems: "center", justifyContent: "center",
        fontSize: "24px", cursor: "pointer",
        border: "1px solid #555",
        userSelect: "none"
    });

    // Sub Container (like in the screenshot)
    const subContainer = document.createElement("div");
    Object.assign(subContainer.style, {
        background: "rgba(0,0,0,0.8)",
        padding: "5px",
        borderRadius: "10px",
        display: "none", // hidden by default
        gap: "5px",
        alignItems: "center",
        border: "1px solid #555"
    });

    // Helper to create cat buttons
    function createCatBtn(label, title, panelId) {
        const btn = document.createElement("div");
        btn.innerHTML = label;
        btn.title = title;
        Object.assign(btn.style, {
            width: "36px", height: "36px",
            background: "rgba(255,255,255,0.1)", borderRadius: "8px",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: "14px", fontWeight: "bold", cursor: "pointer",
            boxShadow: "inset 0 0 5px rgba(0,0,0,0.5)",
            userSelect: "none", color: "#ddd"
        });
        
        btn.onclick = () => {
            const p = document.getElementById(panelId);
            if (p) {
                // If it's currently hidden or undefined, show it
                const isActive = (p.style.display === "none" || !p.style.display);
                p.style.display = isActive ? "block" : "none";
                btn.style.background = isActive ? "rgba(100,200,100,0.4)" : "rgba(255,255,255,0.1)";
            }
        };
        return btn;
    }

    subContainer.appendChild(createCatBtn("DM", "DOM Debug Panel", "nk-debug-panel"));
    subContainer.appendChild(createCatBtn("AP", "Appearance & Canvas Panel", "nk-app-debug-panel"));
    subContainer.appendChild(createCatBtn("FC", "Focus Logger Panel", "nk-focus-debug-panel"));

    mainBtn.onclick = () => {
        const isHidden = subContainer.style.display === "none";
        subContainer.style.display = isHidden ? "flex" : "none";
        mainBtn.style.background = isHidden ? "rgba(100,100,100,0.8)" : "rgba(0,0,0,0.8)";
    };

    wrapper.appendChild(mainBtn);
    wrapper.appendChild(subContainer);

    function mount() {
        if (!document.body) { setTimeout(mount, 200); return; }
        document.body.appendChild(wrapper);
    }
    mount();
})();
