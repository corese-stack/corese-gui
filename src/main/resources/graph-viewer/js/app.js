/* =================================================================
 * CORESE KNOWLEDGE GRAPH - MAIN APPLICATION SCRIPT
 * =================================================================
 * Main entry point for the knowledge graph visualization. 
 * Handles Java bridge setup, global error handling, and theme management.
 * ================================================================= */

"use strict";

/* =================================================================
 * JAVA BRIDGE SETUP
 * =================================================================
 * Configures the bridge between JavaScript and Java backend.
 * Forwards console logs and errors to the Java application.
 * ================================================================= */

/**
 * Sets up the communication bridge between JS and Java.
 * Overrides console.log and console.error to forward messages to Java.
 */
function setupBridge() {
    // Global error handler
    window.onerror = function (msg, url, line, col, error) {
        if (window.bridge) {
            window.bridge.error(`JS ERROR: ${msg} (Line: ${line})`);
        }
    };

    // Override console.log to forward to Java
    const oldLog = console.log;
    console.log = function (msg) {
        if (window.bridge) {
            window.bridge.log(String(msg));
        }
        oldLog.apply(console, arguments);
    };

    // Override console.error to forward to Java
    const oldError = console.error;
    console.error = function (msg) {
        if (window.bridge) {
            let errorMsg = msg;
            if (msg instanceof Error) {
                errorMsg = `${msg.message}\n${msg.stack}`;
            } else if (typeof msg === 'object') {
                try {
                    errorMsg = JSON.stringify(msg);
                } catch (e) {
                    errorMsg = String(msg);
                }
            }
            window.bridge.error(String(errorMsg));
        }
        oldError.apply(console, arguments);
    };

    console.log("Bridge setup complete.");
}

// Initialize bridge if already available (injected by JavaFX)
if (window.bridge) {
    setupBridge();
}

/* =================================================================
 * GLOBAL API - THEME MANAGEMENT
 * =================================================================
 * Functions exposed to Java bridge for theme switching.
 * ================================================================= */

/**
 * Set application theme.
 * Called from Java to sync the UI theme.
 * 
 * @param {boolean} isDark - Whether to use dark mode.
 * @param {string} [accentColor] - Optional accent color override.
 * @param {string} [themeName] - Theme name (e.g., 'primer', 'nord').
 */
window.setTheme = function (isDark, accentColor, themeName) {
    const body = document.body;
    const root = document.documentElement;

    // Toggle dark mode class
    if (isDark) {
        body.classList.add('dark-theme');
    } else {
        body.classList.remove('dark-theme');
    }

    // Apply named theme
    body.className = body.className.replace(/\btheme-\S+/g, '').trim();
    if (themeName) {
        body.classList.add(`theme-${themeName.toLowerCase()}`);
    }

    // Set custom accent color
    if (accentColor) {
        root.style.setProperty('--accent-color', accentColor);
    }

    // Notify graph component of theme change
    const el = document.getElementById('myGraph');
    if (el && typeof el.setTheme === 'function') {
        el.setTheme(isDark);
    }
};

/**
 * Legacy theme function for backward compatibility.
 * 
 * @param {boolean} isDark - Whether to use dark mode.
 */
window.applyTheme = function (isDark) {
    window.setTheme(isDark, null, null);
};