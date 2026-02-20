/* =================================================================
 * CORESE KNOWLEDGE GRAPH - VISUALIZATION COMPONENT
 * =================================================================
 * Custom web component for rendering RDF/JSON-LD knowledge graphs.
 * Uses D3.js for force-directed graph layout and visualization.
 * Supports multiple named graphs with color coding.
 * ================================================================= */

"use strict";

const GRAPH_NODE_FILL = Object.freeze({
    Resource: "#1f77b4",
    Literal: "#ff7f0e",
    Blank: "#2ca02c"
});

const GRAPH_DEFAULTS = Object.freeze({
    defaultGraphId: "default",
    defaultGraphColor: "#6B7280",
    rdfTypePredicate: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
});

const GRAPH_PRESET_COLORS = Object.freeze({
    "urn:corese:inference:rdfs": "#7BC8A4",
    "urn:corese:inference:owlrl": "#F6B26B",
    "urn:corese:inference:owlrl-lite": "#9A86E8",
    "urn:corese:inference:owlrl-ext": "#F08CA0"
});

const GRAPH_COLOR_GENERATION = Object.freeze({
    hueSlotCount: 30,
    hueStride: 11,
    hueRetryStep: 7,
    minDistanceFromPresetHue: 18,
    saturationLevels: [42, 48, 54],
    lightnessLevels: [64, 69, 74]
});

/**
 * Web Component for visualizing a Knowledge Graph.
 * Usage: <kg-graph></kg-graph>
 */
class KGGraphVis extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: "open" });

        // D3 references
        this.svg = null;
        this.simulation = null;
        this.zoomBehavior = null;
        this.linkSelection = null;
        this.nodeSelection = null;
        this.linkLabelSelection = null;
        this.nodeLabelSelection = null;
        this.focusLabelLayer = null;
        this.zoomLayer = null;

        // Component state
        this.internalData = new WeakMap();
        this.internalData.set(this, {});
        this.resizeObserver = null;
        this.showEdgeLabels = true;
        this.edgeLabelsAutoHidden = false;
        this.nodeLabelsVisible = true;
        this.edgeLabelsVisible = true;
        this.currentZoom = 1;
        this.nodeLabelZoomThreshold = 0.2;
        this.edgeLabelZoomThreshold = 0.2;
        this.labelCullEnabled = true;
        this.labelVisibilityThrottleMs = 16;
        this.lastLabelVisibilityUpdate = 0;
        this.labelVisibilityRaf = null;
        this.currentTransform = d3.zoomIdentity;
        this.isInteracting = false;
        this.labelsHiddenForInteraction = false;
        this.interactionTimer = null;
        this.interactionDebounceMs = 140;
        this.interactionHideLabels = true;
        this.interactionHideNodeThreshold = 300;
        this.interactionHideLinkThreshold = 600;
        this.width = 800;
        this.height = 600;
        this.isDarkTheme = false;
        this.graphSummary = this.createEmptyGraphSummary();
        this.componentByNodeId = new Map();
        this.componentTargets = new Map();
        this.pendingAutoFit = false;
        this.pendingAutoFitTimer = null;
        this.pendingRecenterTimer = null;
        this.legendStackElement = null;
        this.globalLegendElement = null;
        this.namedLegendElement = null;
        this.simplifyLinkGeometry = false;
        this.baseRenderProfile = {
            mode: "normal",
            summary: "Standard rendering",
            details: []
        };
        this.lastEffectiveRenderProfileKey = "";
        this.hoveredNodeId = null;
        this.linkIndexesByNodeId = new Map();
        this.HOVER_FOCUS_MAX_LINK_LABELS = 140;
        this.tooltipShowTimerHandle = null;
        this.tooltipMoveTimerHandle = null;

        // Graph coloring
        this.graphColorMap = new Map();
        this.graphContextPrefixes = new Map();
        this.defaultGraphColor = GRAPH_DEFAULTS.defaultGraphColor;
        this.reservedGraphHues = [];

        // Performance optimization
        this.TICK_THROTTLE = 16; // ~60fps throttle for ticked()
        this.lastTickTime = 0;
        this.LABEL_TICK_THROTTLE_SMALL = 16; // ~60fps for small graphs
        this.LABEL_TICK_THROTTLE_LARGE = 24;
        this.LABEL_TICK_THROTTLE_VERY_LARGE = 50;
        this.LABEL_VISIBILITY_THROTTLE_SMALL = 16;
        this.LABEL_VISIBILITY_THROTTLE_LARGE = 40;
        this.LABEL_VISIBILITY_THROTTLE_VERY_LARGE = 80;
        this.LABEL_TICK_THROTTLE = this.LABEL_TICK_THROTTLE_SMALL;
        this.lastLabelUpdate = 0;
        this.simulationStopped = false;
        this.AUTO_STOP_ALPHA = 0.005; // Alpha threshold for auto-stabilize (very low)
        this.PARALLEL_LINK_SPACING = 30;
        this.PARALLEL_LINK_MAX_CURVE = 140;
        this.SELF_LOOP_ATTACH_HALF_GAP = 0.2;
        this.SELF_LOOP_MAX_ANGULAR_SPREAD = 2.0;
        this.SELF_LOOP_ANGULAR_STEP = 0.8;
        this.SELF_LOOP_LENGTH_BASE = 92;
        this.SELF_LOOP_LENGTH_STEP = 14;
        this.SELF_LOOP_WIDTH_BASE = 26;
        this.SELF_LOOP_WIDTH_STEP = 8;
        this.SELF_LOOP_LABEL_OUTWARD = -18;
        this.SELF_LOOP_LABEL_SIDE_STEP = 4;
        this.LARGE_GRAPH_NODE_THRESHOLD = 180;
        this.LARGE_GRAPH_LINK_THRESHOLD = 420;
        this.HUGE_GRAPH_NODE_THRESHOLD = 700;
        this.HUGE_GRAPH_LINK_THRESHOLD = 1300;
        this.AUTO_HIDE_EDGE_LABEL_THRESHOLD = 280;
        this.AUTO_HIDE_NODE_LABEL_THRESHOLD = 520;
        this.DISABLE_LABEL_CREATION_NODE_THRESHOLD = 380;
        this.DISABLE_LABEL_CREATION_LINK_THRESHOLD = 620;
        this.DISABLE_TOOLTIP_NODE_THRESHOLD = 1800;
        this.DISABLE_TOOLTIP_LINK_THRESHOLD = 3600;
        this.SIMPLIFY_LINK_GEOMETRY_LINK_THRESHOLD = 500;
        this.PARALLEL_LAYOUT_MAX_LINK_THRESHOLD = 600;
        this.AUTO_OVERVIEW_PADDING = 760;
        this.AUTO_OVERVIEW_MAX_SCALE = 0.09;
        this.AUTO_RECENTER_DELAY_MS = 560;
    }

    /* -------------------------------------------------------------
     * PUBLIC API METHODS
     * ------------------------------------------------------------- */

    /**
     * Toggle visibility of edge labels.
     * @param {boolean} show - Whether to show edge labels.
     */
    setShowEdgeLabels(show) {
        this.showEdgeLabels = show;
        this.updateLevelOfDetail(this.currentZoom);
    }

    /**
     * Reset simulation with new alpha (re-heats the simulation).
     */
    reset() {
        if (this.simulation) {
            this.simulationStopped = false;
            this.simulation.alphaTarget(0).alpha(1).restart();
        }
    }

    recenter() {
        this.fitToGraph(120, true);
    }

    /**
     * Zoom in by 30%.
     */
    zoomIn() {
        if (this.svg && this.zoomBehavior) {
            this.svg.interrupt();
            this.svg.call(this.zoomBehavior.scaleBy, 1.3);
        }
    }

    /**
     * Zoom out by 30%.
     */
    zoomOut() {
        if (this.svg && this.zoomBehavior) {
            this.svg.interrupt();
            this.svg.call(this.zoomBehavior.scaleBy, 1 / 1.3);
        }
    }

    /**
     * Export SVG with full label detail (independent of current zoom).
     * @returns {string|null} SVG string or null.
     */
    exportSvg() {
        if (!this.shadowRoot) return null;
        const svg = this.shadowRoot.querySelector('svg');
        if (!svg) return null;

        const prevNodeVisible = this.nodeLabelsVisible;
        const prevEdgeVisible = this.edgeLabelsVisible;
        if (this.nodeLabelSelection) {
            this.nodeLabelSelection.style('visibility', 'visible').style('opacity', 1);
        }
        if (this.linkLabelSelection) {
            this.linkLabelSelection
                .style('visibility', this.showEdgeLabels ? 'visible' : 'hidden')
                .style('opacity', this.showEdgeLabels ? 1 : 0);
        }
        if (this.nodeSelection) {
            this.nodeSelection.style('display', null);
        }
        if (this.linkSelection) {
            this.linkSelection.style('display', null);
        }

        const clone = svg.cloneNode(true);
        try {
            const bbox = svg.getBBox();
            const padding = 40;
            const x = bbox.x - padding;
            const y = bbox.y - padding;
            const width = bbox.width + 2 * padding;
            const height = bbox.height + 2 * padding;
            clone.setAttribute('viewBox', `${x} ${y} ${width} ${height}`);
            clone.setAttribute('width', width);
            clone.setAttribute('height', height);
            const bg = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            bg.setAttribute('x', x);
            bg.setAttribute('y', y);
            bg.setAttribute('width', width);
            bg.setAttribute('height', height);
            bg.setAttribute('fill', 'white');
            clone.insertBefore(bg, clone.firstChild);
        } catch (e) {
            console.warn('Could not adjust SVG bounds:', e);
        }

        const serializer = new XMLSerializer();
        const result = serializer.serializeToString(clone);

        if (this.nodeLabelSelection) {
            this.nodeLabelSelection
                .style('visibility', prevNodeVisible ? 'visible' : 'hidden')
                .style('opacity', prevNodeVisible ? 1 : 0);
        }
        if (this.linkLabelSelection) {
            this.linkLabelSelection
                .style('visibility', prevEdgeVisible ? 'visible' : 'hidden')
                .style('opacity', prevEdgeVisible ? 1 : 0);
        }
        this.updateLevelOfDetail(this.currentZoom);
        this.scheduleLabelVisibilityUpdate();
        return result;
    }

    /**
     * Update theme.
     * Styles are handled via CSS variables, so this is mainly a hook if JS logic is needed.
     * @param {boolean} isDark - Whether dark mode is active.
     */
    setTheme(isDark) {
        this.isDarkTheme = Boolean(isDark);
        this.refreshOverlayPanels();
    }

    /* -------------------------------------------------------------
     * PROPERTY ACCESSORS
     * ------------------------------------------------------------- */

    /**
     * Sets the JSON-LD data to display.
     * @param {string} jsonld - The JSON-LD string.
     */
    set jsonld(jsonld) {
        const data = this.internalData.get(this) || {};
        if (data.jsonld === jsonld) {
            return;
        }
        data.jsonld = jsonld;
        this.internalData.set(this, data);
        this.drawChart();
    }

    /**
     * Gets the current JSON-LD data.
     * @returns {string} The JSON-LD string.
     */
    get jsonld() {
        return this.internalData.get(this)?.jsonld;
    }

    /* -------------------------------------------------------------
     * LIFECYCLE METHODS
     * ------------------------------------------------------------- */

    connectedCallback() {
        this.render();
        this.observeResize();
        this.updateSize();
    }

    disconnectedCallback() {
        if (this.resizeObserver) this.resizeObserver.disconnect();
        if (this.simulation) this.simulation.stop();
        this.clearAutoFitTimers();
        if (this.interactionTimer) {
            clearTimeout(this.interactionTimer);
            this.interactionTimer = null;
        }
        if (this.labelVisibilityRaf) {
            cancelAnimationFrame(this.labelVisibilityRaf);
            this.labelVisibilityRaf = null;
        }
    }

    /* -------------------------------------------------------------
     * COLOR MANAGEMENT
     * ------------------------------------------------------------- */

    normalizeGraphId(graphId) {
        if (graphId == null) {
            return GRAPH_DEFAULTS.defaultGraphId;
        }
        const normalized = String(graphId).trim();
        return normalized || GRAPH_DEFAULTS.defaultGraphId;
    }

    rebuildGraphContextPrefixes() {
        this.graphContextPrefixes = new Map();
        this.collectJsonLdContexts(this.jsonLDOntology, context => this.mergeJsonLdContext(context, this.graphContextPrefixes));
    }

    collectJsonLdContexts(node, consumeContext) {
        if (!node) {
            return;
        }
        if (Array.isArray(node)) {
            node.forEach(entry => this.collectJsonLdContexts(entry, consumeContext));
            return;
        }
        if (typeof node !== "object") {
            return;
        }
        if (Object.prototype.hasOwnProperty.call(node, "@context")) {
            consumeContext(node["@context"]);
        }
        if (Object.prototype.hasOwnProperty.call(node, "@graph")) {
            this.collectJsonLdContexts(node["@graph"], consumeContext);
        }
    }

    mergeJsonLdContext(context, prefixMap) {
        if (!context) {
            return;
        }
        if (Array.isArray(context)) {
            context.forEach(entry => this.mergeJsonLdContext(entry, prefixMap));
            return;
        }
        if (typeof context !== "object") {
            return;
        }
        Object.entries(context).forEach(([key, value]) => {
            if (key === "@vocab" && typeof value === "string" && value) {
                prefixMap.set("", value);
                return;
            }
            if (key.startsWith("@")) {
                return;
            }
            if (typeof value === "string" && value) {
                prefixMap.set(key, value);
                return;
            }
            if (value && typeof value === "object" && typeof value["@id"] === "string" && value["@id"]) {
                prefixMap.set(key, value["@id"]);
            }
        });
    }

    resolveGraphColorKey(graphId) {
        const gid = this.normalizeGraphId(graphId);
        if (gid === GRAPH_DEFAULTS.defaultGraphId) {
            return gid;
        }
        if (gid.startsWith("_:") || gid.includes("://") || gid.startsWith("urn:")) {
            return gid;
        }

        const separatorIndex = gid.indexOf(":");
        if (separatorIndex < 0) {
            return gid;
        }
        const prefix = gid.substring(0, separatorIndex);
        const localName = gid.substring(separatorIndex + 1);
        if (!localName) {
            return gid;
        }

        const namespace = this.graphContextPrefixes?.get(prefix);
        if (typeof namespace === "string" && namespace) {
            return `${namespace}${localName}`;
        }
        return gid;
    }

    resolveReservedGraphHues() {
        return Object.values(GRAPH_PRESET_COLORS)
            .map(color => this.hexToHsl(color))
            .filter(hsl => Number.isFinite(hsl.h) && hsl.s > 0)
            .map(hsl => hsl.h);
    }

    stableHash(input) {
        const value = String(input ?? "");
        let hash = 2166136261;
        for (let i = 0; i < value.length; i += 1) {
            hash ^= value.charCodeAt(i);
            hash = Math.imul(hash, 16777619);
        }
        return hash >>> 0;
    }

    hueDistance(left, right) {
        const raw = Math.abs(left - right) % 360;
        return raw > 180 ? 360 - raw : raw;
    }

    isHueTooCloseToReserved(candidateHue, minDistance = GRAPH_COLOR_GENERATION.minDistanceFromPresetHue) {
        if (!Array.isArray(this.reservedGraphHues) || this.reservedGraphHues.length === 0) {
            return false;
        }
        return this.reservedGraphHues.some(reservedHue => this.hueDistance(candidateHue, reservedHue) < minDistance);
    }

    buildStableGraphColor(graphId) {
        const hueHash = this.stableHash(`${graphId}|h`);
        const saturationHash = this.stableHash(`${graphId}|s`);
        const lightnessHash = this.stableHash(`${graphId}|l`);

        const config = GRAPH_COLOR_GENERATION;
        const hueStep = 360 / config.hueSlotCount;
        const baseHueIndex = (hueHash * config.hueStride) % config.hueSlotCount;
        const saturation = config.saturationLevels[saturationHash % config.saturationLevels.length];
        const lightness = config.lightnessLevels[lightnessHash % config.lightnessLevels.length];

        for (let attempt = 0; attempt < config.hueSlotCount; attempt += 1) {
            const candidateIndex = (baseHueIndex + attempt * config.hueRetryStep) % config.hueSlotCount;
            const candidateHue = candidateIndex * hueStep;
            if (!this.isHueTooCloseToReserved(candidateHue)) {
                return this.hslToHex(candidateHue, saturation, lightness);
            }
        }

        return this.hslToHex(baseHueIndex * hueStep, saturation, lightness);
    }

    resolvePresetGraphColor(graphId) {
        return GRAPH_PRESET_COLORS[graphId] ?? null;
    }

    /**
     * Generate a deterministic color for each named graph.
     * @param {string} graphId - Graph identifier.
     * @returns {string} Hex color code.
     */
    getGraphColor(graphId = 'default') {
        const gid = this.normalizeGraphId(graphId);
        if (gid === GRAPH_DEFAULTS.defaultGraphId) {
            return this.defaultGraphColor;
        }
        const graphColorKey = this.resolveGraphColorKey(gid);

        if (this.reservedGraphHues.length === 0) {
            this.reservedGraphHues = this.resolveReservedGraphHues();
        }

        if (!this.graphColorMap.has(graphColorKey)) {
            const presetColor = this.resolvePresetGraphColor(graphColorKey);
            if (presetColor) {
                this.graphColorMap.set(graphColorKey, presetColor);
                return presetColor;
            }
            this.graphColorMap.set(graphColorKey, this.buildStableGraphColor(graphColorKey));
        }
        return this.graphColorMap.get(graphColorKey);
    }

    hexToHsl(hex) {
        const normalized = String(hex ?? "").trim();
        const match = /^#([0-9a-fA-F]{6})$/.exec(normalized);
        if (!match) {
            return { h: NaN, s: 0, l: 0 };
        }
        const value = match[1];
        const r = parseInt(value.substring(0, 2), 16) / 255;
        const g = parseInt(value.substring(2, 4), 16) / 255;
        const b = parseInt(value.substring(4, 6), 16) / 255;
        const max = Math.max(r, g, b);
        const min = Math.min(r, g, b);
        const delta = max - min;
        const lightness = (max + min) / 2;
        let hue = NaN;
        let saturation = 0;

        if (delta !== 0) {
            saturation = delta / (1 - Math.abs(2 * lightness - 1));
            switch (max) {
                case r:
                    hue = 60 * (((g - b) / delta) % 6);
                    break;
                case g:
                    hue = 60 * ((b - r) / delta + 2);
                    break;
                default:
                    hue = 60 * ((r - g) / delta + 4);
                    break;
            }
            if (hue < 0) {
                hue += 360;
            }
        }

        return { h: hue, s: saturation, l: lightness };
    }

    /**
     * Convert HSL color values to hex format.
     * @param {number} h - Hue (0-360).
     * @param {number} s - Saturation (0-100).
     * @param {number} l - Lightness (0-100).
     * @returns {string} Hex color code.
     */
    hslToHex(h, s, l) {
        const hue = ((h % 360) + 360) % 360;
        const sat = s / 100;
        const light = l / 100;
        const c = (1 - Math.abs(2 * light - 1)) * sat;
        const x = c * (1 - Math.abs((hue / 60) % 2 - 1));
        const m = light - c / 2;
        let r = 0;
        let g = 0;
        let b = 0;

        switch (Math.floor(hue / 60)) {
            case 0:
                r = c;
                g = x;
                break;
            case 1:
                r = x;
                g = c;
                break;
            case 2:
                g = c;
                b = x;
                break;
            case 3:
                g = x;
                b = c;
                break;
            case 4:
                r = x;
                b = c;
                break;
            case 5:
                r = c;
                b = x;
                break;
        }

        const toHex = val => {
            const hex = Math.round((val + m) * 255).toString(16);
            return hex.length === 1 ? '0' + hex : hex;
        };
        return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
    }

    /**
     * Sanitize string for use as SVG ID.
     * @param {string} str - Input string.
     * @returns {string} Sanitized string.
     */
    sanitizeId(str = 'default') {
        const safe = (str ?? 'default').toString();
        return safe.replaceAll(/[^a-zA-Z0-9-_]/g, '_');
    }

    getLinkEndpointId(endpoint) {
        if (endpoint && typeof endpoint === 'object' && endpoint.id) {
            return endpoint.id;
        }
        return endpoint ?? null;
    }

    sortLinksForStableLayout(links = []) {
        links.sort((a, b) => {
            const sourceA = this.getLinkEndpointId(a?.source) || '';
            const targetA = this.getLinkEndpointId(a?.target) || '';
            const nameA = a?.name || '';
            const graphA = a?.graph || '';

            const sourceB = this.getLinkEndpointId(b?.source) || '';
            const targetB = this.getLinkEndpointId(b?.target) || '';
            const nameB = b?.name || '';
            const graphB = b?.graph || '';

            return sourceA.localeCompare(sourceB)
                || targetA.localeCompare(targetB)
                || nameA.localeCompare(nameB)
                || graphA.localeCompare(graphB);
        });
    }

    resetLinkOffsets(links = []) {
        if (!Array.isArray(links)) return;
        links.forEach(link => {
            link.parallelOffsetUnit = 0;
            link.loopOffsetUnit = 0;
            link.loopIndex = 0;
            link.loopGroupSize = 1;
            link.hasOppositeDirection = false;
        });
    }

    assignParallelLinkOffsets(links = []) {
        if (!Array.isArray(links)) return;

        const pairGroups = new Map();
        const selfLoopGroups = new Map();
        const setCenteredOffsets = (groupLinks, fieldName = "parallelOffsetUnit") => {
            const center = (groupLinks.length - 1) / 2;
            groupLinks.forEach((link, index) => {
                link[fieldName] = index - center;
            });
        };
        const setDirectionalOffsets = groupLinks => {
            groupLinks.forEach((link, index) => {
                link.parallelOffsetUnit = 0.9 + index;
            });
        };

        this.resetLinkOffsets(links);
        links.forEach(link => {
            const sourceId = this.getLinkEndpointId(link?.source);
            const targetId = this.getLinkEndpointId(link?.target);
            if (!sourceId || !targetId) return;
            if (sourceId === targetId) {
                if (!selfLoopGroups.has(sourceId)) {
                    selfLoopGroups.set(sourceId, []);
                }
                selfLoopGroups.get(sourceId).push(link);
                return;
            }

            const [minId, maxId] = sourceId <= targetId
                ? [sourceId, targetId]
                : [targetId, sourceId];
            const pairKey = `${minId}__${maxId}`;
            if (!pairGroups.has(pairKey)) {
                pairGroups.set(pairKey, {
                    minId,
                    maxId,
                    forward: [],
                    backward: []
                });
            }
            const group = pairGroups.get(pairKey);
            const isForward = sourceId === minId && targetId === maxId;
            (isForward ? group.forward : group.backward).push(link);
        });

        pairGroups.forEach(group => {
            this.sortLinksForStableLayout(group.forward);
            this.sortLinksForStableLayout(group.backward);
            const hasBothDirections = group.forward.length > 0 && group.backward.length > 0;

            if (hasBothDirections) {
                // Keep offsets on the same sign for both directions.
                // Because source/target are reversed, the same sign naturally bends
                // paths on opposite sides and avoids overlap.
                setDirectionalOffsets(group.forward);
                setDirectionalOffsets(group.backward);
                group.forward.forEach(link => {
                    link.hasOppositeDirection = true;
                });
                group.backward.forEach(link => {
                    link.hasOppositeDirection = true;
                });
                return;
            }

            const singleDirection = group.forward.length > 0 ? group.forward : group.backward;
            setCenteredOffsets(singleDirection);
        });

        selfLoopGroups.forEach(groupLinks => {
            this.sortLinksForStableLayout(groupLinks);
            setCenteredOffsets(groupLinks, "loopOffsetUnit");
            const loopGroupSize = groupLinks.length;
            groupLinks.forEach((link, index) => {
                link.loopIndex = index;
                link.loopGroupSize = loopGroupSize;
            });
        });
    }

    buildLinkIndexByNodeId(links = []) {
        const index = new Map();
        if (!Array.isArray(links)) {
            return index;
        }
        links.forEach((link, linkIndex) => {
            const sourceId = this.getLinkEndpointId(link?.source);
            const targetId = this.getLinkEndpointId(link?.target);
            if (!sourceId || !targetId) {
                return;
            }
            if (!index.has(sourceId)) {
                index.set(sourceId, []);
            }
            index.get(sourceId).push(linkIndex);
            if (targetId !== sourceId) {
                if (!index.has(targetId)) {
                    index.set(targetId, []);
                }
                index.get(targetId).push(linkIndex);
            }
        });
        return index;
    }

    clearHoverFocus(restoreLabels = true) {
        this.hoveredNodeId = null;
        if (this.linkSelection) {
            this.linkSelection
                .classed("edge-focused", false)
                .classed("edge-dimmed", false);
        }
        if (this.nodeSelection) {
            this.nodeSelection
                .classed("node-focused", false)
                .classed("node-neighbor", false)
                .classed("node-dimmed", false);
        }
        if (this.focusLabelLayer) {
            this.focusLabelLayer.selectAll("*").remove();
        }
        if (!restoreLabels || this.labelsHiddenForInteraction) {
            return;
        }
        if (this.linkLabelSelection) {
            this.updateLabelVisibility();
        }
    }

    applyHoverFocus(node) {
        if (!node || this.isInteracting || this.labelsHiddenForInteraction) {
            return;
        }
        if (!this.graph || !Array.isArray(this.graph.links) || !this.linkSelection || !this.nodeSelection) {
            return;
        }

        const hoveredId = this.getLinkEndpointId(node?.id ?? node);
        if (!hoveredId) {
            return;
        }

        const linkIndexes = this.linkIndexesByNodeId.get(hoveredId) ?? [];
        if (linkIndexes.length <= 0) {
            this.clearHoverFocus(false);
            return;
        }

        this.hoveredNodeId = hoveredId;
        const connectedLinks = [];
        const connectedLinkSet = new Set();
        const neighborIds = new Set([hoveredId]);

        linkIndexes.forEach(index => {
            const link = this.graph.links[index];
            if (!link) {
                return;
            }
            connectedLinks.push(link);
            connectedLinkSet.add(link);
            const sourceId = this.getLinkEndpointId(link.source);
            const targetId = this.getLinkEndpointId(link.target);
            if (sourceId) {
                neighborIds.add(sourceId);
            }
            if (targetId) {
                neighborIds.add(targetId);
            }
        });

        this.linkSelection
            .classed("edge-focused", link => connectedLinkSet.has(link))
            .classed("edge-dimmed", link => !connectedLinkSet.has(link));

        this.nodeSelection
            .classed("node-focused", graphNode => this.getLinkEndpointId(graphNode?.id ?? graphNode) === hoveredId)
            .classed("node-neighbor", graphNode => {
                const graphNodeId = this.getLinkEndpointId(graphNode?.id ?? graphNode);
                return graphNodeId && graphNodeId !== hoveredId && neighborIds.has(graphNodeId);
            })
            .classed("node-dimmed", graphNode => {
                const graphNodeId = this.getLinkEndpointId(graphNode?.id ?? graphNode);
                return !graphNodeId || !neighborIds.has(graphNodeId);
            });

        const maxLabels = Math.max(0, Math.floor(this.HOVER_FOCUS_MAX_LINK_LABELS));
        const focusedLinksForLabels = maxLabels > 0 ? connectedLinks.slice(0, maxLabels) : [];
        const focusedLabelSet = new Set(focusedLinksForLabels);

        if (this.linkLabelSelection) {
            this.refreshEdgeLabelPositions(true);
            this.linkLabelSelection
                .style("visibility", link => focusedLabelSet.has(link) ? "visible" : "hidden")
                .style("opacity", link => focusedLabelSet.has(link) ? 1 : 0);
            return;
        }

        if (!this.focusLabelLayer) {
            return;
        }

        const focusLabels = this.focusLabelLayer
            .selectAll("text.edge-label-hover")
            .data(focusedLinksForLabels);

        focusLabels.exit().remove();

        focusLabels
            .enter()
            .append("text")
            .attr("class", "edge-label edge-label-hover")
            .attr("text-anchor", "middle")
            .merge(focusLabels)
            .text(link => this.formatLabel(link?.name ?? ""))
            .attr("x", link => {
                const geometry = link?.geometry ?? this.buildLinkGeometry(link);
                return geometry ? geometry.labelX : null;
            })
            .attr("y", link => {
                const geometry = link?.geometry ?? this.buildLinkGeometry(link);
                return geometry ? geometry.labelY : null;
            })
            .attr("text-anchor", link => {
                const geometry = link?.geometry ?? this.buildLinkGeometry(link);
                return geometry?.labelAnchor ?? "middle";
            })
            .style("visibility", "visible")
            .style("opacity", 1);
    }

    buildLinkGeometry(link) {
        const source = link?.source;
        const target = link?.target;
        if (!source || !target) return null;
        if (!Number.isFinite(source.x) || !Number.isFinite(source.y)
            || !Number.isFinite(target.x) || !Number.isFinite(target.y)) {
            return null;
        }

        const sx = source.x;
        const sy = source.y;
        const tx = target.x;
        const ty = target.y;
        const sourceId = this.getLinkEndpointId(source);
        const targetId = this.getLinkEndpointId(target);
        const isSelfLoop = sourceId && targetId && sourceId === targetId;
        const dx = tx - sx;
        const dy = ty - sy;
        const distance = Math.hypot(dx, dy);
        const unitX = distance >= 1e-6 ? dx / distance : 0;
        const unitY = distance >= 1e-6 ? dy / distance : 0;
        const sourceRadius = this.getNodeVisualRadius(source) + 2;
        const targetRadius = this.getNodeVisualRadius(target) + 10;
        const baseStartX = sx + unitX * sourceRadius;
        const baseStartY = sy + unitY * sourceRadius;
        const baseEndX = tx - unitX * targetRadius;
        const baseEndY = ty - unitY * targetRadius;

        if (this.simplifyLinkGeometry && !isSelfLoop && distance >= 1e-6) {
            const midpointX = (baseStartX + baseEndX) / 2;
            const midpointY = (baseStartY + baseEndY) / 2;
            return {
                path: `M${baseStartX},${baseStartY} L${baseEndX},${baseEndY}`,
                labelX: midpointX,
                labelY: midpointY,
                labelAnchor: "middle"
            };
        }

        if (isSelfLoop || distance < 1e-6) {
            const loopUnit = Number.isFinite(link?.loopOffsetUnit)
                ? link.loopOffsetUnit
                : (Number.isFinite(link?.parallelOffsetUnit) ? link.parallelOffsetUnit : 0);
            const loopGroupSize = Number.isFinite(link?.loopGroupSize)
                ? Math.max(1, Math.floor(link.loopGroupSize))
                : 1;
            const loopIndex = Number.isFinite(link?.loopIndex)
                ? Math.max(0, Math.min(loopGroupSize - 1, Math.floor(link.loopIndex)))
                : 0;
            const nodeRadius = this.getNodeVisualRadius(source);
            const rankDistance = Math.abs(loopUnit);
            const normalizedIndex = loopGroupSize === 1
                ? 0
                : (loopIndex / (loopGroupSize - 1)) * 2 - 1;
            const angularSpread = loopGroupSize === 1
                ? 0
                : Math.min(this.SELF_LOOP_MAX_ANGULAR_SPREAD, (loopGroupSize - 1) * this.SELF_LOOP_ANGULAR_STEP);
            const loopAnchorAngle = -Math.PI / 2 + normalizedIndex * (angularSpread / 2);
            const attachHalfGap = Math.min(0.42,
                this.SELF_LOOP_ATTACH_HALF_GAP + rankDistance * 0.02 + Math.abs(normalizedIndex) * 0.07);
            const attachRadius = nodeRadius + 2;
            const outwardSide = normalizedIndex === 0 ? 1 : Math.sign(normalizedIndex);
            // Keep marker end on the outer side of each loop so multi-loops do not
            // cross arrow heads near the node.
            const startAngle = loopAnchorAngle - outwardSide * attachHalfGap;
            const endAngle = loopAnchorAngle + outwardSide * attachHalfGap;
            const startX = sx + Math.cos(startAngle) * attachRadius;
            const startY = sy + Math.sin(startAngle) * attachRadius;
            const endX = sx + Math.cos(endAngle) * attachRadius;
            const endY = sy + Math.sin(endAngle) * attachRadius;
            const outwardX = Math.cos(loopAnchorAngle);
            const outwardY = Math.sin(loopAnchorAngle);
            const tangentX = -outwardY;
            const tangentY = outwardX;
            const sideOffset = normalizedIndex * (22 + rankDistance * 6);
            const loopLength = this.SELF_LOOP_LENGTH_BASE + rankDistance * this.SELF_LOOP_LENGTH_STEP;
            const loopWidth = this.SELF_LOOP_WIDTH_BASE + rankDistance * this.SELF_LOOP_WIDTH_STEP;
            const controlBaseX = sx + outwardX * loopLength + tangentX * sideOffset;
            const controlBaseY = sy + outwardY * loopLength + tangentY * sideOffset;
            const control1X = controlBaseX + tangentX * loopWidth;
            const control1Y = controlBaseY + tangentY * loopWidth;
            const control2X = controlBaseX - tangentX * loopWidth;
            const control2Y = controlBaseY - tangentY * loopWidth;
            const labelSide = Math.sign(normalizedIndex)
                * (2 + Math.abs(normalizedIndex) * this.SELF_LOOP_LABEL_SIDE_STEP);
            // Negative outward offset pulls the label inside the loop area so it
            // stays visually attached to the cycle instead of drifting too high.
            const labelOutward = this.SELF_LOOP_LABEL_OUTWARD + rankDistance + Math.abs(normalizedIndex);
            const labelX = controlBaseX + outwardX * labelOutward + tangentX * labelSide;
            const labelY = controlBaseY + outwardY * labelOutward + tangentY * labelSide;
            const labelAnchor = normalizedIndex > 0 ? "start" : (normalizedIndex < 0 ? "end" : "middle");

            return {
                // Self-loop as an elongated tear-drop shape:
                // start/end stay close to the node, while control points extend outward.
                path: `M${startX},${startY} C${control1X},${control1Y} ${control2X},${control2Y} ${endX},${endY}`,
                labelX,
                labelY,
                labelAnchor
            };
        }

        const offsetUnit = Number.isFinite(link?.parallelOffsetUnit) ? link.parallelOffsetUnit : 0;
        const maxForDistance = Math.min(this.PARALLEL_LINK_MAX_CURVE, Math.max(distance * 0.42, 0));
        const rawCurveOffset = offsetUnit * this.PARALLEL_LINK_SPACING;
        let curveOffset = Math.max(-maxForDistance, Math.min(maxForDistance, rawCurveOffset));
        if (link?.hasOppositeDirection && Math.abs(offsetUnit) > 0.01) {
            const minBidirectionalCurve = Math.min(maxForDistance, Math.max(22, distance * 0.24));
            if (Math.abs(curveOffset) < minBidirectionalCurve) {
                const sign = Math.sign(curveOffset || offsetUnit || 1);
                curveOffset = sign * minBidirectionalCurve;
            }
        }

        const normalX = -dy / distance;
        const normalY = dx / distance;
        const endpointSpread = Math.max(-10, Math.min(10, offsetUnit * 3));
        const startX = baseStartX + normalX * endpointSpread;
        const startY = baseStartY + normalY * endpointSpread;
        const endX = baseEndX + normalX * endpointSpread;
        const endY = baseEndY + normalY * endpointSpread;
        const midpointX = (startX + endX) / 2;
        const midpointY = (startY + endY) / 2;

        if (Math.abs(curveOffset) < 0.5) {
            return {
                path: `M${startX},${startY} L${endX},${endY}`,
                labelX: midpointX,
                labelY: midpointY,
                labelAnchor: "middle"
            };
        }

        const controlX = midpointX + normalX * curveOffset;
        const controlY = midpointY + normalY * curveOffset;
        const t = 0.5;
        const invT = 1 - t;
        const labelX = invT * invT * startX + 2 * invT * t * controlX + t * t * endX;
        const labelY = invT * invT * startY + 2 * invT * t * controlY + t * t * endY;

        return {
            path: `M${startX},${startY} Q${controlX},${controlY} ${endX},${endY}`,
            labelX,
            labelY,
            labelAnchor: "middle"
        };
    }

    /* -------------------------------------------------------------
     * RENDERING METHODS
     * ------------------------------------------------------------- */

    /**
     * Render Shadow DOM structure with styles.
     */
    render() {
        this.shadowRoot.innerHTML = `
            <style>
                :host { 
                    display: block; 
                    width: 100%; 
                    height: 100%; 
                    user-select: none; 
                    -webkit-user-select: none; 
                }
                .container { 
                    position: relative;
                    width: 100%; 
                    height: 100%; 
                    box-sizing: border-box; 
                    overflow: hidden;
                }
                svg { 
                    position: absolute;
                    inset: 0;
                    width: 100%; 
                    height: 100%; 
                    display: block; 
                    cursor: grab; 
                    user-select: none; 
                    background: var(--bg-color, #ffffff);
                }
                svg:active { 
                    cursor: grabbing; 
                }
                svg.labels-hidden .node-label,
                svg.labels-hidden .edge-label {
                    visibility: hidden;
                    opacity: 0;
                }
                svg.node-labels-off .node-label {
                    visibility: hidden;
                    opacity: 0;
                }
                svg.edge-labels-off .edge-label {
                    visibility: hidden;
                    opacity: 0;
                }
                svg.interaction-active .node-label,
                svg.interaction-active .edge-label {
                    visibility: hidden;
                    opacity: 0;
                }
                .node-label {
                    pointer-events: none; 
                    font-family: 'Segoe UI', system-ui, sans-serif;
                    font-size: 13px; 
                    font-weight: 600;
                    fill: var(--text-color, #333);
                    visibility: visible;
                    opacity: 1;
                    text-rendering: geometricPrecision;
                }
                .edge-label { 
                    font-size: 11px; 
                    pointer-events: none; 
                    fill: var(--text-color, #333);
                    visibility: visible;
                    opacity: 1;
                    text-rendering: geometricPrecision;
                }
                .edge-path {
                    fill: none;
                    stroke-linecap: round;
                    stroke-linejoin: round;
                    shape-rendering: geometricPrecision;
                }
                .edge-path.edge-dimmed {
                    opacity: 0.16;
                }
                .edge-path.edge-focused {
                    opacity: 1;
                    stroke-width: 2.9;
                }
                .nodes-layer g.node-dimmed {
                    opacity: 0.28;
                }
                .nodes-layer g.node-neighbor {
                    opacity: 0.9;
                }
                .nodes-layer g.node-focused {
                    opacity: 1;
                }
                .nodes-layer g.node-focused circle,
                .nodes-layer g.node-focused rect {
                    stroke-width: 3.2;
                }
                .edge-label-hover {
                    font-weight: 700;
                    paint-order: stroke;
                    stroke: var(--bg-color, #ffffff);
                    stroke-width: 3px;
                    stroke-linejoin: round;
                }
                .overlay-panel {
                    background: var(--bg-color, #ffffff);
                    opacity: 0.72;
                    border: 1px solid var(--status-bar-border, #d0d7de);
                    border-radius: 6px;
                    box-shadow: 0 6px 14px rgba(0, 0, 0, 0.15);
                    color: var(--text-color, #24292e);
                    font-family: 'Segoe UI', system-ui, sans-serif;
                    transition: opacity 0.2s ease;
                }
                .overlay-panel:hover {
                    opacity: 1;
                }
                .legend-stack {
                    position: absolute;
                    z-index: 4;
                    top: 12px;
                    right: 12px;
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                    align-items: stretch;
                    pointer-events: none;
                }
                .legend-stack > .overlay-panel {
                    pointer-events: auto;
                }
                .legend-global {
                    min-width: 172px;
                    padding: 8px 10px 8px;
                }
                .legend-named {
                    min-width: 220px;
                    max-width: min(360px, 45vw);
                    max-height: min(260px, 40vh);
                    overflow: auto;
                    padding: 8px 10px;
                }
                .legend-title {
                    font-size: 11px;
                    font-weight: 700;
                    letter-spacing: 0.03em;
                    text-transform: uppercase;
                    color: var(--gutter-text, #6e7781);
                    margin: 0 0 7px 0;
                }
                .legend-row {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 12px;
                    line-height: 1.25;
                    margin-bottom: 5px;
                    white-space: nowrap;
                }
                .legend-row:last-child {
                    margin-bottom: 0;
                }
                .legend-color-chip {
                    width: 12px;
                    height: 12px;
                    border-radius: 3px;
                    background: transparent;
                    border: 2px solid var(--legend-color, rgba(0, 0, 0, 0.42));
                    flex-shrink: 0;
                }
                .legend-node-dot {
                    width: 11px;
                    height: 11px;
                    border-radius: 999px;
                    border: 1px solid rgba(0, 0, 0, 0.22);
                    flex-shrink: 0;
                }
                .legend-node-rect {
                    width: 14px;
                    height: 10px;
                    border-radius: 2px;
                    border: 1px solid rgba(0, 0, 0, 0.22);
                    flex-shrink: 0;
                }
                .legend-link-line {
                    width: 14px;
                    height: 2px;
                    border-radius: 2px;
                    background: var(--gutter-text, #6e7781);
                    flex-shrink: 0;
                }
                .legend-count {
                    margin-left: auto;
                    color: var(--gutter-text, #6e7781);
                    font-size: 11px;
                }
                .legend-placeholder {
                    font-size: 12px;
                    color: var(--gutter-text, #6e7781);
                }
            </style>
            <div class="container" id="main-container">
                <svg id="chart-container"></svg>
                <div class="legend-stack" id="legend-stack">
                    <div class="overlay-panel legend-global" id="legend-global"></div>
                    <div class="overlay-panel legend-named" id="legend-named"></div>
                </div>
            </div>
        `;
        this.legendStackElement = this.shadowRoot.querySelector("#legend-stack");
        this.globalLegendElement = this.shadowRoot.querySelector("#legend-global");
        this.namedLegendElement = this.shadowRoot.querySelector("#legend-named");
        this.refreshOverlayPanels();
    }

    /**
     * Setup resize observer to handle container size changes.
     */
    observeResize() {
        this.resizeObserver = new ResizeObserver(() => {
            this.updateSize();
            if (this.svg) {
                this.svg.attr('width', this.width).attr('height', this.height);
                if (this.simulation) {
                    this.buildConnectedComponents();
                    this.simulation.force("center", d3.forceCenter(this.width / 2, this.height / 2));
                    if (this.componentTargets?.size > 1) {
                        this.simulation
                            .force("componentX", d3.forceX(node => {
                                const componentIndex = this.componentByNodeId.get(node?.id) ?? 0;
                                return (this.componentTargets.get(componentIndex) || { x: this.width / 2 }).x;
                            }).strength(0.06))
                            .force("componentY", d3.forceY(node => {
                                const componentIndex = this.componentByNodeId.get(node?.id) ?? 0;
                                return (this.componentTargets.get(componentIndex) || { y: this.height / 2 }).y;
                            }).strength(0.06));
                    }
                    this.simulation.alphaTarget(0.05).restart();
                    setTimeout(() => this.simulation.alphaTarget(0), 500);
                }
            }
        });
        this.resizeObserver.observe(this);
    }

    /**
     * Update component dimensions from bounding rect.
     */
    updateSize() {
        const rect = this.getBoundingClientRect();
        this.width = rect.width || 800;
        this.height = rect.height || 600;
    }

    updateLevelOfDetail(scale = 1) {
        const zoom = scale;
        this.currentZoom = zoom;
        const showNodeLabels = zoom >= this.nodeLabelZoomThreshold;
        const showEdgeLabels = zoom >= this.edgeLabelZoomThreshold && this.showEdgeLabels && !this.edgeLabelsAutoHidden;
        this.nodeLabelsVisible = showNodeLabels;
        this.edgeLabelsVisible = showEdgeLabels;

        if (this.svg) {
            this.svg.classed('node-labels-off', !showNodeLabels);
            this.svg.classed('edge-labels-off', !showEdgeLabels);
        }
        this.notifyEffectiveRenderProfile();
        this.scheduleLabelVisibilityUpdate();
    }

    shouldHideLabelsDuringInteraction() {
        if (!this.interactionHideLabels) return false;
        const nodeCount = this.graph?.nodes?.length ?? 0;
        const linkCount = this.graph?.links?.length ?? 0;
        // In JavaFX WebView, hiding labels during interaction avoids visual trails
        // on heavier graphs while preserving smooth motion on small graphs.
        return nodeCount >= this.interactionHideNodeThreshold
            || linkCount >= this.interactionHideLinkThreshold;
    }

    hideLabelsForInteraction() {
        this.labelsHiddenForInteraction = true;
        if (this.svg) {
            this.svg.classed('labels-hidden', true);
            this.svg.classed('interaction-active', true);
        }
    }

    onInteraction(shouldHideLabels = true) {
        this.isInteracting = true;
        if (this.tooltipShowTimerHandle) {
            clearTimeout(this.tooltipShowTimerHandle);
            this.tooltipShowTimerHandle = null;
        }
        if (this.tooltipMoveTimerHandle) {
            clearTimeout(this.tooltipMoveTimerHandle);
            this.tooltipMoveTimerHandle = null;
        }
        d3.select("#global-tooltip").style("opacity", 0);
        const wasHidden = this.labelsHiddenForInteraction;
        const shouldHide = shouldHideLabels && this.shouldHideLabelsDuringInteraction();
        this.clearHoverFocus(false);
        if (shouldHide) {
            this.hideLabelsForInteraction();
        } else {
            this.labelsHiddenForInteraction = false;
            if (this.svg) {
                this.svg.classed('labels-hidden', false);
                this.svg.classed('interaction-active', false);
            }
        }
        if (wasHidden !== this.labelsHiddenForInteraction) {
            this.notifyEffectiveRenderProfile();
        }
        if (this.interactionTimer) {
            clearTimeout(this.interactionTimer);
        }
        this.interactionTimer = setTimeout(() => {
            this.isInteracting = false;
            this.interactionTimer = null;
            const wasHiddenInInteraction = this.labelsHiddenForInteraction;
            this.labelsHiddenForInteraction = false;
            if (this.svg) {
                this.svg.classed('labels-hidden', false);
                this.svg.classed('interaction-active', false);
            }
            if (wasHiddenInInteraction) {
                this.notifyEffectiveRenderProfile();
            }
            this.updateLabelVisibility();
        }, this.interactionDebounceMs);
    }

    scheduleLabelVisibilityUpdate() {
        if (!this.labelCullEnabled) return;
        if (!this.nodeLabelSelection && !this.linkLabelSelection) return;
        if (this.labelsHiddenForInteraction) return;
        if (this.labelVisibilityRaf) return;
        this.labelVisibilityRaf = requestAnimationFrame(() => {
            this.labelVisibilityRaf = null;
            const now = performance.now();
            if (now - this.lastLabelVisibilityUpdate < this.labelVisibilityThrottleMs) {
                return;
            }
            this.lastLabelVisibilityUpdate = now;
            this.updateLabelVisibility();
        });
    }

    updateLabelVisibility() {
        if (!this.labelCullEnabled) return;
        if (!this.nodeLabelSelection && !this.linkLabelSelection) return;

        const showNodeLabels = this.nodeLabelsVisible && !!this.nodeLabelSelection;
        const showEdgeLabels = this.edgeLabelsVisible && !!this.linkLabelSelection;
        if (showEdgeLabels) {
            this.refreshEdgeLabelPositions(true);
        }
        if (!showNodeLabels && this.nodeLabelSelection) {
            this.nodeLabelSelection.style('visibility', 'hidden').style('opacity', 0);
        }
        if (!showEdgeLabels && this.linkLabelSelection) {
            this.linkLabelSelection.style('visibility', 'hidden').style('opacity', 0);
        }
        if (!showNodeLabels && !showEdgeLabels) {
            return;
        }

        const t = this.currentTransform || { k: 1, x: 0, y: 0 };
        const pad = 80 / t.k;
        const x0 = (-t.x) / t.k - pad;
        const y0 = (-t.y) / t.k - pad;
        const x1 = (this.width - t.x) / t.k + pad;
        const y1 = (this.height - t.y) / t.k + pad;

        const inView = (x, y) => x >= x0 && x <= x1 && y >= y0 && y <= y1;

        if (showNodeLabels) {
            this.nodeLabelSelection
                .style('visibility', d => {
                    if (!d || typeof d.x !== 'number' || typeof d.y !== 'number') {
                        return 'visible';
                    }
                    return inView(d.x, d.y) ? 'visible' : 'hidden';
                })
                .style('opacity', d => {
                    if (!d || typeof d.x !== 'number' || typeof d.y !== 'number') {
                        return 1;
                    }
                    return inView(d.x, d.y) ? 1 : 0;
                });
        }

        if (showEdgeLabels) {
            this.linkLabelSelection
                .style('visibility', d => {
                    const geometry = d?.geometry ?? this.buildLinkGeometry(d);
                    if (!geometry) {
                        return 'hidden';
                    }
                    return inView(geometry.labelX, geometry.labelY) ? 'visible' : 'hidden';
                })
                .style('opacity', d => {
                    const geometry = d?.geometry ?? this.buildLinkGeometry(d);
                    if (!geometry) {
                        return 0;
                    }
                    return inView(geometry.labelX, geometry.labelY) ? 1 : 0;
                });
        }
    }

    refreshEdgeLabelPositions(force = false) {
        if (!this.linkLabelSelection || !this.graph || !Array.isArray(this.graph.links)) {
            return;
        }
        if (!force && (!this.edgeLabelsVisible || this.labelsHiddenForInteraction)) {
            return;
        }

        this.linkLabelSelection
            .attr("x", d => {
                if (!d?.source || !d?.target) return null;
                const geometry = d.geometry ?? this.buildLinkGeometry(d);
                return geometry ? geometry.labelX : null;
            })
            .attr("y", d => {
                if (!d?.source || !d?.target) return null;
                const geometry = d.geometry ?? this.buildLinkGeometry(d);
                return geometry ? geometry.labelY : null;
            })
            .attr("text-anchor", d => {
                const geometry = d?.geometry ?? this.buildLinkGeometry(d);
                return geometry?.labelAnchor ?? "middle";
            });
    }

    escapeHtml(value) {
        const text = String(value ?? "");
        return text
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    shortenGraphName(graphId) {
        const label = this.normalizeGraphId(graphId);
        if (label === GRAPH_DEFAULTS.defaultGraphId) {
            return "default";
        }
        if (label.length <= 42) {
            return label;
        }
        return `${label.substring(0, 39)}...`;
    }

    formatLegendCount(value) {
        if (!Number.isFinite(value)) {
            return "0";
        }
        return Math.max(0, Math.floor(value)).toLocaleString();
    }

    createEmptyGraphSummary() {
        return {
            nodeCount: 0,
            linkCount: 0,
            namedGraphs: [],
            componentCounts: {
                resource: 0,
                literal: 0,
                blank: 0,
                predicateLink: 0
            }
        };
    }

    resolveGraphSummary(summary) {
        if (!summary || typeof summary !== "object") {
            return this.createEmptyGraphSummary();
        }
        const safeSummary = this.createEmptyGraphSummary();
        safeSummary.nodeCount = Number.isFinite(summary.nodeCount)
            ? Math.max(0, Math.floor(summary.nodeCount))
            : 0;
        safeSummary.linkCount = Number.isFinite(summary.linkCount)
            ? Math.max(0, Math.floor(summary.linkCount))
            : 0;
        safeSummary.namedGraphs = Array.isArray(summary.namedGraphs) ? summary.namedGraphs : [];
        const counts = summary.componentCounts ?? {};
        safeSummary.componentCounts.resource = Number.isFinite(counts.resource) ? Math.max(0, Math.floor(counts.resource)) : 0;
        safeSummary.componentCounts.literal = Number.isFinite(counts.literal) ? Math.max(0, Math.floor(counts.literal)) : 0;
        safeSummary.componentCounts.blank = Number.isFinite(counts.blank) ? Math.max(0, Math.floor(counts.blank)) : 0;
        safeSummary.componentCounts.predicateLink = Number.isFinite(counts.predicateLink)
            ? Math.max(0, Math.floor(counts.predicateLink))
            : safeSummary.linkCount;
        return safeSummary;
    }

    collectGraphSummary() {
        const summary = this.createEmptyGraphSummary();
        const graph = this.graph;
        if (!graph || !Array.isArray(graph.nodes) || !Array.isArray(graph.links)) {
            return summary;
        }

        summary.nodeCount = graph.nodes.length;
        summary.linkCount = graph.links.length;
        summary.componentCounts.predicateLink = summary.linkCount;

        const namedGraphStats = new Map();
        const upsertNamedGraphStat = graphId => {
            const gid = this.normalizeGraphId(graphId);
            if (gid === GRAPH_DEFAULTS.defaultGraphId) {
                return null;
            }
            if (!namedGraphStats.has(gid)) {
                namedGraphStats.set(gid, {
                    id: gid,
                    nodeCount: 0,
                    linkCount: 0
                });
            }
            return namedGraphStats.get(gid);
        };

        graph.nodes.forEach(node => {
            switch (node?.type) {
                case "Literal":
                    summary.componentCounts.literal += 1;
                    break;
                case "Blank":
                    summary.componentCounts.blank += 1;
                    break;
                default:
                    // "Resource" and internal sub-types (e.g., "Class") share
                    // the same visual component entry.
                    summary.componentCounts.resource += 1;
                    break;
            }

            const graphIds = node?.graphs instanceof Set
                ? [...node.graphs]
                : [node?.graph];
            graphIds.forEach(graphId => {
                const stat = upsertNamedGraphStat(graphId);
                if (stat) {
                    stat.nodeCount += 1;
                }
            });
        });

        graph.links.forEach(link => {
            const stat = upsertNamedGraphStat(link?.graph);
            if (stat) {
                stat.linkCount += 1;
            }
        });

        summary.namedGraphs = [...namedGraphStats.values()]
            .map(stat => ({
                ...stat,
                color: this.getGraphColor(stat.id)
            }))
            .sort((left, right) => right.linkCount - left.linkCount || left.id.localeCompare(right.id));
        return summary;
    }

    notifyGraphStats() {
        const summary = this.resolveGraphSummary(this.graphSummary);
        if (!window.bridge || typeof window.bridge.onGraphStatsUpdated !== "function") {
            return;
        }
        try {
            const namedGraphPayload = Array.isArray(summary.namedGraphs)
                ? summary.namedGraphs.map(namedGraph => ({
                    id: String(namedGraph?.id ?? ""),
                    linkCount: Number.isFinite(namedGraph?.linkCount)
                        ? Math.max(0, Math.floor(namedGraph.linkCount))
                        : 0
                }))
                : [];
            window.bridge.onGraphStatsUpdated(
                String(summary.linkCount),
                String(summary.namedGraphs.length),
                namedGraphPayload
            );
        } catch (error) {
            try {
                window.bridge.onGraphStatsUpdated(String(summary.linkCount), String(summary.namedGraphs.length));
            } catch (legacyError) {
                // Ignore bridge callback failures to keep rendering resilient.
            }
        }
    }

    notifyRenderProfile(profile = {}) {
        if (!window.bridge || typeof window.bridge.onGraphRenderProfileUpdated !== "function") {
            return;
        }
        const mode = String(profile.mode || "normal").toLowerCase();
        const summary = String(profile.summary || "");
        const details = Array.isArray(profile.details)
            ? profile.details.map(line => String(line || "").trim()).filter(line => line.length > 0)
            : [];
        try {
            window.bridge.onGraphRenderProfileUpdated(mode, summary, details);
        } catch (error) {
            try {
                window.bridge.onGraphRenderProfileUpdated(mode, summary);
            } catch (legacyError) {
                // Ignore bridge callback failures to keep rendering resilient.
            }
        }
    }

    notifyEffectiveRenderProfile() {
        const base = this.baseRenderProfile || {};
        const baseDetails = Array.isArray(base.details)
            ? base.details.map(line => String(line || "").trim()).filter(line => line.length > 0)
            : [];
        const effectiveDetails = [...baseDetails];

        const nodeCount = Array.isArray(this.graph?.nodes) ? this.graph.nodes.length : 0;
        const edgeCount = Array.isArray(this.graph?.links) ? this.graph.links.length : 0;
        const hasNodes = nodeCount > 0;
        const hasEdges = edgeCount > 0;
        const interactionHidden = this.labelsHiddenForInteraction && (hasNodes || hasEdges);

        const effectiveNodeLabelsVisible = !hasNodes
            || (!interactionHidden && this.nodeLabelsVisible && !!this.nodeLabelSelection);
        const effectiveEdgeLabelsVisible = !hasEdges
            || (!interactionHidden && this.edgeLabelsVisible && !!this.linkLabelSelection);

        if (interactionHidden) {
            effectiveDetails.push("Labels temporarily hidden while interacting with the graph.");
        }
        if (hasNodes && !effectiveNodeLabelsVisible && !interactionHidden) {
            if (!this.nodeLabelSelection) {
                effectiveDetails.push("Node labels disabled for current graph size.");
            } else {
                effectiveDetails.push("Node labels hidden at current zoom level.");
            }
        }
        if (hasEdges && !effectiveEdgeLabelsVisible && !interactionHidden) {
            if (!this.showEdgeLabels) {
                effectiveDetails.push("Edge labels disabled.");
            } else if (!this.linkLabelSelection) {
                effectiveDetails.push("Edge labels disabled for current graph size.");
            } else if (this.edgeLabelsAutoHidden) {
                effectiveDetails.push("Edge labels hidden by default for dense graph.");
            } else {
                effectiveDetails.push("Edge labels hidden at current zoom level.");
            }
        }

        const uniqueDetails = [...new Set(effectiveDetails)];
        const isDegraded = String(base.mode || "normal").toLowerCase() === "degraded"
            || !effectiveNodeLabelsVisible
            || !effectiveEdgeLabelsVisible;

        const effectiveProfile = {
            mode: isDegraded ? "degraded" : "normal",
            summary: isDegraded
                ? (String(base.summary || "").trim() || "Adaptive rendering enabled")
                : "Standard rendering",
            details: uniqueDetails
        };

        const key = JSON.stringify(effectiveProfile);
        if (key === this.lastEffectiveRenderProfileKey) {
            return;
        }
        this.lastEffectiveRenderProfileKey = key;
        this.notifyRenderProfile(effectiveProfile);
    }

    renderGlobalLegend(componentCounts) {
        return `
            <div class="legend-title">Components</div>
            <div class="legend-row">
                <span class="legend-node-dot" style="background:${GRAPH_NODE_FILL.Resource}"></span>
                <span>Resource</span>
                <span class="legend-count">${this.formatLegendCount(componentCounts.resource)}</span>
            </div>
            <div class="legend-row">
                <span class="legend-node-rect" style="background:${GRAPH_NODE_FILL.Literal}"></span>
                <span>Literal</span>
                <span class="legend-count">${this.formatLegendCount(componentCounts.literal)}</span>
            </div>
            <div class="legend-row">
                <span class="legend-node-dot" style="background:${GRAPH_NODE_FILL.Blank}"></span>
                <span>Blank Node</span>
                <span class="legend-count">${this.formatLegendCount(componentCounts.blank)}</span>
            </div>
            <div class="legend-row">
                <span class="legend-link-line"></span>
                <span>Predicate Link</span>
                <span class="legend-count">${this.formatLegendCount(componentCounts.predicateLink)}</span>
            </div>
        `;
    }

    renderNamedGraphLegend(namedGraphs) {
        if (!Array.isArray(namedGraphs) || namedGraphs.length === 0) {
            return "";
        }
        const rows = namedGraphs
            .map(namedGraph => `
                <div class="legend-row" title="${this.escapeHtml(namedGraph.id)}">
                    <span class="legend-color-chip" style="--legend-color:${namedGraph.color};"></span>
                    <span>${this.escapeHtml(this.shortenGraphName(namedGraph.id))}</span>
                    <span class="legend-count">${this.formatLegendCount(namedGraph.linkCount)}</span>
                </div>
            `)
            .join("");
        return `
            <div class="legend-title">Named Graphs</div>
            ${rows}
        `;
    }

    refreshOverlayPanels() {
        const summary = this.resolveGraphSummary(this.graphSummary);
        const componentCounts = summary.componentCounts;

        if (this.globalLegendElement) {
            this.globalLegendElement.innerHTML = this.renderGlobalLegend(componentCounts);
        }

        if (this.namedLegendElement) {
            const namedLegendHtml = this.renderNamedGraphLegend(summary.namedGraphs);
            if (!namedLegendHtml) {
                this.namedLegendElement.innerHTML = "";
                this.namedLegendElement.style.display = "none";
            } else {
                this.namedLegendElement.innerHTML = namedLegendHtml;
                this.namedLegendElement.style.display = "block";
            }
        }
    }

    /* -------------------------------------------------------------
     * GRAPH PROCESSING
     * ------------------------------------------------------------- */

    /**
     * Parse JSON-LD into graph structure (nodes and links).
     * @returns {Object} Graph object with nodes and links arrays.
     */
    createGraph() {
        const graph = { nodes: [], links: [] };
        const nodeById = new Map();
        const linkDedup = new Set();
        const subjectProcessingState = new Map();
        const activeSubjectStack = new Set();
        const RDF_TYPE_PREDICATE = GRAPH_DEFAULTS.rdfTypePredicate;

        const normalizeArray = value => (Array.isArray(value) ? value : [value]);
        const isObject = value => value !== null && typeof value === 'object';
        const resolveGraphId = graphId => this.normalizeGraphId(graphId);
        const isInferenceGraphId = graphId => {
            const gid = resolveGraphId(graphId);
            return gid.startsWith('urn:corese:inference:')
                || gid === 'http://ns.inria.fr/corese/rule'
                || gid === 'http://ns.inria.fr/corese/constraint';
        };
        const isDefaultGraphId = graphId => resolveGraphId(graphId) === GRAPH_DEFAULTS.defaultGraphId;
        const SUBJECT_WEIGHT = 100;
        const MENTION_WEIGHT = 10;

        const ensureGraphScore = (node, graphId) => {
            if (!(node.graphScores instanceof Map)) {
                node.graphScores = new Map();
            }
            const gid = resolveGraphId(graphId);
            if (!node.graphScores.has(gid)) {
                node.graphScores.set(gid, {
                    subjectMentions: 0,
                    objectMentions: 0,
                    totalWeight: 0
                });
            }
            return node.graphScores.get(gid);
        };

        const registerNodeGraphUsage = (node, graphId, isSubject) => {
            if (!node.graphs) {
                node.graphs = new Set();
            }
            const gid = resolveGraphId(graphId);
            node.graphs.add(gid);
            const score = ensureGraphScore(node, gid);
            if (isSubject) {
                score.subjectMentions += 1;
                score.totalWeight += SUBJECT_WEIGHT;
                return;
            }
            score.objectMentions += 1;
            score.totalWeight += MENTION_WEIGHT;
        };

        const buildPriority = (graphId, score = null) => ({
            graphId,
            nonInference: isInferenceGraphId(graphId) ? 0 : 1,
            hasSubjectMentions: (score?.subjectMentions ?? 0) > 0 ? 1 : 0,
            subjectMentions: score?.subjectMentions ?? 0,
            totalWeight: score?.totalWeight ?? 0,
            named: isDefaultGraphId(graphId) ? 0 : 1
        });

        const comparePriority = (candidate, current) => {
            // Keep existing resources anchored to asserted graphs: any
            // non-inference graph must win over inference graphs.
            if (candidate.nonInference !== current.nonInference) {
                return candidate.nonInference > current.nonInference;
            }

            if (candidate.hasSubjectMentions !== current.hasSubjectMentions) {
                return candidate.hasSubjectMentions > current.hasSubjectMentions;
            }

            if (candidate.subjectMentions !== current.subjectMentions) {
                return candidate.subjectMentions > current.subjectMentions;
            }

            if (candidate.totalWeight !== current.totalWeight) {
                return candidate.totalWeight > current.totalWeight;
            }

            // Keep named-over-default only as a late deterministic tie-breaker.
            if (candidate.named !== current.named) {
                return candidate.named > current.named;
            }

            return candidate.graphId.localeCompare(current.graphId) < 0;
        };

        const resolvePrimaryGraph = node => {
            const candidateIds = node?.graphs instanceof Set
                ? [...node.graphs]
                : [resolveGraphId(node?.graph)];
            if (candidateIds.length === 0) {
                return GRAPH_DEFAULTS.defaultGraphId;
            }
            // If a node was explicitly defined in a non-inference graph, keep
            // that graph as its primary color source.
            const definitionGraphId = resolveGraphId(node?.definitionGraph);
            if (candidateIds.includes(definitionGraphId) && !isInferenceGraphId(definitionGraphId)) {
                return definitionGraphId;
            }
            candidateIds.sort((left, right) => left.localeCompare(right));
            let bestId = candidateIds[0];
            const scoreMap = node?.graphScores instanceof Map ? node.graphScores : new Map();
            for (let index = 1; index < candidateIds.length; index += 1) {
                const candidateId = candidateIds[index];
                const candidatePriority = buildPriority(candidateId, scoreMap.get(candidateId));
                const bestPriority = buildPriority(bestId, scoreMap.get(bestId));
                if (comparePriority(candidatePriority, bestPriority)) {
                    bestId = candidateId;
                }
            }
            return bestId;
        };

        const upsertNode = (id, type, graphId, meta = {}, isSubject = false) => {
            if (!id) return null;
            const resolvedGraph = resolveGraphId(graphId);
            const existing = nodeById.get(id);
            if (existing) {
                if (type === 'Blank' || type === 'Class') {
                    existing.type = type;
                }

                registerNodeGraphUsage(existing, resolvedGraph, isSubject);
                if (isSubject) {
                    existing.isDefinedAsSubject = true;
                    if (!existing.definitionGraph
                        || (isInferenceGraphId(existing.definitionGraph) && !isInferenceGraphId(resolvedGraph))
                        || (isDefaultGraphId(existing.definitionGraph)
                            && !isDefaultGraphId(resolvedGraph)
                            && !isInferenceGraphId(resolvedGraph))) {
                        existing.definitionGraph = resolvedGraph;
                    }
                }

                Object.assign(existing, meta);
                return existing;
            }

            const node = {
                id,
                name: id,
                type,
                graph: resolvedGraph,
                graphs: new Set(),
                graphScores: new Map(),
                isDefinedAsSubject: isSubject,
                definitionGraph: isSubject ? resolvedGraph : null,
                ...meta
            };
            registerNodeGraphUsage(node, resolvedGraph, isSubject);
            graph.nodes.push(node);
            nodeById.set(id, node);
            return node;
        };

        const resolveSubjectType = (item, subjectId) => {
            const types = normalizeArray(item['@type'] ?? []);
            const hasClassType = types.some(typeValue => {
                if (typeValue == null) {
                    return false;
                }
                const typeLabel = (isObject(typeValue) && typeValue['@id'])
                    ? typeValue['@id']
                    : String(typeValue);
                return typeLabel.includes('Class');
            });
            if (hasClassType) {
                return 'Class';
            }
            if (subjectId.startsWith('_:')) {
                return 'Blank';
            }
            return 'Resource';
        };

        const isSubstantialDefinition = item => {
            const hasProperties = Object.keys(item).some(k => !k.startsWith('@'));
            return hasProperties || !!item['@type'];
        };

        const addLink = (subj, pred, objId, objType, meta, currentGraph) => {
            if (objId === undefined || objId === null || objId === 'undefined') return;
            const resolvedGraph = resolveGraphId(currentGraph);
            const dedupKey = `${resolvedGraph}\u0000${subj}\u0000${pred}\u0000${objId}`;
            if (linkDedup.has(dedupKey)) {
                return;
            }
            linkDedup.add(dedupKey);
            upsertNode(objId, objType, currentGraph, meta, false);
            graph.links.push({
                source: subj,
                target: objId,
                name: pred,
                graph: currentGraph
            });
        };

        const processObject = (obj, currentGraph) => {
            if (obj && typeof obj === 'object') {
                if (obj['@id']) {
                    const objId = obj['@id'];
                    const objType = objId.startsWith('_:') ? 'Blank' : 'Resource';
                    processItem(obj, currentGraph);
                    return { objId, objType, meta: {} };
                }
                if (obj['@value'] !== undefined) {
                    const literalValue = String(obj['@value']);
                    const datatype = typeof obj['@type'] === "string" ? obj['@type'] : "";
                    const language = typeof obj['@language'] === "string" ? obj['@language'] : "";
                    const objId = this.buildLiteralNodeId(literalValue, datatype, language);
                    const meta = { name: literalValue };
                    if (datatype) meta.datatype = datatype;
                    if (language) meta.language = language;
                    return { objId, objType: 'Literal', meta };
                }
            }
            const literalValue = String(obj);
            return {
                objId: this.buildLiteralNodeId(literalValue),
                objType: 'Literal',
                meta: { name: literalValue }
            };
        };

        const processGraphContainer = (item, currentGraph) => {
            if (!item['@graph']) return false;
            const newGraph = item['@id'] || currentGraph;
            normalizeArray(item['@graph']).forEach(child => processItem(child, newGraph));
            return true;
        };

        const processPredicates = (item, subj, currentGraph) => {
            Object.keys(item)
                .filter(pred => !pred.startsWith('@'))
                .forEach(pred => {
                    normalizeArray(item[pred]).forEach(obj => {
                        const { objId, objType, meta } = processObject(obj, currentGraph);
                        addLink(subj, pred, objId, objType, meta, currentGraph);
                    });
                });
        };

        const processTypePredicates = (item, subj, currentGraph) => {
            normalizeArray(item['@type'] ?? []).forEach(rawType => {
                if (rawType === undefined || rawType === null) {
                    return;
                }
                const typeId = (isObject(rawType) && rawType['@id'])
                    ? rawType['@id']
                    : String(rawType);
                if (!typeId || typeId.startsWith('@')) {
                    return;
                }
                const typeNodeKind = typeId.startsWith('_:') ? 'Blank' : 'Class';
                addLink(subj, RDF_TYPE_PREDICATE, typeId, typeNodeKind, {}, currentGraph);
            });
        };

        const processItem = (item, currentGraph) => {
            if (!isObject(item)) return;
            if (processGraphContainer(item, currentGraph)) return;

            const subj = item['@id'];
            if (!subj) return;

            const resolvedGraph = resolveGraphId(currentGraph);
            const subjectKey = `${resolvedGraph}\u0000${subj}`;
            const subjType = resolveSubjectType(item, subj);
            const isSubstantial = isSubstantialDefinition(item);
            const previousState = subjectProcessingState.get(subjectKey);

            if (previousState?.fullProcessed && isSubstantial) {
                return;
            }
            if (previousState?.stubProcessed && !isSubstantial) {
                return;
            }

            const alreadyOnStack = activeSubjectStack.has(subjectKey);
            if (alreadyOnStack && isSubstantial) {
                upsertNode(subj, subjType, currentGraph, {}, false);
                return;
            }

            subjectProcessingState.set(subjectKey, {
                stubProcessed: true,
                fullProcessed: isSubstantial || Boolean(previousState?.fullProcessed)
            });

            upsertNode(subj, subjType, currentGraph, {}, isSubstantial);
            if (!isSubstantial) {
                return;
            }

            activeSubjectStack.add(subjectKey);
            try {
                processTypePredicates(item, subj, currentGraph);
                processPredicates(item, subj, currentGraph);
            } finally {
                activeSubjectStack.delete(subjectKey);
            }
        };

        const root = this.jsonLDOntology;
        normalizeArray(root).forEach(item => processItem(item, GRAPH_DEFAULTS.defaultGraphId));
        graph.nodes.forEach(node => {
            const primaryGraph = resolvePrimaryGraph(node);
            node.graph = primaryGraph;
            if (node.isDefinedAsSubject && !node.definitionGraph) {
                node.definitionGraph = primaryGraph;
            }
            delete node.graphScores;
        });
        return graph;
    }

    captureNodeStateById() {
        const previousNodes = this.graph?.nodes;
        if (!Array.isArray(previousNodes) || previousNodes.length === 0) {
            return new Map();
        }
        const states = new Map();
        previousNodes.forEach(node => {
            if (!node || !node.id) return;
            if (!Number.isFinite(node.x) || !Number.isFinite(node.y)) return;
            states.set(node.id, {
                x: node.x,
                y: node.y,
                vx: Number.isFinite(node.vx) ? node.vx : 0,
                vy: Number.isFinite(node.vy) ? node.vy : 0,
                fx: Number.isFinite(node.fx) ? node.fx : null,
                fy: Number.isFinite(node.fy) ? node.fy : null
            });
        });
        return states;
    }

    computeLayoutAnchor(previousNodeStateById) {
        if (!previousNodeStateById || previousNodeStateById.size === 0) {
            return { x: this.width / 2, y: this.height / 2 };
        }
        let sumX = 0;
        let sumY = 0;
        let count = 0;
        previousNodeStateById.forEach(state => {
            sumX += state.x;
            sumY += state.y;
            count += 1;
        });
        if (count === 0) {
            return { x: this.width / 2, y: this.height / 2 };
        }
        return { x: sumX / count, y: sumY / count };
    }

    buildConnectedComponents() {
        const graph = this.graph;
        if (!graph || !Array.isArray(graph.nodes) || graph.nodes.length === 0) {
            this.componentByNodeId = new Map();
            this.componentTargets = new Map([[0, { x: this.width / 2, y: this.height / 2 }]]);
            return [];
        }

        const adjacency = new Map();
        graph.nodes.forEach(node => {
            if (!node?.id) return;
            adjacency.set(node.id, new Set());
        });
        graph.links.forEach(link => {
            const sourceId = this.getLinkEndpointId(link?.source);
            const targetId = this.getLinkEndpointId(link?.target);
            if (!sourceId || !targetId || sourceId === targetId) {
                return;
            }
            if (!adjacency.has(sourceId) || !adjacency.has(targetId)) {
                return;
            }
            adjacency.get(sourceId).add(targetId);
            adjacency.get(targetId).add(sourceId);
        });

        const visited = new Set();
        const components = [];
        adjacency.forEach((_, startId) => {
            if (visited.has(startId)) {
                return;
            }
            const queue = [startId];
            let queueHead = 0;
            const component = [];
            visited.add(startId);
            while (queueHead < queue.length) {
                const current = queue[queueHead];
                queueHead += 1;
                component.push(current);
                adjacency.get(current).forEach(nextId => {
                    if (visited.has(nextId)) {
                        return;
                    }
                    visited.add(nextId);
                    queue.push(nextId);
                });
            }
            component.sort((left, right) => left.localeCompare(right));
            components.push(component);
        });

        const sortedComponents = components
            .map(component => [...component])
            .sort((left, right) => right.length - left.length || left[0].localeCompare(right[0]));
        this.componentByNodeId = new Map();
        sortedComponents.forEach((component, componentIndex) => {
            component.forEach(nodeId => {
                this.componentByNodeId.set(nodeId, componentIndex);
            });
        });
        this.componentTargets = this.computeComponentTargets(sortedComponents.length);
        return sortedComponents;
    }

    computeComponentTargets(componentCount) {
        const targets = new Map();
        if (componentCount <= 0) {
            targets.set(0, { x: this.width / 2, y: this.height / 2 });
            return targets;
        }
        if (componentCount === 1) {
            targets.set(0, { x: this.width / 2, y: this.height / 2 });
            return targets;
        }

        const columns = Math.ceil(Math.sqrt(componentCount));
        const rows = Math.ceil(componentCount / columns);
        const horizontalSpacing = Math.max(190, Math.min(420, this.width / Math.max(2, columns)));
        const verticalSpacing = Math.max(170, Math.min(360, this.height / Math.max(2, rows)));
        const gridWidth = horizontalSpacing * Math.max(0, columns - 1);
        const gridHeight = verticalSpacing * Math.max(0, rows - 1);
        const originX = this.width / 2 - gridWidth / 2;
        const originY = this.height / 2 - gridHeight / 2;

        for (let index = 0; index < componentCount; index += 1) {
            const row = Math.floor(index / columns);
            const col = index % columns;
            targets.set(index, {
                x: originX + col * horizontalSpacing,
                y: originY + row * verticalSpacing
            });
        }
        return targets;
    }

    positionNewNode(node, fallbackAnchor) {
        const nodeId = node?.id;
        const componentIndex = this.componentByNodeId.get(nodeId) ?? 0;
        const componentTarget = this.componentTargets.get(componentIndex) || fallbackAnchor;
        const hash = this.stableHash(nodeId);
        const angle = (hash % 360) * (Math.PI / 180);
        const ring = (hash >>> 5) % 5;
        const radius = 26 + ring * 18;

        node.x = componentTarget.x + Math.cos(angle) * radius;
        node.y = componentTarget.y + Math.sin(angle) * radius;
        node.vx = 0;
        node.vy = 0;
    }

    shouldApplyAutoFit(hadExistingLayout, addedRatio) {
        if (!hadExistingLayout) {
            return true;
        }
        return addedRatio > 0.82;
    }

    clearAutoFitTimers() {
        this.pendingAutoFit = false;
        if (this.pendingAutoFitTimer) {
            clearTimeout(this.pendingAutoFitTimer);
            this.pendingAutoFitTimer = null;
        }
        if (this.pendingRecenterTimer) {
            clearTimeout(this.pendingRecenterTimer);
            this.pendingRecenterTimer = null;
        }
    }

    scheduleAutoFit(delayMs = 120, overviewAlreadyApplied = false) {
        this.clearAutoFitTimers();
        this.pendingAutoFit = true;
        const startupDelay = Math.max(0, Number(delayMs) || 0);
        this.pendingAutoFitTimer = setTimeout(() => {
            this.pendingAutoFitTimer = null;
            if (!this.graph || !Array.isArray(this.graph.nodes) || this.graph.nodes.length === 0) {
                this.pendingAutoFit = false;
                return;
            }
            if (overviewAlreadyApplied) {
                this.pendingAutoFit = false;
                this.fitToGraph(120, true);
                return;
            }

            // Phase 1: very distant overview to keep heavy labels hidden while
            // the simulation stabilizes.
            this.fitToGraph(this.AUTO_OVERVIEW_PADDING, false, this.AUTO_OVERVIEW_MAX_SCALE);

            // Phase 2: delayed center action, same visual effect as pressing
            // the center button once the layout is more stable.
            this.pendingRecenterTimer = setTimeout(() => {
                this.pendingRecenterTimer = null;
                this.pendingAutoFit = false;
                this.fitToGraph(120, true);
            }, this.AUTO_RECENTER_DELAY_MS);
        }, startupDelay);
    }

    resolveFitTransform(padding = 90, maxScale = 2.4) {
        if (!this.graph || !Array.isArray(this.graph.nodes)) {
            return null;
        }
        const positionedNodes = this.graph.nodes.filter(node =>
            Number.isFinite(node?.x) && Number.isFinite(node?.y));
        if (positionedNodes.length === 0) {
            return null;
        }

        const minX = d3.min(positionedNodes, node => node.x);
        const maxX = d3.max(positionedNodes, node => node.x);
        const minY = d3.min(positionedNodes, node => node.y);
        const maxY = d3.max(positionedNodes, node => node.y);
        const boundsWidth = Math.max(24, (maxX - minX) || 0);
        const boundsHeight = Math.max(24, (maxY - minY) || 0);
        const usableWidth = Math.max(120, this.width - padding);
        const usableHeight = Math.max(120, this.height - padding);
        const resolvedMaxScale = Number.isFinite(maxScale) ? Math.max(0.05, maxScale) : 2.4;
        const scale = Math.max(0.05,
            Math.min(resolvedMaxScale, Math.min(usableWidth / boundsWidth, usableHeight / boundsHeight)));
        const centerX = (minX + maxX) / 2;
        const centerY = (minY + maxY) / 2;
        const translateX = this.width / 2 - centerX * scale;
        const translateY = this.height / 2 - centerY * scale;
        return d3.zoomIdentity.translate(translateX, translateY).scale(scale);
    }

    fitToGraph(padding = 90, animate = false, maxScale = 2.4) {
        if (!this.svg || !this.zoomBehavior || !this.graph || !Array.isArray(this.graph.nodes)) {
            return;
        }
        const targetTransform = this.resolveFitTransform(padding, maxScale);
        if (!targetTransform) {
            return;
        }
        if (animate) {
            this.svg.transition().duration(450).call(this.zoomBehavior.transform, targetTransform);
            return;
        }
        this.svg.call(this.zoomBehavior.transform, targetTransform);
    }

    resolvePreservedTransform() {
        const t = this.currentTransform;
        if (!t
            || !Number.isFinite(t.k)
            || !Number.isFinite(t.x)
            || !Number.isFinite(t.y)
            || t.k <= 0) {
            return d3.zoomIdentity;
        }
        return d3.zoomIdentity.scale(t.k).translate(t.x / t.k, t.y / t.k);
    }

    /* -------------------------------------------------------------
     * MAIN DRAWING
     * ------------------------------------------------------------- */

    /**
     * Main drawing entry point.
     * Parses JSON-LD, initializes simulation, and renders SVG elements.
     */
    drawChart() {
        if (!this.width || !this.height) this.updateSize();
        const previousNodeStateById = this.captureNodeStateById();
        const hadExistingLayout = previousNodeStateById.size > 0;
        const hadPendingAutoFit = this.pendingAutoFit || Boolean(this.pendingAutoFitTimer)
            || Boolean(this.pendingRecenterTimer);
        let initialTransform = this.resolvePreservedTransform();
        if (this.simulation) this.simulation.stop();
        if (this.interactionTimer) {
            clearTimeout(this.interactionTimer);
            this.interactionTimer = null;
        }
        if (this.labelVisibilityRaf) {
            cancelAnimationFrame(this.labelVisibilityRaf);
            this.labelVisibilityRaf = null;
        }
        this.clearAutoFitTimers();

        const chartSvg = this.shadowRoot.querySelector("#chart-container");
        if (!chartSvg) return;

        this.svg = d3.select(chartSvg);

        if (!this.jsonld) {
            this.svg.selectAll("*").remove();
            this.isInteracting = false;
            this.labelsHiddenForInteraction = false;
            this.lastLabelVisibilityUpdate = 0;
            this.lastLabelUpdate = 0;
            this.graph = { nodes: [], links: [] };
            this.simulation = null;
            this.linkSelection = null;
            this.nodeSelection = null;
            this.linkLabelSelection = null;
            this.nodeLabelSelection = null;
            this.focusLabelLayer = null;
            this.zoomLayer = null;
            this.currentTransform = d3.zoomIdentity;
            this.graphContextPrefixes = new Map();
            this.graphSummary = this.createEmptyGraphSummary();
            this.hoveredNodeId = null;
            this.linkIndexesByNodeId = new Map();
            this.refreshOverlayPanels();
            this.notifyGraphStats();
            return;
        }

        let parsedGraph = null;
        try {
            this.jsonLDOntology = JSON.parse(this.jsonld);
            this.rebuildGraphContextPrefixes();
            parsedGraph = this.createGraph();
        } catch (e) {
            console.error("Graph drawing error:", e);
            return;
        }

        this.svg.selectAll("*").remove();
        this.isInteracting = false;
        this.labelsHiddenForInteraction = false;
        this.lastLabelVisibilityUpdate = 0;
        this.lastLabelUpdate = 0;
        this.hoveredNodeId = null;

        try {
            this.graph = parsedGraph || { nodes: [], links: [] };
            this.linkIndexesByNodeId = this.buildLinkIndexByNodeId(this.graph.links);
            this.graphSummary = this.collectGraphSummary();
            this.refreshOverlayPanels();
            this.notifyGraphStats();

            this.buildConnectedComponents();
            if ((this.graph?.links?.length || 0) <= this.PARALLEL_LAYOUT_MAX_LINK_THRESHOLD) {
                this.assignParallelLinkOffsets(this.graph.links);
            } else {
                this.resetLinkOffsets(this.graph.links);
            }
            const anchor = this.computeLayoutAnchor(previousNodeStateById);
            let reusedNodeCount = 0;

            this.graph.nodes.forEach(node => {
                const previous = previousNodeStateById.get(node.id);
                if (previous) {
                    reusedNodeCount += 1;
                    node.x = previous.x;
                    node.y = previous.y;
                    node.vx = previous.vx;
                    node.vy = previous.vy;
                    node.fx = previous.fx;
                    node.fy = previous.fy;
                    return;
                }
                this.positionNewNode(node, anchor);
            });

            const addedNodeCount = this.graph.nodes.length - reusedNodeCount;
            const addedRatio = this.graph.nodes.length === 0
                ? 0
                : addedNodeCount / this.graph.nodes.length;
            const initialAlpha = !hadExistingLayout
                ? 1
                : (addedRatio <= 0.12 ? 0.18 : (addedRatio <= 0.35 ? 0.30 : 0.55));

            const shouldAutoFit = hadPendingAutoFit || this.shouldApplyAutoFit(hadExistingLayout, addedRatio);
            if (shouldAutoFit) {
                const overviewTransform = this.resolveFitTransform(this.AUTO_OVERVIEW_PADDING,
                    this.AUTO_OVERVIEW_MAX_SCALE);
                if (overviewTransform) {
                    initialTransform = overviewTransform;
                }
            }

            this.renderGraph(initialTransform, initialAlpha);
            if (shouldAutoFit) {
                this.scheduleAutoFit(this.AUTO_RECENTER_DELAY_MS, true);
            }
        } catch (e) {
            console.error("Graph drawing error:", e);
        }
    }

    /**
     * Configures D3 forces, zoom, and appends visual elements.
     */
    renderGraph(initialTransform = d3.zoomIdentity, initialAlpha = 1) {
        const nodeCount = this.graph.nodes.length;
        const linkCount = this.graph.links.length;
        const isLargeGraph = nodeCount >= this.LARGE_GRAPH_NODE_THRESHOLD || linkCount >= this.LARGE_GRAPH_LINK_THRESHOLD;
        const isVeryLargeGraph = nodeCount >= this.AUTO_HIDE_NODE_LABEL_THRESHOLD;
        const isHugeGraph = nodeCount >= this.HUGE_GRAPH_NODE_THRESHOLD || linkCount >= this.HUGE_GRAPH_LINK_THRESHOLD;
        const shouldCreateNodeLabels = nodeCount < this.DISABLE_LABEL_CREATION_NODE_THRESHOLD
            && linkCount < this.DISABLE_LABEL_CREATION_LINK_THRESHOLD;
        const shouldCreateEdgeLabels = linkCount < this.DISABLE_LABEL_CREATION_LINK_THRESHOLD && !isHugeGraph;
        const shouldEnableTooltips = nodeCount <= this.DISABLE_TOOLTIP_NODE_THRESHOLD
            && linkCount <= this.DISABLE_TOOLTIP_LINK_THRESHOLD;
        const isParallelLayoutSimplified = linkCount > this.PARALLEL_LAYOUT_MAX_LINK_THRESHOLD;
        this.simplifyLinkGeometry = isHugeGraph || linkCount >= this.SIMPLIFY_LINK_GEOMETRY_LINK_THRESHOLD;
        this.edgeLabelsAutoHidden = !shouldCreateEdgeLabels || linkCount >= this.AUTO_HIDE_EDGE_LABEL_THRESHOLD;
        this.nodeLabelZoomThreshold = shouldCreateNodeLabels
            ? (isVeryLargeGraph ? 0.62 : (isLargeGraph ? 0.38 : 0.2))
            : 2.0;
        this.edgeLabelZoomThreshold = shouldCreateEdgeLabels
            ? (this.edgeLabelsAutoHidden ? 0.92 : (isLargeGraph ? 0.5 : 0.2))
            : 2.0;
        if (isVeryLargeGraph) {
            this.LABEL_TICK_THROTTLE = this.LABEL_TICK_THROTTLE_VERY_LARGE;
            this.labelVisibilityThrottleMs = this.LABEL_VISIBILITY_THROTTLE_VERY_LARGE;
        } else if (isLargeGraph) {
            this.LABEL_TICK_THROTTLE = this.LABEL_TICK_THROTTLE_LARGE;
            this.labelVisibilityThrottleMs = this.LABEL_VISIBILITY_THROTTLE_LARGE;
        } else {
            this.LABEL_TICK_THROTTLE = this.LABEL_TICK_THROTTLE_SMALL;
            this.labelVisibilityThrottleMs = this.LABEL_VISIBILITY_THROTTLE_SMALL;
        }
        const renderProfileDetails = [];
        if (!shouldCreateNodeLabels) {
            renderProfileDetails.push("Node labels hidden to keep rendering responsive.");
        }
        if (!shouldCreateEdgeLabels) {
            renderProfileDetails.push("Edge labels hidden to reduce draw cost.");
        }
        if (this.simplifyLinkGeometry) {
            renderProfileDetails.push("Link geometry simplified for dense graphs.");
        }
        if (isParallelLayoutSimplified) {
            renderProfileDetails.push("Parallel link offset layout disabled for dense edges.");
        }
        if (!shouldEnableTooltips) {
            renderProfileDetails.push("Node tooltips disabled for very large graph.");
        }
        this.baseRenderProfile = {
            mode: renderProfileDetails.length > 0 ? "degraded" : "normal",
            summary: renderProfileDetails.length > 0
                ? "Performance mode enabled"
                : "Standard rendering",
            details: renderProfileDetails
        };
        this.notifyEffectiveRenderProfile();

        const nodeById = new Map(this.graph.nodes.map(node => [node.id, node]));
        const getSourceId = link => (link.source && link.source.id) ? link.source.id : link.source;
        const getTargetId = link => (link.target && link.target.id) ? link.target.id : link.target;

        const componentCount = Math.max(1, this.componentTargets?.size || 0);
        const chargeStrength = isHugeGraph ? -180 : (isVeryLargeGraph ? -360 : (isLargeGraph ? -680 : -1400));
        const linkDistance = isHugeGraph ? 56 : (isVeryLargeGraph ? 70 : (isLargeGraph ? 92 : 158));
        const collisionRadius = isHugeGraph ? 0 : (isVeryLargeGraph ? 18 : (isLargeGraph ? 30 : 44));
        const componentForceStrength = componentCount > 1 ? (isHugeGraph ? 0.03 : (isLargeGraph ? 0.09 : 0.06)) : 0;

        this.simulation = d3.forceSimulation(this.graph.nodes)
            .force("link", d3.forceLink(this.graph.links)
                .id(d => d.id)
                .distance(link => {
                    const sourceId = getSourceId(link);
                    const targetId = getTargetId(link);
                    if (sourceId && targetId) {
                        const sourceComponent = this.componentByNodeId.get(sourceId);
                        const targetComponent = this.componentByNodeId.get(targetId);
                        if (sourceComponent !== undefined && targetComponent !== undefined
                            && sourceComponent !== targetComponent) {
                            return linkDistance * 1.18;
                        }
                    }
                    return linkDistance;
                }))
            .force("charge", d3.forceManyBody().strength(chargeStrength))
            .force("center", d3.forceCenter(this.width / 2, this.height / 2))
            .alphaDecay(isHugeGraph ? 0.11 : (isVeryLargeGraph ? 0.07 : (isLargeGraph ? 0.05 : 0.024)))
            .velocityDecay(isHugeGraph ? 0.72 : (isVeryLargeGraph ? 0.62 : (isLargeGraph ? 0.54 : 0.4)))
            .on("tick", () => this.tickedThrottled())
            .on("end", () => {
                this.simulationStopped = true;
                this.refreshEdgeLabelPositions(true);
                this.scheduleLabelVisibilityUpdate();
            });
        if (collisionRadius > 0) {
            this.simulation.force("collision", d3.forceCollide().radius(collisionRadius));
        } else {
            this.simulation.force("collision", null);
        }

        if (componentForceStrength > 0) {
            this.simulation
                .force("componentX", d3.forceX(node => {
                    const componentIndex = this.componentByNodeId.get(node?.id) ?? 0;
                    return (this.componentTargets.get(componentIndex) || { x: this.width / 2 }).x;
                }).strength(componentForceStrength))
                .force("componentY", d3.forceY(node => {
                    const componentIndex = this.componentByNodeId.get(node?.id) ?? 0;
                    return (this.componentTargets.get(componentIndex) || { y: this.height / 2 }).y;
                }).strength(componentForceStrength));
        } else {
            this.simulation.force("componentX", null);
            this.simulation.force("componentY", null);
        }

        this.simulationStopped = false;

        this.zoomLayer = this.svg.append("g").attr("class", "zoom-layer");
        const gZoom = this.zoomLayer;

        this.zoomBehavior = d3.zoom()
            .scaleExtent([0.05, 10])
            .on("zoom", () => {
                const transform = d3.event.transform;
                gZoom.attr("transform", transform);
                this.currentTransform = transform;
                this.updateLevelOfDetail(transform.k);
                const sourceEvent = d3.event?.sourceEvent;
                if (!sourceEvent) {
                    this.scheduleLabelVisibilityUpdate();
                    return;
                }
                this.onInteraction(true);
            });
        this.svg.call(this.zoomBehavior);
        this.svg.call(this.zoomBehavior.transform, initialTransform || d3.zoomIdentity);

        const linkGroup = gZoom.append("g").attr("class", "links-layer");
        const labelGroup = gZoom.append("g").attr("class", "labels-layer");
        const nodeGroup = gZoom.append("g").attr("class", "nodes-layer");
        this.focusLabelLayer = gZoom.append("g").attr("class", "focus-label-layer");

        const defs = this.svg.append("defs");
        const graphIds = new Set();
        this.graph.nodes.forEach(node => {
            graphIds.add(this.normalizeGraphId(node?.graph));
        });
        this.graph.links.forEach(link => {
            graphIds.add(this.normalizeGraphId(link?.graph));
        });

        [...graphIds].forEach(graphId => {
            const color = this.getGraphColor(graphId);
            defs.append("marker")
                .attr("id", `arrow-${this.sanitizeId(graphId)}`)
                .attr("viewBox", "0 -5 10 10")
                .attr("refX", 9)
                .attr("markerWidth", 10)
                .attr("markerHeight", 10)
                .attr("markerUnits", "userSpaceOnUse")
                .attr("orient", "auto")
                .append("path")
                .attr("d", "M0,-4L10,0L0,4")
                .attr("fill", color);

            defs.append("marker")
                .attr("id", `arrow-loop-${this.sanitizeId(graphId)}`)
                .attr("viewBox", "0 -5 10 10")
                .attr("refX", 8)
                .attr("markerWidth", 9)
                .attr("markerHeight", 9)
                .attr("markerUnits", "userSpaceOnUse")
                .attr("orient", "auto")
                .append("path")
                .attr("d", "M0,-4L10,0L0,4")
                .attr("fill", color);
        });

        const resolveLinkGraphId = link => {
            const sourceNode = nodeById.get(getSourceId(link));
            const targetNode = nodeById.get(getTargetId(link));
            return this.normalizeGraphId(link?.graph || sourceNode?.graph || targetNode?.graph);
        };

        this.linkSelection = linkGroup.selectAll("path")
            .data(this.graph.links)
            .enter()
            .append("path")
            .attr("class", "edge-path")
            .attr("stroke", link => this.getGraphColor(resolveLinkGraphId(link)))
            .attr("stroke-width", isLargeGraph ? 1.7 : 2)
            .attr("marker-end", link => {
                const sourceId = this.getLinkEndpointId(link?.source);
                const targetId = this.getLinkEndpointId(link?.target);
                const markerPrefix = sourceId && targetId && sourceId === targetId ? "arrow-loop" : "arrow";
                return `url(#${markerPrefix}-${this.sanitizeId(resolveLinkGraphId(link))})`;
            });

        if (shouldCreateEdgeLabels) {
            this.linkLabelSelection = labelGroup.selectAll("text")
                .data(this.graph.links)
                .enter()
                .append("text")
                .attr("class", "edge-label")
                .attr("text-anchor", "middle")
                .text(link => this.formatLabel(link.name));
        } else {
            this.linkLabelSelection = null;
        }

        this.nodeSelection = nodeGroup.selectAll("g")
            .data(this.graph.nodes)
            .enter()
            .append("g")
            .call(d3.drag()
                .on("start", node => {
                    this.onInteraction(true);
                    if (!d3.event.active) {
                        this.simulationStopped = false;
                        this.simulation.alphaTarget(0.28).restart();
                    }
                    node.fx = node.x;
                    node.fy = node.y;
                })
                .on("drag", node => {
                    this.onInteraction(true);
                    node.fx = d3.event.x;
                    node.fy = d3.event.y;
                    // Keep links responsive while dragging, independent of tick throttle.
                    this.ticked();
                })
                .on("end", node => {
                    this.onInteraction(false);
                    if (!d3.event.active) {
                        this.simulation.alphaTarget(0);
                    }
                    node.fx = null;
                    node.fy = null;
                }));

        // Apply initial coordinates immediately to avoid a one-frame flash at
        // (0,0) before the first simulation tick.
        this.nodeSelection.attr("transform", node => (
            Number.isFinite(node?.x) && Number.isFinite(node?.y)
                ? `translate(${node.x},${node.y})`
                : null
        ));

        this.nodeSelection.each((node, index, nodes) => {
            const element = d3.select(nodes[index]);
            const size = isVeryLargeGraph ? 12 : 20;
            const strokeColor = this.getGraphColor(node.graph);
            const fillColor = this.resolveNodeFillColor(node.type);

            if (node.type === "Literal") {
                element.append("rect")
                    .attr("x", -size * 1.1)
                    .attr("y", -size * 0.6)
                    .attr("width", size * 2.2)
                    .attr("height", size * 1.2)
                    .attr("rx", 4)
                    .attr("fill", fillColor)
                    .attr("stroke", strokeColor)
                    .attr("stroke-width", 2.6);
                return;
            }

            element.append("circle")
                .attr("r", size)
                .attr("fill", fillColor)
                .attr("stroke", strokeColor)
                .attr("stroke-width", 2.6);
        });

        if (shouldCreateNodeLabels) {
            this.nodeLabelSelection = this.nodeSelection.append("text")
                .attr("class", "node-label")
                .attr("dy", isVeryLargeGraph ? 24 : 35)
                .attr("text-anchor", "middle")
                .text(node => {
                    const displayValue = this.resolveNodeDisplayValue(node);
                    if (node?.type === "Literal") {
                        return this.truncateLabel(displayValue);
                    }
                    return this.truncateLabel(this.formatLabel(displayValue));
                });
        } else {
            this.nodeLabelSelection = null;
        }

        // Render one immediate layout frame so links/labels are placed before
        // the first animated tick.
        this.ticked();
        this.updateLevelOfDetail(this.currentZoom);

        const tooltip = d3.select("#global-tooltip");
        if (shouldEnableTooltips) {
            const TOOLTIP_SHOW_DELAY_MS = 180;
            const TOOLTIP_MOVE_THROTTLE_MS = 16;
            const TOOLTIP_OFFSET = 24;
            const TOOLTIP_MARGIN = 12;
            let tooltipMoveTimer = null;
            let tooltipShowTimer = null;
            let tooltipVisible = false;
            let lastMouseEvent = null;

            const clearTooltipTimers = () => {
                if (tooltipShowTimer) {
                    clearTimeout(tooltipShowTimer);
                    tooltipShowTimer = null;
                }
                if (this.tooltipShowTimerHandle) {
                    clearTimeout(this.tooltipShowTimerHandle);
                    this.tooltipShowTimerHandle = null;
                }
                if (tooltipMoveTimer) {
                    clearTimeout(tooltipMoveTimer);
                    tooltipMoveTimer = null;
                }
                if (this.tooltipMoveTimerHandle) {
                    clearTimeout(this.tooltipMoveTimerHandle);
                    this.tooltipMoveTimerHandle = null;
                }
            };

            const positionTooltip = event => {
                if (!event?.pageX && !event?.pageY) {
                    return;
                }
                const tooltipNode = tooltip.node();
                if (!tooltipNode) {
                    return;
                }

                const tooltipWidth = Math.max(220, tooltipNode.offsetWidth || 0);
                const tooltipHeight = Math.max(90, tooltipNode.offsetHeight || 0);
                const viewportLeft = window.scrollX;
                const viewportTop = window.scrollY;
                const viewportRight = viewportLeft + window.innerWidth;
                const viewportBottom = viewportTop + window.innerHeight;
                const clientX = Number.isFinite(event.clientX) ? event.clientX : (event.pageX - viewportLeft);
                const clientY = Number.isFinite(event.clientY) ? event.clientY : (event.pageY - viewportTop);

                let left = event.pageX + TOOLTIP_OFFSET;
                let top = event.pageY + TOOLTIP_OFFSET;

                if (clientX > window.innerWidth * 0.56) {
                    left = event.pageX - tooltipWidth - TOOLTIP_OFFSET;
                }
                if (clientY > window.innerHeight * 0.56) {
                    top = event.pageY - tooltipHeight - TOOLTIP_OFFSET;
                }

                const minLeft = viewportLeft + TOOLTIP_MARGIN;
                const maxLeft = viewportRight - tooltipWidth - TOOLTIP_MARGIN;
                const minTop = viewportTop + TOOLTIP_MARGIN;
                const maxTop = viewportBottom - tooltipHeight - TOOLTIP_MARGIN;
                const clampedLeft = Math.max(minLeft, Math.min(maxLeft, left));
                const clampedTop = Math.max(minTop, Math.min(maxTop, top));

                tooltip
                    .style("left", `${clampedLeft}px`)
                    .style("top", `${clampedTop}px`);
            };

            const hideTooltip = () => {
                clearTooltipTimers();
                tooltipVisible = false;
                tooltip.style("opacity", 0);
            };

            const throttleTooltip = (callback, delay = TOOLTIP_MOVE_THROTTLE_MS) => {
                return (...args) => {
                    const event = d3.event;
                    if (!tooltipMoveTimer) {
                        tooltipMoveTimer = setTimeout(() => {
                            callback(event, ...args);
                            tooltipMoveTimer = null;
                            this.tooltipMoveTimerHandle = null;
                        }, delay);
                        this.tooltipMoveTimerHandle = tooltipMoveTimer;
                    }
                };
            };

            this.nodeSelection
                .on("mouseover", node => {
                    this.applyHoverFocus(node);
                    const isLiteral = node.type === "Literal";
                    const displayValue = this.resolveNodeDisplayValue(node);
                    const title = isLiteral
                        ? `"${this.formatLabel(displayValue)}"`
                        : this.formatLabel(displayValue);
                    let content = `<div class="tooltip-title">${title}</div>`;

                    if (isLiteral) {
                        content += `<div class="tooltip-row"><strong>Value</strong> <span>"${displayValue}"</span></div>`;
                        let typeValue = "xsd:string";
                        if (node.datatype) {
                            typeValue = this.formatLabel(node.datatype);
                        } else if (!Number.isNaN(Number(displayValue))) {
                            typeValue = "xsd:decimal";
                        }
                        content += `<div class="tooltip-row"><strong>Type</strong> <span>${typeValue}</span></div>`;
                        if (node.language) {
                            content += `<div class="tooltip-row"><strong>Lang</strong> <span>${node.language}</span></div>`;
                        }
                    } else if (node.type === "Blank") {
                        content += `<div class="tooltip-row"><strong>ID</strong> <span>${node.id}</span></div>`;
                        content += `<div class="tooltip-row"><strong>Type</strong> <span>Blank Node</span></div>`;
                    } else {
                        content += `<div class="tooltip-row"><strong>URI</strong> <span>${node.id}</span></div>`;
                        content += `<div class="tooltip-row"><strong>Type</strong> <span>${node.type}</span></div>`;
                    }

                    if (node.graphs?.size > 1) {
                        const graphList = Array.from(node.graphs).join(", ");
                        content += `<div class="tooltip-row"><strong>Graphs</strong> <span>${graphList}</span></div>`;
                        content += `<div class="tooltip-row"><strong>Primary</strong> <span>${node.graph || GRAPH_DEFAULTS.defaultGraphId}</span></div>`;
                    } else {
                        content += `<div class="tooltip-row"><strong>Graph</strong> <span>${node.graph || GRAPH_DEFAULTS.defaultGraphId}</span></div>`;
                    }

                    hideTooltip();
                    tooltip.html(content);
                    lastMouseEvent = d3.event;
                    positionTooltip(lastMouseEvent);
                    tooltipShowTimer = setTimeout(() => {
                        tooltipShowTimer = null;
                        this.tooltipShowTimerHandle = null;
                        if (!lastMouseEvent) {
                            return;
                        }
                        positionTooltip(lastMouseEvent);
                        tooltipVisible = true;
                        tooltip.style("opacity", 1);
                    }, TOOLTIP_SHOW_DELAY_MS);
                    this.tooltipShowTimerHandle = tooltipShowTimer;

                    const target = d3.event?.currentTarget;
                    if (target) {
                        d3.select(target).on("mousemove", throttleTooltip(event => {
                            if (event?.pageX != null && event?.pageY != null) {
                                lastMouseEvent = event;
                                if (tooltipVisible) {
                                    positionTooltip(event);
                                }
                            }
                        }));
                    }
                })
                .on("mouseout", () => {
                    this.clearHoverFocus(true);
                    hideTooltip();
                    const target = d3.event?.currentTarget;
                    if (target) {
                        d3.select(target).on("mousemove", null);
                    }
                });
        } else {
            if (this.tooltipShowTimerHandle) {
                clearTimeout(this.tooltipShowTimerHandle);
                this.tooltipShowTimerHandle = null;
            }
            if (this.tooltipMoveTimerHandle) {
                clearTimeout(this.tooltipMoveTimerHandle);
                this.tooltipMoveTimerHandle = null;
            }
            tooltip.style("opacity", 0);
            this.nodeSelection
                .on("mouseover", node => {
                    this.applyHoverFocus(node);
                })
                .on("mouseout", () => {
                    this.clearHoverFocus(true);
                });
        }

        this.refreshOverlayPanels();
        const clampedAlpha = Number.isFinite(initialAlpha)
            ? Math.max(0.05, Math.min(1, initialAlpha))
            : 1;
        this.simulation.alpha(clampedAlpha).restart();
    }

    /**
     * Throttled tick function to limit redraws for performance.
     */
    tickedThrottled() {
        const now = performance.now();
        if (now - this.lastTickTime < this.TICK_THROTTLE) {
            return; // Skip this tick
        }
        this.lastTickTime = now;

        // Auto-stabilize simulation when converged to save CPU (but keep it alive)
        if (this.simulation && this.simulation.alpha() < this.AUTO_STOP_ALPHA && !this.simulationStopped) {
            this.simulation.alphaTarget(0); // Set target to 0 but don't stop
            this.simulationStopped = true;
        }

        this.ticked();
        this.scheduleLabelVisibilityUpdate();
    }

    /**
     * Animation tick function.
     * Updates SVG element positions based on simulation data.
     */
    ticked() {
        const hasPos = d => d && typeof d.x === 'number' && typeof d.y === 'number';

        // Update Links
        if (this.linkSelection) {
            this.linkSelection.attr("d", d => {
                if (!hasPos(d.source) || !hasPos(d.target)) return null;
                const geometry = this.buildLinkGeometry(d);
                d.geometry = geometry;
                return geometry ? geometry.path : null;
            });
        }

        // Update Nodes
        if (this.nodeSelection) {
            this.nodeSelection.attr("transform", d => {
                if (!hasPos(d)) return null;
                return `translate(${d.x},${d.y})`;
            });
        }

        // Update Edge Labels (throttled)
        if (this.linkLabelSelection) {
            const now = performance.now();
            if (now - this.lastLabelUpdate >= this.LABEL_TICK_THROTTLE) {
                this.lastLabelUpdate = now;
                this.refreshEdgeLabelPositions(false);
            }
        }
    }

    /* -------------------------------------------------------------
     * HELPERS
     * ------------------------------------------------------------- */

    resolveNodeFillColor(nodeType) {
        switch (nodeType) {
            case "Literal":
                return GRAPH_NODE_FILL.Literal;
            case "Blank":
                return GRAPH_NODE_FILL.Blank;
            default:
                return GRAPH_NODE_FILL.Resource;
        }
    }

    getNodeVisualRadius(node) {
        if (node?.type === "Literal") {
            return 24;
        }
        return 20;
    }

    encodeLiteralNodeIdPart(value) {
        return encodeURIComponent(String(value ?? ""));
    }

    buildLiteralNodeId(value, datatype = "", language = "") {
        const lexicalPart = this.encodeLiteralNodeIdPart(value);
        const datatypePart = this.encodeLiteralNodeIdPart(datatype);
        const languagePart = this.encodeLiteralNodeIdPart(language);
        return `__lit__:${lexicalPart}|dt:${datatypePart}|lang:${languagePart}`;
    }

    resolveNodeDisplayValue(node) {
        if (!node) {
            return "";
        }
        if (node.type === "Literal") {
            return String(node.name ?? "");
        }
        return String(node.id ?? "");
    }

    /**
     * Formats a URI into a readable label.
     * @param {string} uri - The URI.
     * @returns {string} Short label.
     */
    formatLabel(uri) {
        if (!uri) return "";
        if (uri === GRAPH_DEFAULTS.rdfTypePredicate) return "rdf:type";
        if (uri.startsWith('_:')) return uri;
        const parts = uri.includes('#') ? uri.split('#') : uri.split('/');
        const last = parts.pop();
        return last || uri;
    }

    /**
     * Truncates a label if it's too long.
     * @param {string} label - The label.
     * @returns {string} Truncated label.
     */
    truncateLabel(label) {
        return label.length > 25 ? label.substring(0, 22) + '...' : label;
    }

}

// Register Custom Element
customElements.define('kg-graph', KGGraphVis);
