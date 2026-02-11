/* =================================================================
 * CORESE KNOWLEDGE GRAPH - VISUALIZATION COMPONENT
 * =================================================================
 * Custom web component for rendering RDF/JSON-LD knowledge graphs.
 * Uses D3.js for force-directed graph layout and visualization.
 * Supports multiple named graphs with color coding.
 * ================================================================= */

"use strict";

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

        // Component state
        this.internalData = new WeakMap();
        this.internalData.set(this, {});
        this.resizeObserver = null;
        this.showEdgeLabels = true;
        this.nodeLabelsVisible = true;
        this.edgeLabelsVisible = true;
        this.currentZoom = 1;
        this.nodeLabelZoomThreshold = 0.2;
        this.edgeLabelZoomThreshold = 0.2;
        this.labelCullEnabled = true;
        this.labelVisibilityThrottleMs = 80;
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

        // Graph coloring
        this.graphColorMap = new Map();
        this.defaultGraphColor = '#6B7280';

        // Performance optimization
        this.TICK_THROTTLE = 16; // ~60fps throttle for ticked()
        this.lastTickTime = 0;
        this.LABEL_TICK_THROTTLE = 50; // Throttle label position updates
        this.lastLabelUpdate = 0;
        this.simulationStopped = false;
        this.AUTO_STOP_ALPHA = 0.005; // Alpha threshold for auto-stabilize (very low)
        this.PARALLEL_LINK_SPACING = 22;
        this.PARALLEL_LINK_MAX_CURVE = 90;
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

    /**
     * Zoom in by 30%.
     */
    zoomIn() {
        if (this.svg && this.zoomBehavior) {
            this.svg.transition().duration(300).call(this.zoomBehavior.scaleBy, 1.3);
        }
    }

    /**
     * Zoom out by 30%.
     */
    zoomOut() {
        if (this.svg && this.zoomBehavior) {
            this.svg.transition().duration(300).call(this.zoomBehavior.scaleBy, 1 / 1.3);
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
            this.nodeLabelSelection.style('display', null);
        }
        if (this.linkLabelSelection) {
            this.linkLabelSelection.style('display', this.showEdgeLabels ? null : 'none');
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
            this.nodeLabelSelection.style('display', prevNodeVisible ? null : 'none');
        }
        if (this.linkLabelSelection) {
            this.linkLabelSelection.style('display', prevEdgeVisible ? null : 'none');
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
        // Future theme logic can go here
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
    }

    /* -------------------------------------------------------------
     * COLOR MANAGEMENT
     * ------------------------------------------------------------- */

    /**
     * Generate distinct colors for named graphs using the golden angle.
     * @param {string} graphId - Graph identifier.
     * @returns {string} Hex color code.
     */
    getGraphColor(graphId = 'default') {
        const gid = graphId ?? 'default';
        if (gid === 'default') {
            return this.defaultGraphColor;
        }

        if (!this.graphColorMap.has(gid)) {
            const index = this.graphColorMap.size;
            // Use golden angle approx (137.5) for optimal color distribution
            const hue = (index * 137.508) % 360;
            const saturation = 35 + (index % 4) * 10;
            const lightness = 72 + (index % 3) * 6;
            const color = this.hslToHex(hue, saturation, lightness);
            this.graphColorMap.set(gid, color);
        }
        return this.graphColorMap.get(gid);
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

    assignParallelLinkOffsets(links = []) {
        if (!Array.isArray(links)) return;

        const pairGroups = new Map();
        const setCenteredOffsets = groupLinks => {
            const center = (groupLinks.length - 1) / 2;
            groupLinks.forEach((link, index) => {
                link.parallelOffsetUnit = index - center;
            });
        };
        const setDirectionalOffsets = groupLinks => {
            groupLinks.forEach((link, index) => {
                link.parallelOffsetUnit = index + 0.5;
            });
        };

        links.forEach(link => {
            link.parallelOffsetUnit = 0;
            const sourceId = this.getLinkEndpointId(link?.source);
            const targetId = this.getLinkEndpointId(link?.target);
            if (!sourceId || !targetId) return;
            if (sourceId === targetId) return;

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
                return;
            }

            const singleDirection = group.forward.length > 0 ? group.forward : group.backward;
            setCenteredOffsets(singleDirection);
        });
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
        const dx = tx - sx;
        const dy = ty - sy;
        const distance = Math.hypot(dx, dy);

        if (distance < 1e-6) {
            const loopSize = 34 + Math.abs(link?.parallelOffsetUnit || 0) * 10;
            const startX = sx;
            const startY = sy - 20;
            const endX = sx + 1;
            const endY = sy - 20;
            const c1x = sx + loopSize;
            const c1y = sy - loopSize;
            const c2x = sx + loopSize;
            const c2y = sy + loopSize;
            const labelX = sx + loopSize + 6;
            const labelY = sy;
            return {
                path: `M${startX},${startY} C${c1x},${c1y} ${c2x},${c2y} ${endX},${endY}`,
                labelX,
                labelY
            };
        }

        const midpointX = (sx + tx) / 2;
        const midpointY = (sy + ty) / 2;
        const offsetUnit = Number.isFinite(link?.parallelOffsetUnit) ? link.parallelOffsetUnit : 0;
        const maxForDistance = Math.min(this.PARALLEL_LINK_MAX_CURVE, Math.max(distance * 0.35, 0));
        const rawCurveOffset = offsetUnit * this.PARALLEL_LINK_SPACING;
        const curveOffset = Math.max(-maxForDistance, Math.min(maxForDistance, rawCurveOffset));

        if (Math.abs(curveOffset) < 0.5) {
            return {
                path: `M${sx},${sy} L${tx},${ty}`,
                labelX: midpointX,
                labelY: midpointY
            };
        }

        const normalX = -dy / distance;
        const normalY = dx / distance;
        const controlX = midpointX + normalX * curveOffset;
        const controlY = midpointY + normalY * curveOffset;
        const t = 0.5;
        const invT = 1 - t;
        const labelX = invT * invT * sx + 2 * invT * t * controlX + t * t * tx;
        const labelY = invT * invT * sy + 2 * invT * t * controlY + t * t * ty;

        return {
            path: `M${sx},${sy} Q${controlX},${controlY} ${tx},${ty}`,
            labelX,
            labelY
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
                    width: 100%; 
                    height: 100%; 
                    box-sizing: border-box; 
                }
                svg { 
                    width: 100%; 
                    height: 100%; 
                    display: block; 
                    cursor: grab; 
                    user-select: none; 
                }
                svg:active { 
                    cursor: grabbing; 
                }
                svg.labels-hidden .node-label,
                svg.labels-hidden .edge-label {
                    display: none;
                }
                svg.node-labels-off .node-label {
                    display: none;
                }
                svg.edge-labels-off .edge-label {
                    display: none;
                }
                .node-label {
                    pointer-events: none; 
                    font-family: 'Segoe UI', system-ui, sans-serif;
                    font-size: 13px; 
                    font-weight: 600;
                    fill: var(--text-color, #333);
                }
                .edge-label { 
                    font-size: 11px; 
                    pointer-events: none; 
                    fill: var(--text-color, #333);
                }
                .edge-path {
                    fill: none;
                }
            </style>
            <div class="container" id="main-container">
                <svg id="chart-container"></svg>
            </div>
        `;
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
                    this.simulation.force("center", d3.forceCenter(this.width / 2, this.height / 2));
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
        const showEdgeLabels = zoom >= this.edgeLabelZoomThreshold && this.showEdgeLabels;
        this.nodeLabelsVisible = showNodeLabels;
        this.edgeLabelsVisible = showEdgeLabels;

        if (this.svg) {
            this.svg.classed('node-labels-off', !showNodeLabels);
            this.svg.classed('edge-labels-off', !showEdgeLabels);
        }
        this.scheduleLabelVisibilityUpdate();
    }

    shouldHideLabelsDuringInteraction() {
        if (!this.interactionHideLabels) return false;
        const nodeCount = this.graph?.nodes?.length ?? 0;
        const linkCount = this.graph?.links?.length ?? 0;
        return nodeCount >= this.interactionHideNodeThreshold
            || linkCount >= this.interactionHideLinkThreshold;
    }

    hideLabelsForInteraction() {
        this.labelsHiddenForInteraction = true;
        if (this.svg) {
            this.svg.classed('labels-hidden', true);
        }
    }

    onInteraction(shouldHideLabels = true) {
        this.isInteracting = true;
        if (shouldHideLabels && this.shouldHideLabelsDuringInteraction()) {
            this.hideLabelsForInteraction();
        }
        if (this.interactionTimer) {
            clearTimeout(this.interactionTimer);
        }
        this.interactionTimer = setTimeout(() => {
            this.isInteracting = false;
            this.interactionTimer = null;
            this.labelsHiddenForInteraction = false;
            if (this.svg) {
                this.svg.classed('labels-hidden', false);
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
        if (!showNodeLabels && this.nodeLabelSelection) {
            this.nodeLabelSelection.style('display', 'none');
        }
        if (!showEdgeLabels && this.linkLabelSelection) {
            this.linkLabelSelection.style('display', 'none');
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
            this.nodeLabelSelection.style('display', d => {
                if (!d || typeof d.x !== 'number' || typeof d.y !== 'number') {
                    return null;
                }
                return inView(d.x, d.y) ? null : 'none';
            });
        }

        if (showEdgeLabels) {
            this.linkLabelSelection.style('display', d => {
                const geometry = d?.geometry ?? this.buildLinkGeometry(d);
                if (!geometry) {
                    return 'none';
                }
                return inView(geometry.labelX, geometry.labelY) ? null : 'none';
            });
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
        this.graphColorMap = new Map();
        const nodeById = new Map();

        const normalizeArray = value => (Array.isArray(value) ? value : [value]);
        const isObject = value => value !== null && typeof value === 'object';
        const resolveGraphId = graphId => graphId || 'default';
        const isInferenceGraphId = graphId => {
            const gid = resolveGraphId(graphId);
            return gid.startsWith('urn:corese:inference:')
                || gid === 'http://ns.inria.fr/corese/rule'
                || gid === 'http://ns.inria.fr/corese/constraint';
        };
        const graphPriority = graphId => {
            const gid = resolveGraphId(graphId);
            if (gid === 'default') {
                return 3;
            }
            if (isInferenceGraphId(gid)) {
                return 1;
            }
            return 2;
        };
        const shouldPreferGraph = (currentGraph, candidateGraph) =>
            graphPriority(candidateGraph) > graphPriority(currentGraph);

        const upsertNode = (id, type, graphId, meta = {}, isSubject = false) => {
            if (!id) return null;
            const resolvedGraph = resolveGraphId(graphId);
            const existing = nodeById.get(id);
            if (existing) {
                if (type === 'Blank' || type === 'Class') {
                    existing.type = type;
                }

                if (!existing.graphs) existing.graphs = new Set([existing.graph]);
                existing.graphs.add(resolvedGraph);

                if (isSubject) {
                    if (!existing.isDefinedAsSubject) {
                        existing.graph = resolvedGraph;
                        existing.isDefinedAsSubject = true;
                        existing.definitionGraph = resolvedGraph;
                    } else if (shouldPreferGraph(existing.definitionGraph || existing.graph, resolvedGraph)) {
                        existing.graph = resolvedGraph;
                        existing.definitionGraph = resolvedGraph;
                    }
                } else if (!existing.isDefinedAsSubject && shouldPreferGraph(existing.graph, resolvedGraph)) {
                    existing.graph = resolvedGraph;
                }

                Object.assign(existing, meta);
                return existing;
            }

            const node = {
                id,
                name: id,
                type,
                graph: resolvedGraph,
                graphs: new Set([resolvedGraph]),
                isDefinedAsSubject: isSubject,
                definitionGraph: isSubject ? resolvedGraph : null,
                ...meta
            };
            graph.nodes.push(node);
            nodeById.set(id, node);
            return node;
        };

        const resolveSubjectType = (item, subjectId) => {
            const types = normalizeArray(item['@type'] ?? []);
            if (types.some(t => t.includes('Class'))) {
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
                    const objId = String(obj['@value']);
                    const meta = {};
                    if (obj['@type']) meta.datatype = obj['@type'];
                    if (obj['@language']) meta.language = obj['@language'];
                    return { objId, objType: 'Literal', meta };
                }
            }
            return { objId: String(obj), objType: 'Literal', meta: {} };
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

        const processItem = (item, currentGraph) => {
            if (!isObject(item)) return;
            if (processGraphContainer(item, currentGraph)) return;

            const subj = item['@id'];
            if (!subj) return;

            const subjType = resolveSubjectType(item, subj);
            const isSubstantial = isSubstantialDefinition(item);
            upsertNode(subj, subjType, currentGraph, {}, isSubstantial);
            processPredicates(item, subj, currentGraph);
        };

        const root = this.jsonLDOntology;
        normalizeArray(root).forEach(item => processItem(item, 'default'));
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
        const preservedTransform = this.resolvePreservedTransform();
        if (this.simulation) this.simulation.stop();
        if (this.interactionTimer) {
            clearTimeout(this.interactionTimer);
            this.interactionTimer = null;
        }
        if (this.labelVisibilityRaf) {
            cancelAnimationFrame(this.labelVisibilityRaf);
            this.labelVisibilityRaf = null;
        }

        const chartSvg = this.shadowRoot.querySelector("#chart-container");
        if (!chartSvg) return;

        this.svg = d3.select(chartSvg);
        this.svg.selectAll("*").remove();
        this.isInteracting = false;
        this.labelsHiddenForInteraction = false;
        this.lastLabelVisibilityUpdate = 0;
        this.lastLabelUpdate = 0;

        if (!this.jsonld) {
            this.graph = { nodes: [], links: [] };
            this.simulation = null;
            this.linkSelection = null;
            this.nodeSelection = null;
            this.linkLabelSelection = null;
            this.nodeLabelSelection = null;
            this.currentTransform = d3.zoomIdentity;
            return;
        }

        try {
            this.jsonLDOntology = JSON.parse(this.jsonld);
            const graph = this.createGraph();
            this.graph = graph;
            this.assignParallelLinkOffsets(this.graph.links);
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
                node.x = anchor.x + (Math.random() - 0.5) * 220;
                node.y = anchor.y + (Math.random() - 0.5) * 220;
                node.vx = 0;
                node.vy = 0;
            });

            const addedNodeCount = this.graph.nodes.length - reusedNodeCount;
            const addedRatio = this.graph.nodes.length === 0
                ? 0
                : addedNodeCount / this.graph.nodes.length;
            const initialAlpha = !hadExistingLayout
                ? 1
                : (addedRatio <= 0.12 ? 0.18 : (addedRatio <= 0.35 ? 0.30 : 0.55));

            this.renderGraph(preservedTransform, initialAlpha);
        } catch (e) {
            console.error("Graph drawing error:", e);
        }
    }

    /**
     * Configures D3 forces, zoom, and appends visual elements.
     */
    renderGraph(initialTransform = d3.zoomIdentity, initialAlpha = 1) {
        // Force Simulation Configuration with adaptive strength for large graphs
        const nodeCount = this.graph.nodes.length;
        const isLargeGraph = nodeCount > 200;
        const nodeById = new Map(this.graph.nodes.map(node => [node.id, node]));
        const getSourceId = link => (link.source && link.source.id) ? link.source.id : link.source;
        const getTargetId = link => (link.target && link.target.id) ? link.target.id : link.target;
        
        // Adjust forces based on graph size
        const chargeStrength = isLargeGraph ? -800 : -1500;
        const linkDistance = isLargeGraph ? 100 : 160;
        const collisionRadius = isLargeGraph ? 30 : 45;
        
        this.simulation = d3.forceSimulation(this.graph.nodes)
            .force("link", d3.forceLink(this.graph.links).id(d => d.id).distance(linkDistance))
            .force("charge", d3.forceManyBody().strength(chargeStrength))
            .force("center", d3.forceCenter(this.width / 2, this.height / 2))
            .force("collision", d3.forceCollide().radius(collisionRadius))
            .alphaDecay(isLargeGraph ? 0.05 : 0.0228) // Faster convergence for large graphs
            .velocityDecay(isLargeGraph ? 0.5 : 0.4) // More damping for large graphs
            .on("tick", () => this.tickedThrottled())
            .on("end", () => {
                this.simulationStopped = true;
            });
        
        this.simulationStopped = false;

        // Zoom Layer
        const gZoom = this.svg.append("g").attr("class", "zoom-layer");

        // Zoom Behavior
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
                const isWheel = sourceEvent?.type === 'wheel';
                const isDblClick = sourceEvent?.type === 'dblclick';
                this.onInteraction(!(isWheel || isDblClick));
            });
        this.svg.call(this.zoomBehavior);
        this.svg.call(this.zoomBehavior.transform, initialTransform || d3.zoomIdentity);

        // Layers
        const linkGroup = gZoom.append("g").attr("class", "links-layer");
        const labelGroup = gZoom.append("g").attr("class", "labels-layer");
        const nodeGroup = gZoom.append("g").attr("class", "nodes-layer");

        // SVG Definitions (Markers, Gradients)
        const defs = this.svg.append("defs");

        // Create Markers
        const graphIds = [...new Set(this.graph.nodes.map(n => n.graph))];
        graphIds.forEach(graphId => {
            const color = this.getGraphColor(graphId);
            defs.append("marker")
                .attr("id", `arrow-${this.sanitizeId(graphId)}`)
                .attr("viewBox", "0 -5 10 10")
                .attr("refX", 32)
                .attr("markerWidth", 6)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("path")
                .attr("d", "M0,-5L10,0L0,5")
                .attr("fill", color);
        });

        // Create Gradients
        this.graph.links.forEach((link, i) => {
            const sourceNode = nodeById.get(getSourceId(link));
            const targetNode = nodeById.get(getTargetId(link));

            if (sourceNode && targetNode && sourceNode.graph !== targetNode.graph) {
                const gradId = `gradient-${i}`;
                const gradient = defs.append("linearGradient")
                    .attr("id", gradId)
                    .attr("gradientUnits", "userSpaceOnUse")
                    .attr("x1", sourceNode.x || 0)
                    .attr("y1", sourceNode.y || 0)
                    .attr("x2", targetNode.x || 0)
                    .attr("y2", targetNode.y || 0);

                gradient.append("stop")
                    .attr("offset", "0%")
                    .attr("stop-color", this.getGraphColor(sourceNode.graph));

                gradient.append("stop")
                    .attr("offset", "100%")
                    .attr("stop-color", this.getGraphColor(targetNode.graph));

                link.gradientId = gradId;
            }
        });

        // Draw Links
        this.linkSelection = linkGroup.selectAll("path")
            .data(this.graph.links).enter().append("path")
            .attr("class", "edge-path")
            .attr("stroke", d => {
                const sNode = nodeById.get(getSourceId(d));
                const tNode = nodeById.get(getTargetId(d));

                if (sNode && tNode) {
                    if (sNode.graph === tNode.graph) {
                        return this.getGraphColor(sNode.graph);
                    } else if (d.gradientId) {
                        return `url(#${d.gradientId})`;
                    }
                }
                return '#999';
            })
            .attr("stroke-width", 2)
            .attr("marker-end", d => {
                const tNode = nodeById.get(getTargetId(d));
                if (tNode) {
                    return `url(#arrow-${this.sanitizeId(tNode.graph)})`;
                }
                return null;
            });

        // Draw Edge Labels
        this.linkLabelSelection = labelGroup.selectAll("text")
            .data(this.graph.links).enter().append("text")
            .attr("class", "edge-label")
            .attr("text-anchor", "middle")
            .text(d => this.formatLabel(d.name));

        // Draw Nodes with Drag Behavior
        this.nodeSelection = nodeGroup.selectAll("g")
            .data(this.graph.nodes).enter().append("g")
            .call(d3.drag()
                .on("start", (d) => {
                    if (!d3.event.active) {
                        this.simulationStopped = false; // Re-enable ticking when dragging
                        this.simulation.alphaTarget(0.3).restart();
                    }
                    d.fx = d.x;
                    d.fy = d.y;
                })
                .on("drag", (d) => {
                    d.fx = d3.event.x;
                    d.fy = d3.event.y;
                })
                .on("end", (d) => {
                    if (!d3.event.active) {
                        this.simulation.alphaTarget(0);
                    }
                    d.fx = null;
                    d.fy = null;
                }));

        // Node Shapes
        this.nodeSelection.each((d, i, nodes) => {
            const el = d3.select(nodes[i]);
            const size = 20;
            const strokeColor = this.getGraphColor(d.graph);

            if (d.type === 'Literal') {
                el.append("rect")
                    .attr("x", -size * 1.2)
                    .attr("y", -size * 0.7)
                    .attr("width", size * 2.4)
                    .attr("height", size * 1.4)
                    .attr("rx", 4)
                    .attr("fill", '#ff7f0e')
                    .attr("stroke", strokeColor)
                    .attr("stroke-width", 3);
            } else {
                el.append("circle")
                    .attr("r", size)
                    .attr("fill", d.type === 'Blank' ? '#2ca02c' : '#1f77b4')
                    .attr("stroke", strokeColor)
                    .attr("stroke-width", 3);
            }
        });

        // Node Labels
        this.nodeLabelSelection = this.nodeSelection.append("text")
            .attr("class", "node-label")
            .attr("dy", 35)
            .attr("text-anchor", "middle")
            .text(d => this.truncateLabel(this.formatLabel(d.id)));

        this.updateLevelOfDetail(this.currentZoom);

        // Tooltip Interaction
        // Note: #global-tooltip is outside shadow DOM
        const tooltip = d3.select("#global-tooltip");
        
        // Throttle function to limit mousemove frequency
        let tooltipMoveTimer = null;
        const throttleTooltip = (callback, delay = 16) => { // ~60fps
            return (...args) => {
                const event = d3.event; // Capture event
                if (!tooltipMoveTimer) {
                    tooltipMoveTimer = setTimeout(() => {
                        callback(event, ...args);
                        tooltipMoveTimer = null;
                    }, delay);
                }
            };
        };
        
        this.nodeSelection
            .on("mouseover", (d) => {
                const isLiteral = d.type === 'Literal';
                const title = isLiteral ? `"${this.formatLabel(d.id)}"` : this.formatLabel(d.id);
                let content = `<div class="tooltip-title">${title}</div>`;

                if (isLiteral) {
                    content += `<div class="tooltip-row"><strong>Value</strong> <span>"${d.id}"</span></div>`;
                    let typeVal = 'xsd:string';
                    if (d.datatype) {
                        typeVal = this.formatLabel(d.datatype);
                    } else if (!Number.isNaN(Number(d.id))) {
                        typeVal = 'xsd:decimal';
                    }
                    content += `<div class="tooltip-row"><strong>Type</strong> <span>${typeVal}</span></div>`;
                    if (d.language) {
                        content += `<div class="tooltip-row"><strong>Lang</strong> <span>${d.language}</span></div>`;
                    }
                } else if (d.type === 'Blank') {
                    content += `<div class="tooltip-row"><strong>ID</strong> <span>${d.id}</span></div>`;
                    content += `<div class="tooltip-row"><strong>Type</strong> <span>Blank Node</span></div>`;
                } else {
                    content += `<div class="tooltip-row"><strong>URI</strong> <span>${d.id}</span></div>`;
                    content += `<div class="tooltip-row"><strong>Type</strong> <span>${d.type}</span></div>`;
                }

                if (d.graphs?.size > 1) {
                    const graphList = Array.from(d.graphs ?? []).join(', ');
                    content += `<div class="tooltip-row"><strong>Graphs</strong> <span>${graphList}</span></div>`;
                    content += `<div class="tooltip-row"><strong>Primary</strong> <span>${d.graph || 'default'}</span></div>`;
                } else {
                    content += `<div class="tooltip-row"><strong>Graph</strong> <span>${d.graph || 'default'}</span></div>`;
                }

                tooltip.style("opacity", 1).html(content);
                
                // Attach mousemove only when hovering over node
                const target = d3.event?.currentTarget;
                if (target) {
                    d3.select(target).on("mousemove", throttleTooltip((event) => {
                        if (event?.pageX != null && event?.pageY != null) {
                            tooltip
                                .style("left", (event.pageX + 15) + "px")
                                .style("top", (event.pageY - 10) + "px");
                        }
                    }));
                }
            })
            .on("mouseout", () => {
                tooltip.style("opacity", 0);
                // Remove mousemove listener when leaving node
                const target = d3.event?.currentTarget;
                if (target) {
                    d3.select(target).on("mousemove", null);
                }
                if (tooltipMoveTimer) {
                    clearTimeout(tooltipMoveTimer);
                    tooltipMoveTimer = null;
                }
            });

        // Start!
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

            // Update Gradients (throttled for performance)
            // Only update every 3rd tick for large graphs
            const shouldUpdateGradients = this.graph.nodes.length < 200 || (this.lastTickTime % 48) < this.TICK_THROTTLE;
            if (shouldUpdateGradients) {
                this.linkSelection.each((d) => {
                    if (d.gradientId && hasPos(d.source) && hasPos(d.target)) {
                        const gradient = this.svg.select(`#${d.gradientId}`);
                        if (!gradient.empty()) {
                            gradient
                                .attr("x1", d.source.x)
                                .attr("y1", d.source.y)
                                .attr("x2", d.target.x)
                                .attr("y2", d.target.y);
                        }
                    }
                });
            }
        }

        // Update Nodes
        if (this.nodeSelection) {
            this.nodeSelection.attr("transform", d => {
                if (!hasPos(d)) return null;
                return `translate(${d.x},${d.y})`;
            });
        }

        // Update Edge Labels (throttled)
        if (this.linkLabelSelection && this.edgeLabelsVisible && !this.labelsHiddenForInteraction) {
            const now = performance.now();
            if (now - this.lastLabelUpdate >= this.LABEL_TICK_THROTTLE) {
                this.lastLabelUpdate = now;
                this.linkLabelSelection
                    .attr("x", d => {
                        if (!hasPos(d.source) || !hasPos(d.target)) return null;
                        const geometry = d.geometry ?? this.buildLinkGeometry(d);
                        return geometry ? geometry.labelX : null;
                    })
                    .attr("y", d => {
                        if (!hasPos(d.source) || !hasPos(d.target)) return null;
                        const geometry = d.geometry ?? this.buildLinkGeometry(d);
                        return geometry ? geometry.labelY : null;
                    });
            }
        }
    }

    /* -------------------------------------------------------------
     * HELPERS
     * ------------------------------------------------------------- */

    /**
     * Formats a URI into a readable label.
     * @param {string} uri - The URI.
     * @returns {string} Short label.
     */
    formatLabel(uri) {
        if (!uri) return "";
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
