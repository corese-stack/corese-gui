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
    matchBrackets: true,
    autoCloseBrackets: true,
    indentUnit: 4,
    tabSize: 4,
    lineWrapping: false,
    styleActiveLine: true,
    extraKeys: {
        "Ctrl-Space": "autocomplete",
        "Cmd-/": "toggleComment",
        "Ctrl-/": "toggleComment"
    }
});

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

    switch (mode) {
        case "turtle":
        case "ttl":
        case "n3":
        case "nt":
        case "nq":
        case "trig":
            mime = "text/turtle";
            displayName = "Turtle";
            tooltip = "Trig, Turtle, N-Triples, N-Quads, N3";
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
            displayName = "XML/RDF";
            break;
            
        case "json":
        case "jsonld":
        case "json-ld":
            mime = "application/ld+json";
            displayName = "JSON-LD";
            break;
            
        case "js":
        case "javascript":
            mime = "text/javascript";
            displayName = "JavaScript";
            break;
    }

    cm.setOption("mode", mime);
    
    const modeDisplay = document.getElementById('mode-display');
    if (modeDisplay) {
        modeDisplay.textContent = displayName;
        modeDisplay.title = tooltip;
    }
};

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
