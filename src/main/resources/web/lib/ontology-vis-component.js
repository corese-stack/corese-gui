class KGGraphVis extends HTMLElement {

    constructor() {    
        super()
        // Only attach shadow once
        this.attachShadow({ mode: "open" });

        this.svg = null;
        
        this.simulation = null;
        this.defaultCharge = -50;
        this.defaultDistance = 10;

        // To organize private attributes (e.g. query, endpoint)
        this.internalData = new WeakMap()
        this.internalData.set(this, {}); // Initialize internal storage
       
    }

    set ttl(ttl) {
        const data = this.internalData.get(this) || {};
        data.ttl = ttl;
        this.internalData.set(this, data);
        this.drawChart()
    }
    
    get ttl() {
        return this.internalData.get(this)?.ttl;
    }

    async connectedCallback() {
        this.render();

        this.width = await this.computePixelValue('width')
        this.height = await this.computePixelValue('height')

        this.svg = d3.select(this.shadowRoot.querySelector("#chart-container"))
            .attr('width', this.width)
            .attr('height', this.height)

        const defs = this.svg.append("defs");

        defs.append("marker")
            .attr("id", "arrowhead")
            .attr("viewBox", "0 -5 10 10")
            .attr("refX", 15)         // Adjust to move arrowhead closer/farther to node
            .attr("refY", 0)
            .attr("markerWidth", 6)   // Size of the arrowhead
            .attr("markerHeight", 6)
            .attr("orient", "auto")
            .attr("fill", "#999")
            .append("path")
            .attr("d", "M0,-5L10,0L0,5");  // Triangle shape
    }

    render() {
        this.shadowRoot.innerHTML = `
          <style>
            svg {
              border: 1px solid #ccc;
            }
          </style>
          <div style="width=100%; height:100%;">
            <svg id="chart-container"></svg>
          </div>
        `;
    }

    async computePixelValue(attr) {
        const sizeStr = this.getAttribute(attr);
        let pixelValue = null;
      
        if (!sizeStr) return null;
      
        if (sizeStr.endsWith('vw')) {
          const vw = parseFloat(sizeStr);
          pixelValue = (vw / 100) * window.innerWidth;
      
        } else if (sizeStr.endsWith('vh')) {
          const vh = parseFloat(sizeStr);
          pixelValue = (vh / 100) * window.innerHeight;
          pixelValue *= 0.85;
      
        } else if (sizeStr.endsWith('px')) {
          pixelValue = parseFloat(sizeStr);
      
        } else if (sizeStr.endsWith('%')) {
          const percent = parseFloat(sizeStr);
          // get parent element to compute relative size
          const parent = this.parentElement;
          if (parent) {
            const parentRect = parent.getBoundingClientRect();
            // you can choose width or height here depending on context
            // let's assume width for example:
            pixelValue = (percent / 100) * parentRect.width;
          } else {
            pixelValue = null;
          }
      
        } else {
          pixelValue = null;
        }
      
        return pixelValue;
    }
      

    async drawChart() {

        console.log("Loading ontology:", this.ttl);
        this.jsonLDOntology = ttl2jsonld.parse(this.ttl);
        console.log("Parsed JSON-LD Ontology:", this.jsonLDOntology);
    
        let graph = await this.createGraph();
        this.graph = await this.duplicateTargetsForMultipleLinks(graph);

        console.log(this.graph)
        await this.layoutGraphWithHierarchy(this.graph);

        this.renderGraph();
        this.setControls();
    }

    async createGraph() {
        let graph = {
            nodes: [],
            links: []
        };

        const definedNodeIds = new Set();

        this.jsonLDOntology['@graph'].forEach((item) => {
            if (item['@type'] === 'rdfs:Class') {
                graph.nodes.push({
                    id: item['@id'],
                    name: item['@id'],
                    type: 'class'
                })
                definedNodeIds.add(item['@id']);
            } 
        });

        // Add subClassOf links
        this.jsonLDOntology['@graph'].forEach((item) => {
            if (item['@type'] === 'rdfs:Class' && item['rdfs:subClassOf']) {
                const sourceId = item['@id'];
                const subclasses = Array.isArray(item['rdfs:subClassOf'])
                    ? item['rdfs:subClassOf']
                    : [item['rdfs:subClassOf']];

                subclasses.forEach(sub => {
                    const targetId = sub['@id'];

                    graph.links.push({
                        source: sourceId,
                        target: targetId,
                        name: 'rdfs:subClassOf'
                    });

                    if (!definedNodeIds.has(targetId)) {
                        graph.nodes.push({
                            id: targetId,
                            name: targetId,
                            type: 'external'
                        });
                        definedNodeIds.add(targetId);
                    }
                });
            }
        });

        this.jsonLDOntology['@graph'].forEach((item) => {
            if (item['@type'] === 'rdf:Property') {
                const domain = item['rdfs:domain'];
                const range = item['rdfs:range'];

                if (domain && range) {
                    const domainId = domain['@id'];
                    const rangeId = range['@id'];

                    graph.links.push({
                        source: domainId,
                        target: rangeId,
                        name: item['@id'],
                    });

                    // Add domain node if not defined
                    if (!definedNodeIds.has(domainId)) {
                        graph.nodes.push({
                            id: domainId,
                            name: domainId,
                            type: 'external'  // or 'class' / 'undefined'
                        });
                        definedNodeIds.add(domainId);
                    }

                    // Add range node if not defined
                    if (!definedNodeIds.has(rangeId)) {
                        graph.nodes.push({
                            id: rangeId,
                            name: rangeId,
                            type: 'external'
                        });
                        definedNodeIds.add(rangeId);
                    }
                }
            }
        });

        return graph;
    }

    async duplicateTargetsForMultipleLinks(graph) {
        const sourceTargetCount = {};
    
        graph.links.forEach(link => {
            const key = `${link.source}->${link.target}`;
            sourceTargetCount[key] = (sourceTargetCount[key] || 0) + 1;
        });
    
        const newNodes = [...graph.nodes];
        const newLinks = [];
        const copyIndex = {};
    
        graph.links.forEach(link => {
            const key = `${link.source}->${link.target}`;
    
            if (sourceTargetCount[key] > 1) {
                copyIndex[key] = (copyIndex[key] || 0) + 1;
    
                // Only duplicate beyond the first link
                if (copyIndex[key] === 1) {
                    newLinks.push(link); // First one uses original
                } else {
                    const originalTarget = graph.nodes.find(n => n.id === link.target);
                    const newTargetId = `${link.target}_copy${copyIndex[key]}`;
                    const newTarget = {
                        ...originalTarget,
                        id: newTargetId
                    };
    
                    newNodes.push(newTarget);
                    newLinks.push({
                        ...link,
                        target: newTargetId
                    });
                }
            } else {
                newLinks.push(link); // Only one link, keep as is
            }
        });
    
        return {
            nodes: newNodes,
            links: newLinks
        };
    }
    
    /**
     * Step 1: Build hierarchical tree(s) from subClassOf links
     */
    async buildHierarchy(graph) {
        const subclassLinks = graph.links.filter(l => l.name === 'rdfs:subClassOf');

        const childrenByParent = {};
        const allIds = new Set();

        subclassLinks.forEach(link => {
            if (!childrenByParent[link.target]) {
                childrenByParent[link.target] = [];
            }
            childrenByParent[link.target].push(link.source);
            allIds.add(link.source);
            allIds.add(link.target);
        });

        const childIds = new Set(subclassLinks.map(l => l.source));
        const rootIds = [...allIds].filter(id => !childIds.has(id));

        function buildNode(id) {
            return {
                id,
                children: (childrenByParent[id] || []).map(buildNode)
            };
        }

        return rootIds.map(buildNode);
    }

    
    /**
     * Step 2: Apply layout using tree for subclass hierarchy, and light positioning for others
     */
    async layoutGraphWithHierarchy(graph) {
        // const hierarchyRoots = await this.buildHierarchy(graph);
        const hierarchyRoots = []

        const tree = d3.tree().nodeSize([70, 200]); // x, y spacing
        const positionedNodes = new Map();

        // Apply tree layout for each root
        hierarchyRoots.forEach((root, index) => {
            const rootHierarchy = d3.hierarchy(root, d => d.children);
            tree(rootHierarchy);

            // Offset each tree horizontally if there are multiple roots
            const xOffset = index * 400;

            rootHierarchy.descendants().forEach(d => {
                positionedNodes.set(d.data.id, {
                    x: d.y + xOffset,
                    y: d.x
                });
            });
        });

        // Assign positions to hierarchy nodes
        graph.nodes.forEach(node => {
            if (positionedNodes.has(node.id)) {
                const pos = positionedNodes.get(node.id);
                node.fx = pos.x;
                node.fy = pos.y;
            }
        });

        // Place the remaining nodes (non-subClassOf) based on their links
        graph.nodes.forEach(node => {
            if (!node.x && !node.y) {
                // Try to find linked nodes that have a fixed position
                const linked = graph.links.filter(l => l.source === node.id || l.target === node.id);
                const connected = linked.map(l =>
                    l.source === node.id ? l.target : l.source
                ).filter(id => positionedNodes.has(id));

                if (connected.length > 0) {
                    const avgX = d3.mean(connected.map(id => positionedNodes.get(id).x));
                    const avgY = d3.mean(connected.map(id => positionedNodes.get(id).y));
                    node.x = avgX + (Math.random() - 0.5) * 50;
                    node.y = avgY + 100 + (Math.random() - 0.5) * 30;
                } else {
                    node.x = Math.random() * 800;
                    node.y = 800;
                }
            }
        });
    } 
    
    setupSimulation() {
        // Fix hierarchical positions
        this.graph.nodes.forEach(node => {
            if (node.fx !== undefined && node.fy !== undefined) {
                // Node already fixed by tree layout
            } else {
                // Leave free for simulation
                delete node.fx;
                delete node.fy;
            }
        });
    
        this.simulation = d3.forceSimulation(this.graph.nodes)
            .force("link", d3.forceLink(this.graph.links)
                .id(d => d.id)
                .distance(d => {
                    // Allow longer distance for links to/from tree nodes
                    const isTreeLink = d.source.fx !== undefined || d.target.fx !== undefined;
                    return isTreeLink ? 120 : 60;
                })
                .strength(0.7)
            )
            .force("charge", d3.forceManyBody().strength(-100))
            .force("center", d3.forceCenter(this.width / 2, this.height / 2))
            .force("collision", d3.forceCollide().radius(40))
            //.on("tick", () => this.updatePositions());
    
        // Optional: unfix tree nodes after stabilization
        this.simulation.on('end', () => {
            this.graph.nodes.forEach(n => {
                if (n.fx !== undefined) delete n.fx;
                if (n.fy !== undefined) delete n.fy;
            });
        });
    }

    // Define the updatePositions method somewhere in your class or module:
    updatePositions() {
        this.link
            .attr("x1", d => d.source.x)
            .attr("y1", d => d.source.y)
            .attr("x2", d => d.target.x)
            .attr("y2", d => d.target.y)
            .attr("d", d => `M${d.source.x},${d.source.y} L${d.target.x},${d.target.y}`);
    
        this.nodeGroup
            .attr("transform", d => `translate(${d.x},${d.y})`);
    
        this.linkLabel
            .attr("transform", (d, i) => {
                const path = document.querySelector(`#linkPath${i}`);
                if (!path) return null;
            
                const pathLength = path.getTotalLength();
                const midPoint = path.getPointAtLength(pathLength / 2);
            
                const start = path.getPointAtLength(0);
                const end = path.getPointAtLength(pathLength);
            
                const angle = Math.atan2(end.y - start.y, end.x - start.x) * (180 / Math.PI);
            
                if (angle > 90 || angle < -90) {
                    // Rotate 180 degrees around midpoint of the path
                    return `translate(${midPoint.x},${midPoint.y}) rotate(180) translate(${-midPoint.x},${-midPoint.y})`;
                } else {
                    return null;
                }
            });
    }
    

    renderGraph() {
        // Measure link label widths and set a custom distance
        const tempText = this.svg.append("text").attr("font-size", "10px").attr("opacity", 0);
        this.graph.links.forEach(link => {
            tempText.text(link.name);
            const bbox = tempText.node().getBBox();
            link._labelWidth = bbox.width;
        });
        tempText.remove();

        this.setupSimulation();
        
        // Create a group that will be zoomed
        const gZoom = this.svg.append("g").attr("class", "zoom-layer");

        // Define zoom behavior
        const zoom = d3.zoom()
            .scaleExtent([0.1, 5])
            .on("zoom", () => {
                gZoom.attr("transform", d3.event.transform);
            });

        // Apply zoom to svg
        this.svg.call(zoom);

        this.link = gZoom.append("g")
            .attr("class", "links")
            .selectAll("path")
            .data(this.graph.links)
            .enter()
            .append("path")
            .attr("id", (d, i) => `linkPath${i}`)
            .attr("stroke-width", 2)
            .attr("stroke", "#999")
            .attr("fill", "none")
            .attr("marker-end", "url(#arrowhead)")  // <-- add this line
            .attr("method", "align")


        this.linkLabel = gZoom.append("g")
            .attr("class", "link-labels")
            .selectAll("text")
            .data(this.graph.links)
            .enter()
            .append("text")
            .attr("font-size", "10px")
            .attr("fill", "#555")
            .attr("text-anchor", "middle")
            // .attr("dominant-baseline", "middle")
            .append("textPath")
            .attr("xlink:href", (d, i) => `#linkPath${i}`)
            .attr("startOffset", "50%")
            .text(d => d.name)
    
        this.nodeGroup = gZoom.append("g")
            .attr("class", "nodes")
            .selectAll("g")
            .data(this.graph.nodes)
            .enter()
            .append("g")
            .style('cursor', 'grab')
            .call(d3.drag()
                .on("start", function(d) {
                    // If node is fixed, skip drag
                    if (d.fx !== null && d.fy !== null && d.fx !== undefined && d.fy !== undefined) {
                    d._skipDrag = true;  // mark to skip other drag events
                    return;
                    }
                    d._skipDrag = false;

                    if (!d3.event.active) this.simulation.alphaTarget(0.3).restart();

                    d._wasFixed = false; // was not fixed before drag

                    d.fx = d.x;
                    d.fy = d.y;
                }.bind(this))
                .on("drag", function(d) {
                    if (d._skipDrag) return;  // skip dragging fixed nodes

                    d.fx = d3.event.x;
                    d.fy = d3.event.y;
                })
                .on("end", function(d) {
                    if (d._skipDrag) return; // skip

                    if (!d3.event.active) this.simulation.alphaTarget(0);

                    d.fx = null;
                    d.fy = null;
                    delete d._wasFixed;
                }.bind(this))
            )
        
        const colors = d3.scaleOrdinal(d3.schemeCategory10)
        this.nodeGroup.each(function(d) {
            const group = d3.select(this);
            const text = group.append("text")
                .text(d.name)
                .attr("font-size", "10px")
                .attr("text-anchor", "middle")
                .attr("dominant-baseline", "middle");
        
            // After text is appended, measure its width
            const textNode = text.node();
            const bbox = textNode.getBBox();
            const paddingX = 10;
            const paddingY = 6;
        
            group.insert("rect", "text")
                .attr("x", -bbox.width / 2 - paddingX / 2)
                .attr("y", -bbox.height / 2 - paddingY / 2)
                .attr("width", bbox.width + paddingX)
                .attr("height", bbox.height + paddingY)
                .attr("fill", d => colors(d.type))
                .attr("rx", 4) // optional rounded corners
                .attr("ry", 4);
        });
        
    
        this.simulation
            .nodes(this.graph.nodes)
            .on("tick", () => this.updatePositions())
    
        this.simulation.force("link").links(this.graph.links);

    }

    
    
    
    setControls() {
        // Add UI Sliders inside SVG
        const sliderGroup = this.svg.append("g").attr("class", "sliders");

        // Constants for layout
        const sliderWidth = 120;
        const sliderX = 20;
        let sliderY = this.height - 100;

        const createSlider = (label, min, max, initial, step, onChange) => {
            const group = sliderGroup.append("g")
                .attr("transform", `translate(${sliderX}, ${sliderY})`);
            
            // Label
            group.append("text")
                .text(`${label}: ${initial}`)
                .attr("x", 0)
                .attr("y", -10)
                .attr("font-size", "10px")
                .attr("class", `label-${label}`);

            // Track
            group.append("line")
                .attr("x1", 0)
                .attr("x2", sliderWidth)
                .attr("y1", 0)
                .attr("y2", 0)
                .attr("stroke", "#ccc")
                .attr("stroke-width", 2);

            // Handle
            const handle = group.append("circle")
                .attr("cx", ((initial - min) / (max - min)) * sliderWidth)
                .attr("cy", 0)
                .attr("r", 6)
                .attr("fill", "#888")
                .style("cursor", "pointer")
                .call(d3.drag().on("drag", () => {
                    let x = Math.max(0, Math.min(sliderWidth, d3.event.x));
                    handle.attr("cx", x);

                    const value = min + (x / sliderWidth) * (max - min);
                    group.select(`text.label-${label}`)
                        .text(`${label}: ${Math.round(value)}`);

                    onChange(value);
                }));

            sliderY += 40; // Space for next slider
        };

        // Charge slider
        createSlider("Charge", -300, -10, -50, 1, val => {
            this.simulation.force("charge").strength(val);
            this.simulation.alpha(1).restart();
        });

        // Distance slider
        createSlider("Distance", 10, 200, 10, 1, val => {
            this.simulation.force("link").distance(val);
            this.simulation.alpha(1).restart();
        });


        // Reset button (SVG rectangle + text)
        const buttonGroup = sliderGroup.append("g")
            .attr("transform", `translate(${sliderX}, ${sliderY - 15})`)
            .style("cursor", "pointer")
            .on("click", () => {
                let _this = this;
                // Reset simulation forces
                this.simulation.force("charge").strength(this.defaultCharge);
                this.simulation.force("link").distance(this.defaultDistance);
                this.simulation.alpha(1).restart();

                // Reset slider labels
                sliderGroup.select("text.label-Charge").text(`Charge: ${this.defaultCharge}`);
                sliderGroup.select("text.label-Distance").text(`Distance: ${this.defaultDistance}`);

                // Reset handles
                sliderGroup.selectAll("g").each(function() {
                    const text = d3.select(this).select("text").text();
                    if (text.startsWith("Charge")) {
                        d3.select(this).select("circle")
                            .attr("cx", ((_this.defaultCharge - (-300)) / (290)) * sliderWidth);
                    } else if (text.startsWith("Distance")) {
                        d3.select(this).select("circle")
                            .attr("cx", ((_this.defaultDistance - 10) / 190) * sliderWidth);
                    }
                })
            })

        buttonGroup.append("rect")
            .attr("width", 60)
            .attr("height", 20)
            .attr("rx", 4)
            .attr("ry", 4)
            .attr("fill", "#eee")
            .attr("stroke", "#aaa")

        buttonGroup.append("text")
            .text("Reset")
            .attr("x", 30)
            .attr("y", 14)
            .attr("text-anchor", "middle")
            .attr("font-size", "10px")
    }

}

customElements.define('kg-graph', KGGraphVis);