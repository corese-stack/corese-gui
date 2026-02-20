/* =================================================================
 * CORESE CODE EDITOR - APPLICATION SCRIPT
 * =================================================================
 * Main application logic for the CodeMirror-based editor.
 * Handles initialization, theming, and Java bridge communication.
 * ================================================================= */

"use strict";

/* =================================================================
 * JAVA BRIDGE SETUP
 * ================================================================= */

globalThis.onerror = function (msg, url, line, col, error) {
    if (globalThis.bridge) {
        globalThis.bridge.log(`JS ERROR: ${msg} (Line: ${line})`);
    }
};

const originalLog = console.log;
console.log = function (...args) {
    if (globalThis.bridge) {
        // Convert args to string for bridge logging
        const message = args.map(arg => 
            (typeof arg === 'object') ? JSON.stringify(arg) : String(arg)
        ).join(' ');
        globalThis.bridge.log(message);
    }
    originalLog.apply(console, args);
};

/* =================================================================
 * EDITOR INITIALIZATION
 * ================================================================= */

const editorArea = document.getElementById('editor');
const cm = CodeMirror.fromTextArea(editorArea, {
    lineNumbers: true,
    mode: "text/plain",
    // Improves dead-key/IME handling on Linux WebView (GTK input-method quirks).
    inputStyle: 'contenteditable',
    matchBrackets: true,
    autoCloseBrackets: true,
    indentUnit: 4,
    tabSize: 4,
    lineWrapping: false,
    styleActiveLine: true,
    extraKeys: {
        "Ctrl-Space": "autocomplete",
        "Ctrl-Enter": () => undefined,
        "Cmd-Enter": () => undefined,
        "Cmd-/": "toggleComment",
        "Ctrl-/": "toggleComment",
        "Ctrl-C": () => copySelectionToClipboard(),
        "Cmd-C": () => copySelectionToClipboard(),
        "Ctrl-X": () => cutSelectionToClipboard(),
        "Cmd-X": () => cutSelectionToClipboard()
    }
});

let lastRunShortcutTs = 0;

function preventRunShortcutLineBreak(event) {
    const isEnter = event && (event.key === "Enter" || event.code === "Enter" || event.keyCode === 13);
    if (!isEnter || !(event.ctrlKey || event.metaKey)) {
        return undefined;
    }
    lastRunShortcutTs = Date.now();
    event.preventDefault();
    event.stopPropagation();
    if (typeof event.stopImmediatePropagation === "function") {
        event.stopImmediatePropagation();
    }
    return false;
}

const editorWrapper = cm.getWrapperElement();
editorWrapper.addEventListener("keydown", preventRunShortcutLineBreak, true);
editorWrapper.addEventListener("keypress", preventRunShortcutLineBreak, true);

cm.on("beforeChange", (_, changeObj) => {
    if (!changeObj || typeof changeObj.cancel !== "function") {
        return;
    }
    // Defensive guard: if an Enter insert still sneaks through right after Ctrl/Cmd+Enter,
    // cancel it to keep query text unchanged.
    if (Date.now() - lastRunShortcutTs > 120) {
        return;
    }
    const inserted = Array.isArray(changeObj.text) ? changeObj.text.join("\n") : "";
    if (inserted.includes("\n") || inserted.includes("\r")) {
        changeObj.cancel();
    }
});

function copySelectionToClipboard() {
    return copySelectionUsingBridge() ? undefined : CodeMirror.Pass;
}

function cutSelectionToClipboard() {
    if (cm.getOption("readOnly")) {
        return CodeMirror.Pass;
    }

    if (!copySelectionUsingBridge()) {
        return CodeMirror.Pass;
    }
    cm.replaceSelection("");
    return undefined;
}

function copySelectionUsingBridge() {
    const selection = cm.getSelection();
    if (!selection) {
        return false;
    }
    const bridge = globalThis.bridge;
    if (!bridge || typeof bridge.copyToClipboard !== "function") {
        return false;
    }

    try {
        return bridge.copyToClipboard(selection) !== false;
    } catch (e) {
        return false;
    }
}

/* =================================================================
 * LIGHTWEIGHT MODES (CSV/TSV)
 * ================================================================= */

function countSeparators(stream, separator) {
    const start = stream.lineStart || 0;
    const upto = stream.string.slice(start, stream.start);
    let count = 0;
    for (let i = 0; i < upto.length; i++) {
        if (upto[i] === separator) count++;
    }
    return count;
}

function defineSeparatedMode(name, separator) {
    if (CodeMirror.modes[name]) {
        return;
    }
    CodeMirror.defineMode(name, function () {
        return {
            token: function (stream) {
                const col = countSeparators(stream, separator) % 7;
                if (stream.eatSpace()) return null;
                if (stream.eat(separator)) {
                    return "punctuation";
                }

                let ch = stream.peek();
                if (ch === '"') {
                    stream.next();
                    while ((ch = stream.next()) != null) {
                        if (ch === '"') break;
                        if (ch === "\\") stream.next();
                    }
                    return "string column-" + col;
                }

                stream.eatWhile(function (c) {
                    return c !== separator && c !== "\n" && c !== "\r" && c !== "\t";
                });

                const cur = stream.current();
                if (/^-?\d+(\.\d+)?$/.test(cur)) {
                    return "number column-" + col;
                }
                return "atom column-" + col;
            }
        };
    });
}

defineSeparatedMode("csv", ",");
defineSeparatedMode("tsv", "\t");
CodeMirror.defineMIME("text/x-csv", "csv");
CodeMirror.defineMIME("text/x-tsv", "tsv");

/* =================================================================
 * EVENT HANDLERS
 * ================================================================= */

/**
 * Update status bar with cursor position and selection info
 */
function updateStatusBar() {
    const pos = cm.getCursor();
    const line = pos.line + 1;
    const col = pos.ch + 1;

    document.getElementById('cursor-position').textContent = `Ln ${line}, Col ${col}`;

    const selection = cm.getSelection();
    const selLength = selection.length;
    document.getElementById('selection-count').textContent = selLength > 0 ? `(${selLength} selected)` : '';
}

cm.on("change", function (instance, changeObj) {
    if (changeObj.origin === 'setValue') return;
    if (globalThis.bridge) {
        globalThis.bridge.onContentChanged(instance.getValue());
    }
    updateStatusBar();
});

cm.on("cursorActivity", () => updateStatusBar());

/* =================================================================
 * PUBLIC API - EXPOSED TO JAVA
 * ================================================================= */

/**
 * Set editor content
 * @param {string} content - Text content to display
 */
globalThis.setContent = function (content) {
    if (content !== cm.getValue()) {
        cm.setValue(content || '');
        cm.clearHistory();
    }
    if (autoFormatResults) {
        formatContentIfPossible();
    }
    updateStatusBar();
};

/**
 * Get current editor content
 * @returns {string} Current text content
 */
globalThis.getContent = function () {
    return cm.getValue();
};

/**
 * Set read-only mode
 * @param {boolean} isReadOnly - Whether editor should be read-only
 */
globalThis.setReadOnly = function (isReadOnly) {
    cm.setOption("readOnly", isReadOnly);
};

/**
 * Set syntax highlighting mode
 * @param {string} modeName - Language mode identifier
 */
globalThis.setMode = function (modeName) {
    let mime = "text/plain";
    let displayName = "Plain Text";
    let tooltip = "";

    const mode = (modeName || "").toLowerCase();
    currentMode = mode;

    switch (mode) {
        case "turtle":
        case "ttl":
        case "n3":
        case "nt":
        case "nq":
        case "trig":
            mime = "text/turtle";
            if (mode === "trig") {
                displayName = "TriG";
            } else if (mode === "nt") {
                displayName = "N-Triples";
            } else if (mode === "nq") {
                displayName = "N-Quads";
            } else if (mode === "n3") {
                displayName = "N3";
            } else {
                displayName = "Turtle";
            }
            tooltip = "TriG, Turtle, N-Triples, N-Quads, N3";
            break;
            
        case "sparql":
        case "rq":
        case "query":
        case "update":
            mime = "application/sparql-query";
            displayName = "SPARQL";
            break;
            
        case "xml":
        case "rdf":
        case "owl":
            mime = "application/xml";
            displayName = "RDF/XML";
            break;
            
        case "json":
            mime = "application/json";
            displayName = "JSON";
            break;

        case "json-ld":
            mime = "application/ld+json";
            displayName = "JSON-LD";
            break;
            
        case "js":
        case "javascript":
            mime = "text/javascript";
            displayName = "JavaScript";
            break;

        case "csv":
            mime = "text/x-csv";
            displayName = "CSV";
            break;

        case "tsv":
            mime = "text/x-tsv";
            displayName = "TSV";
            break;

        case "markdown":
            mime = "text/plain";
            displayName = "Markdown";
            break;
    }

    cm.setOption("mode", mime);
    const wrapper = cm.getWrapperElement();
    if (wrapper) {
        wrapper.setAttribute("data-mode", mode || "text");
    }
    if (autoFormatResults) {
        formatContentIfPossible();
    }
    
    const modeDisplay = document.getElementById('mode-display');
    if (modeDisplay) {
        modeDisplay.textContent = displayName;
        modeDisplay.title = tooltip;
    }
};

/**
 * Set editor zoom level (affects editor text only, not the status bar)
 * @param {number} zoom - Zoom multiplier (e.g. 1.0, 1.1, 0.9)
 */
globalThis.setEditorZoom = function (zoom) {
    const root = document.documentElement;
    const value = Number.isFinite(zoom) ? zoom : 1;
    root.style.setProperty('--editor-zoom', String(value));
};

/**
 * Focuses the editor input so users can type immediately.
 */
globalThis.focusEditor = function () {
    cm.focus();
    updateStatusBar();
};

/* =================================================================
 * AUTO FORMAT (READ-ONLY RESULTS)
 * ================================================================= */

let autoFormatResults = false;
let currentMode = "text";

const MAX_AUTO_FORMAT_LENGTH = 200000;

globalThis.setAutoFormat = function (enabled) {
    autoFormatResults = Boolean(enabled);
};

function formatContentIfPossible() {
    const content = cm.getValue();
    if (!content || content.length > MAX_AUTO_FORMAT_LENGTH) {
        return;
    }
    try {
        if (currentMode === "json" || currentMode === "json-ld") {
            const parsed = JSON.parse(content);
            const pretty = JSON.stringify(parsed, null, 2);
            if (pretty !== content) {
                cm.setValue(pretty);
                cm.clearHistory();
            }
            return;
        }
        if (currentMode === "xml") {
            const pretty = formatXml(content);
            if (pretty && pretty !== content) {
                cm.setValue(pretty);
                cm.clearHistory();
            }
            return;
        }
    } catch (e) {
        // Ignore formatting failures and keep original content
    }
}

function formatXml(xml) {
    const input = xml.replace(/>\s+</g, "><").trim();
    if (!input.startsWith("<")) {
        return xml;
    }
    const PADDING = "  ";
    let indent = 0;
    return input
        .replace(/</g, "\n<")
        .split("\n")
        .filter(line => line.trim().length > 0)
        .map(line => {
            let trimmed = line.trim();
            if (trimmed.startsWith("</")) {
                indent = Math.max(indent - 1, 0);
            }
            const pad = PADDING.repeat(indent);
            if (trimmed.startsWith("<") && !trimmed.startsWith("</") && !trimmed.endsWith("/>") && !trimmed.startsWith("<?") && !trimmed.startsWith("<!")) {
                indent += 1;
            }
            return pad + trimmed;
        })
        .join("\n");
}

/**
 * Set application theme
 * @param {boolean} isDark - Whether to use dark mode
 * @param {string} accentColor - Optional accent color override
 * @param {string} themeName - Theme name (primer, nord, etc.)
 */
globalThis.setTheme = function (isDark, accentColor, themeName) {
    const root = document.documentElement;
    const body = document.body;

    // Set Dark/Light Mode
    if (isDark) {
        body.classList.add('dark-theme');
    } else {
        body.classList.remove('dark-theme');
    }

    // Set Theme Family (primer, nord, cupertino)
    // Remove existing theme classes
    body.className = body.className.replace(/\btheme-\S+/g, '').trim();
    if (themeName) {
        body.classList.add(`theme-${themeName.toLowerCase()}`);
    }

    if (accentColor) {
        root.style.setProperty('--accent-color', accentColor);
        if (!isDark) {
            // Lighter selection for light mode
            root.style.setProperty('--selection-bg', `${accentColor}40`);
        } else {
            // Slightly more opaque for dark mode visibility
            root.style.setProperty('--selection-bg', `${accentColor}60`);
        }
    }
};

/* =================================================================
 * INITIALIZATION
 * ================================================================= */

globalThis.onload = function () {
    console.log("Editor Loaded.");
};
