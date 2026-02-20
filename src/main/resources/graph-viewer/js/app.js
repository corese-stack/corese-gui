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

function notifyBridge(method, ...args) {
    if (!window.bridge || typeof window.bridge[method] !== 'function') {
        return;
    }
    try {
        window.bridge[method](...args);
    } catch (err) {
        // Ignore bridge callback errors to avoid breaking rendering flow.
    }
}

function asErrorMessage(error) {
    return String(error && error.message ? error.message : error);
}

/**
 * Inject graph content from Base64-encoded JSON-LD.
 * Rendering is scheduled asynchronously to keep JavaFX calls lightweight.
 *
 * @param {string} base64Json - Base64 encoded JSON-LD payload.
 * @param {string} requestId - Render request identifier from Java.
 */
window.renderGraphFromBase64 = function (base64Json, requestId) {
    const renderId = requestId == null ? "" : String(requestId);
    const graphElement = document.getElementById('myGraph');
    if (!graphElement) {
        notifyBridge('onGraphRenderFailed', renderId, 'Graph component not available.');
        return;
    }

    let decoded;
    try {
        decoded = decodeURIComponent(escape(window.atob(base64Json)));
    } catch (error) {
        notifyBridge('onGraphRenderFailed', renderId, asErrorMessage(error));
        return;
    }

    setTimeout(() => {
        try {
            graphElement.jsonld = decoded;
            const warmupStart = performance.now();
            const FALLBACK_WARMUP_TIMEOUT_MS = 1100;

            const waitForWarmup = () => {
                const elapsed = performance.now() - warmupStart;
                const graph = graphElement.graph;
                const nodeCount = Array.isArray(graph?.nodes) ? graph.nodes.length : 0;
                const linkCount = Array.isArray(graph?.links) ? graph.links.length : 0;
                const simulation = graphElement.simulation;
                const staticLayout = !simulation || Boolean(graphElement.simulationStopped);
                const dynamicWarmupTimeout = Math.min(14000, Math.max(FALLBACK_WARMUP_TIMEOUT_MS,
                    Math.round(1200 + nodeCount * 3.1 + linkCount * 0.95)));
                const dynamicMinWait = Math.min(5200, Math.max(260,
                    Math.round(260 + nodeCount * 0.9 + linkCount * 0.28)));
                const dynamicAlphaThreshold = nodeCount >= 2200 || linkCount >= 4200
                    ? 0.10
                    : nodeCount >= 900 || linkCount >= 1800
                        ? 0.12
                        : nodeCount >= 300 || linkCount >= 800
                            ? 0.16
                            : 0.28;
                const settled = !simulation || typeof simulation.alpha !== "function"
                    || simulation.alpha() <= dynamicAlphaThreshold
                    || Boolean(graphElement.simulationStopped);
                if ((elapsed >= dynamicMinWait && settled)
                    || (staticLayout && elapsed >= 180)
                    || elapsed >= dynamicWarmupTimeout) {
                    notifyBridge('onGraphRenderComplete', renderId);
                    return;
                }
                requestAnimationFrame(waitForWarmup);
            };
            requestAnimationFrame(waitForWarmup);
        } catch (error) {
            console.error("Graph rendering failed:", error);
            notifyBridge('onGraphRenderFailed', renderId, asErrorMessage(error));
        }
    }, 0);
};

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

    const darkMode = Boolean(isDark);
    body.classList.toggle('dark-theme', darkMode);

    // Replace theme-* classes without resetting the full className to avoid flashes
    Array.from(body.classList)
        .filter(className => className.startsWith('theme-'))
        .forEach(className => body.classList.remove(className));
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
