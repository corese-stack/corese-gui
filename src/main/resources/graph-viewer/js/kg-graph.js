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

        // Component state
        this.internalData = new WeakMap();
        this.internalData.set(this, {});
        this.resizeObserver = null;
        this.showEdgeLabels = true;
        this.width = 800;
        this.height = 600;

        // Graph coloring
        this.graphColorMap = new Map();
        this.defaultGraphColor = '#6B7280';

        // Performance optimization
        this.MAX_NODES = 1000;  // Maximum nodes to display
        this.MAX_LINKS = 2000;  // Maximum links to display
        this.TICK_THROTTLE = 16; // ~60fps throttle for ticked()
        this.lastTickTime = 0;
        this.simulationStopped = false;
        this.AUTO_STOP_ALPHA = 0.005; // Alpha threshold for auto-stabilize (very low)
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
        if (this.linkLabelSelection) {
            this.linkLabelSelection.style('display', show ? null : 'none');
        }
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

    async connectedCallback() {
        this.render();
        this.observeResize();
        await this.updateSize();
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
    getGraphColor(graphId) {
        const gid = graphId || 'default';

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
        s /= 100;
        l /= 100;
        const c = (1 - Math.abs(2 * l - 1)) * s;
        const x = c * (1 - Math.abs((h / 60) % 2 - 1));
        const m = l - c / 2;
        let r = 0, g = 0, b = 0;

        if (h >= 0 && h < 60) { r = c; g = x; b = 0; }
        else if (h >= 60 && h < 120) { r = x; g = c; b = 0; }
        else if (h >= 120 && h < 180) { r = 0; g = c; b = x; }
        else if (h >= 180 && h < 240) { r = 0; g = x; b = c; }
        else if (h >= 240 && h < 300) { r = x; g = 0; b = c; }
        else if (h >= 300 && h < 360) { r = c; g = 0; b = x; }

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
    sanitizeId(str) {
        return (str || 'default').replace(/[^a-zA-Z0-9-_]/g, '_');
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
        this.resizeObserver = new ResizeObserver(async () => {
            await this.updateSize();
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
    async updateSize() {
        const rect = this.getBoundingClientRect();
        this.width = rect.width || 800;
        this.height = rect.height || 600;
    }

    /* -------------------------------------------------------------
     * GRAPH PROCESSING
     * ------------------------------------------------------------- */

    /**
     * Parse JSON-LD into graph structure (nodes and links).
     * @returns {Object} Graph object with nodes and links arrays.
     */
    async createGraph() {
        const graph = { nodes: [], links: [] };
        this.graphColorMap = new Map();

        const addNode = (id, type, graphId, meta = {}, isSubject = false) => {
            if (!id) return null;

            let node = graph.nodes.find(n => n.id === id);
            if (!node) {
                // Create new node
                node = {
                    id,
                    name: id,
                    type,
                    graph: graphId || 'default',
                    graphs: new Set([graphId || 'default']),
                    isDefinedAsSubject: isSubject,
                    definitionGraph: isSubject ? (graphId || 'default') : null,
                    ...meta
                };
                graph.nodes.push(node);
            } else {
                // Update existing node
                if (type === 'Blank' || type === 'Class') {
                    node.type = type;
                }

                if (!node.graphs) node.graphs = new Set([node.graph]);
                node.graphs.add(graphId || 'default');

                // Determine primary graph based on subject definition
                if (isSubject && graphId && graphId !== 'default') {
                    if (!node.isDefinedAsSubject) {
                        node.graph = graphId;
                        node.isDefinedAsSubject = true;
                        node.definitionGraph = graphId;
                    }
                } else if ((!node.graph || node.graph === 'default') && graphId && graphId !== 'default') {
                    if (!node.isDefinedAsSubject) {
                        node.graph = graphId;
                    }
                }
                Object.assign(node, meta);
            }
            return node;
        };

        const processItem = (item, currentGraph) => {
            if (!item || typeof item !== 'object') return;

            if (item['@graph']) {
                const newGraph = item['@id'] || currentGraph;
                const contents = Array.isArray(item['@graph']) ? item['@graph'] : [item['@graph']];
                contents.forEach(child => processItem(child, newGraph));
                return;
            }

            const subj = item['@id'];
            if (!subj) return;

            let subjType = 'Resource';
            if (item['@type']) {
                const types = Array.isArray(item['@type']) ? item['@type'] : [item['@type']];
                if (types.some(t => t.includes('Class'))) subjType = 'Class';
            } else if (subj.startsWith('_:')) {
                subjType = 'Blank';
            }

            // Determine if this is a substantial definition (has properties or type)
            // vs just a reference (only @id). This prevents references from hijacking the primary graph.
            const hasProperties = Object.keys(item).some(k => !k.startsWith('@'));
            const isSubstantial = hasProperties || !!item['@type'];

            addNode(subj, subjType, currentGraph, {}, isSubstantial);

            Object.keys(item).forEach(pred => {
                if (pred.startsWith('@')) return;

                const objs = Array.isArray(item[pred]) ? item[pred] : [item[pred]];
                objs.forEach(obj => {
                    let objId;
                    let objType = 'Literal';
                    let meta = {};

                    if (typeof obj === 'object' && obj['@id']) {
                        objId = obj['@id'];
                        objType = objId.startsWith('_:') ? 'Blank' : 'Resource';
                        processItem(obj, currentGraph);
                    } else if (typeof obj === 'object' && obj['@value'] !== undefined) {
                        objId = String(obj['@value']);
                        if (obj['@type']) meta.datatype = obj['@type'];
                        if (obj['@language']) meta.language = obj['@language'];
                    } else {
                        objId = String(obj);
                    }

                    if (objId !== undefined && objId !== "undefined") {
                        addNode(objId, objType, currentGraph, meta, false);
                        graph.links.push({
                            source: subj,
                            target: objId,
                            name: pred,
                            graph: currentGraph
                        });
                    }
                });
            });
        };

        // Process root JSON-LD
        const root = this.jsonLDOntology;
        const data = Array.isArray(root) ? root : [root];
        data.forEach(d => processItem(d, 'default'));

        // Performance optimization: Sample large graphs
        if (graph.nodes.length > this.MAX_NODES) {
            // Graph sampled for performance (keeping most important nodes)
            graph = this.sampleGraph(graph);
        }

        return graph;
    }

    /**
     * Sample a large graph to a manageable size while preserving structure.
     * Uses degree-based importance sampling.
     * @param {Object} graph - The full graph.
     * @returns {Object} Sampled graph.
     */
    sampleGraph(graph) {
        // Calculate node degrees (importance)
        const nodeDegree = new Map();
        graph.nodes.forEach(n => nodeDegree.set(n.id, 0));
        graph.links.forEach(l => {
            const src = typeof l.source === 'object' ? l.source.id : l.source;
            const tgt = typeof l.target === 'object' ? l.target.id : l.target;
            nodeDegree.set(src, (nodeDegree.get(src) || 0) + 1);
            nodeDegree.set(tgt, (nodeDegree.get(tgt) || 0) + 1);
        });

        // Sort by degree and take top nodes
        const sortedNodes = [...graph.nodes].sort((a, b) => 
            (nodeDegree.get(b.id) || 0) - (nodeDegree.get(a.id) || 0)
        );
        const selectedNodes = new Set(sortedNodes.slice(0, this.MAX_NODES).map(n => n.id));

        // Filter links to only include selected nodes
        const sampledLinks = graph.links.filter(l => {
            const src = typeof l.source === 'object' ? l.source.id : l.source;
            const tgt = typeof l.target === 'object' ? l.target.id : l.target;
            return selectedNodes.has(src) && selectedNodes.has(tgt);
        }).slice(0, this.MAX_LINKS);

        return {
            nodes: sortedNodes.slice(0, this.MAX_NODES),
            links: sampledLinks
        };
    }

    /* -------------------------------------------------------------
     * MAIN DRAWING
     * ------------------------------------------------------------- */

    /**
     * Main drawing entry point.
     * Parses JSON-LD, initializes simulation, and renders SVG elements.
     */
    async drawChart() {
        if (!this.width || !this.height) await this.updateSize();
        if (this.simulation) this.simulation.stop();

        const chartSvg = this.shadowRoot.querySelector("#chart-container");
        if (!chartSvg) return;

        this.svg = d3.select(chartSvg);
        this.svg.selectAll("*").remove();

        if (!this.jsonld) return;

        try {
            this.jsonLDOntology = JSON.parse(this.jsonld);
            const graph = await this.createGraph();
            this.graph = graph;

            // Initialize node positions
            this.graph.nodes.forEach(node => {
                node.x = this.width / 2 + (Math.random() - 0.5) * 400;
                node.y = this.height / 2 + (Math.random() - 0.5) * 400;
                node.vx = 0;
                node.vy = 0;
            });

            this.renderGraph();
        } catch (e) {
            console.error("Graph drawing error:", e);
        }
    }

    /**
     * Configures D3 forces, zoom, and appends visual elements.
     */
    renderGraph() {
        const self = this;

        // Force Simulation Configuration with adaptive strength for large graphs
        const nodeCount = this.graph.nodes.length;
        const isLargeGraph = nodeCount > 200;
        
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
            .on("zoom", () => gZoom.attr("transform", d3.event.transform));
        this.svg.call(this.zoomBehavior);

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
            // Helper to find node (D3 replaces IDs with objects, but initially they are IDs)
            const getSrc = (l) => l.source.id || l.source;
            const getTgt = (l) => l.target.id || l.target;

            const sourceNode = this.graph.nodes.find(n => n.id === getSrc(link));
            const targetNode = this.graph.nodes.find(n => n.id === getTgt(link));

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
                const getSrc = (l) => l.source.id || l.source;
                const getTgt = (l) => l.target.id || l.target;
                const sNode = this.graph.nodes.find(n => n.id === getSrc(d));
                const tNode = this.graph.nodes.find(n => n.id === getTgt(d));

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
                const getTgt = (l) => l.target.id || l.target;
                const tNode = this.graph.nodes.find(n => n.id === getTgt(d));
                if (tNode) {
                    return `url(#arrow-${this.sanitizeId(tNode.graph)})`;
                }
                return null;
            });

        // Draw Edge Labels (disable for large graphs for performance)
        const autoDisableLabels = this.graph.nodes.length > 300;
        if (autoDisableLabels && this.showEdgeLabels) {
            // Labels disabled automatically for performance
            this.showEdgeLabels = false;
        }
        
        this.linkLabelSelection = labelGroup.selectAll("text")
            .data(this.graph.links).enter().append("text")
            .attr("class", "edge-label")
            .attr("text-anchor", "middle")
            .text(d => this.formatLabel(d.name));

        if (!this.showEdgeLabels) {
            this.linkLabelSelection.style('display', 'none');
        }

        // Draw Nodes with Drag Behavior
        this.nodeSelection = nodeGroup.selectAll("g")
            .data(this.graph.nodes).enter().append("g")
            .call(d3.drag()
                .on("start", function (d) {
                    if (!d3.event.active) {
                        self.simulationStopped = false; // Re-enable ticking when dragging
                        self.simulation.alphaTarget(0.3).restart();
                    }
                    d.fx = d.x;
                    d.fy = d.y;
                })
                .on("drag", function (d) {
                    d.fx = d3.event.x;
                    d.fy = d3.event.y;
                })
                .on("end", function (d) {
                    if (!d3.event.active) {
                        self.simulation.alphaTarget(0);
                    }
                    d.fx = null;
                    d.fy = null;
                }));

        // Node Shapes
        this.nodeSelection.each(function (d) {
            const el = d3.select(this);
            const size = 20;
            const strokeColor = self.getGraphColor(d.graph);

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
        this.nodeSelection.append("text")
            .attr("class", "node-label")
            .attr("dy", 35)
            .attr("text-anchor", "middle")
            .text(d => this.truncateLabel(this.formatLabel(d.id)));

        // Tooltip Interaction
        // Note: #global-tooltip is outside shadow DOM
        const tooltip = d3.select("#global-tooltip");
        
        // Throttle function to limit mousemove frequency
        let tooltipMoveTimer = null;
        const throttleTooltip = (callback, delay = 16) => { // ~60fps
            return function(...args) {
                if (!tooltipMoveTimer) {
                    tooltipMoveTimer = setTimeout(() => {
                        callback.apply(this, args);
                        tooltipMoveTimer = null;
                    }, delay);
                }
            };
        };
        
        this.nodeSelection
            .on("mouseover", function(d) {
                const isLiteral = d.type === 'Literal';
                const title = isLiteral ? `"${self.formatLabel(d.id)}"` : self.formatLabel(d.id);
                let content = `<div class="tooltip-title">${title}</div>`;

                if (isLiteral) {
                    content += `<div class="tooltip-row"><strong>Value</strong> <span>"${d.id}"</span></div>`;
                    let typeVal = d.datatype ? self.formatLabel(d.datatype) : (isNaN(d.id) ? 'xsd:string' : 'xsd:decimal');
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

                if (d.graphs && d.graphs.size > 1) {
                    const graphList = Array.from(d.graphs).join(', ');
                    content += `<div class="tooltip-row"><strong>Graphs</strong> <span>${graphList}</span></div>`;
                    content += `<div class="tooltip-row"><strong>Primary</strong> <span>${d.graph || 'default'}</span></div>`;
                } else {
                    content += `<div class="tooltip-row"><strong>Graph</strong> <span>${d.graph || 'default'}</span></div>`;
                }

                tooltip.style("opacity", 1).html(content);
                
                // Attach mousemove only when hovering over node
                d3.select(this).on("mousemove", throttleTooltip(function() {
                    const evt = d3.event || window.event;
                    if (evt && evt.pageX !== undefined && evt.pageY !== undefined) {
                        tooltip
                            .style("left", (evt.pageX + 15) + "px")
                            .style("top", (evt.pageY - 10) + "px");
                    }
                }));
            })
            .on("mouseout", function() {
                tooltip.style("opacity", 0);
                // Remove mousemove listener when leaving node
                d3.select(this).on("mousemove", null);
                if (tooltipMoveTimer) {
                    clearTimeout(tooltipMoveTimer);
                    tooltipMoveTimer = null;
                }
            });

        // Start!
        this.simulation.alpha(1).restart();
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
    }

    /**
     * Animation tick function.
     * Updates SVG element positions based on simulation data.
     */
    ticked() {
        const hasPos = d => d && typeof d.x === 'number' && typeof d.y === 'number';
        const self = this;

        // Update Links
        if (this.linkSelection) {
            this.linkSelection.attr("d", d => {
                if (!hasPos(d.source) || !hasPos(d.target)) return null;
                return `M${d.source.x},${d.source.y} L${d.target.x},${d.target.y}`;
            });

            // Update Gradients (throttled for performance)
            // Only update every 3rd tick for large graphs
            const shouldUpdateGradients = this.graph.nodes.length < 200 || (this.lastTickTime % 48) < this.TICK_THROTTLE;
            if (shouldUpdateGradients) {
                this.linkSelection.each(function (d) {
                    if (d.gradientId && hasPos(d.source) && hasPos(d.target)) {
                        const gradient = self.svg.select(`#${d.gradientId}`);
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

        // Update Labels
        if (this.linkLabelSelection && this.showEdgeLabels) {
            this.linkLabelSelection
                .attr("x", d => (hasPos(d.source) && hasPos(d.target)) ? (d.source.x + d.target.x) / 2 : 0)
                .attr("y", d => (hasPos(d.source) && hasPos(d.target)) ? (d.source.y + d.target.y) / 2 : 0);
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
        try {
            const parts = uri.includes('#') ? uri.split('#') : uri.split('/');
            const last = parts.pop();
            return last || uri;
        } catch (e) {
            return uri;
        }
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