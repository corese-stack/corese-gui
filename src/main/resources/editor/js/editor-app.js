/* =================================================================
 * CORESE CODE EDITOR - APPLICATION SCRIPT
 * =================================================================
 * Main application logic for the CodeMirror-based editor
 * Handles initialization, theming, and Java bridge communication
 * ================================================================= */

"use strict";

/* =================================================================
 * JAVA BRIDGE SETUP
 * ================================================================= */

globalThis.onerror = function (msg, url, line, col, error) {
    if (globalThis.bridge) {
        globalThis.bridge.log("JS ERROR: " + msg + " (Line: " + line + ")");
    }
};

var oldLog = console.log;
console.log = function (msg) {
    if (globalThis.bridge) globalThis.bridge.log(msg);
    oldLog.apply(console, arguments);
};

/* =================================================================
 * EDITOR INITIALIZATION
 * ================================================================= */

var editorArea = document.getElementById('editor');
var cm = CodeMirror.fromTextArea(editorArea, {
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
    var pos = cm.getCursor();
    var line = pos.line + 1;
    var col = pos.ch + 1;

    document.getElementById('cursor-position').textContent = 'Ln ' + line + ', Col ' + col;

    var selection = cm.getSelection();
    var selLength = selection.length;
    document.getElementById('selection-count').textContent = selLength > 0 ? '(' + selLength + ' selected)' : '';
}

cm.on("change", function (instance, changeObj) {
    if (changeObj.origin === 'setValue') return;
    if (globalThis.bridge) {
        globalThis.bridge.onContentChanged(instance.getValue());
    }
    updateStatusBar();
});

cm.on("cursorActivity", function () {
    updateStatusBar();
});

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
    var mime = "text/plain";
    var displayName = "Plain Text";
    var tooltip = "";

    switch (modeName.toLowerCase()) {
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
    }

    cm.setOption("mode", mime);
    var modeDisplay = document.getElementById('mode-display');
    modeDisplay.textContent = displayName;
    modeDisplay.title = tooltip;
};

/**
 * Set application theme
 * @param {boolean} isDark - Whether to use dark mode
 * @param {string} accentColor - Optional accent color override
 * @param {string} themeName - Theme name (primer, nord, etc.)
 */
globalThis.setTheme = function (isDark, accentColor, themeName) {
    var root = document.documentElement;
    var body = document.body;

    // Set Dark/Light Mode
    if (isDark) {
        body.classList.add('dark-theme');
    } else {
        body.classList.remove('dark-theme');
    }

    // Set Theme Family (primer, nord, cupertino)
    body.className = body.className.replace(/\btheme-\S+/g, '').trim();
    if (themeName) {
        body.classList.add('theme-' + themeName.toLowerCase());
    }

    if (accentColor) {
        root.style.setProperty('--accent-color', accentColor);
        if (!isDark) {
            // Lighter selection for light mode
            root.style.setProperty('--selection-bg', accentColor + '40');
        } else {
            // Slightly more opaque for dark mode visibility
            root.style.setProperty('--selection-bg', accentColor + '60');
        }
    }
};

/* =================================================================
 * INITIALIZATION
 * ================================================================= */

globalThis.onload = function () {
    console.log("Editor Loaded.");
};
