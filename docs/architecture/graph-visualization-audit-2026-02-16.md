# Graph Visualization Audit & Refactor Report (2026-02-16)

## Scope

This audit covers the graph visualization stack used in Data and Graph Result views:

- `src/main/resources/graph-viewer/js/kg-graph.js`
- `src/main/resources/graph-viewer/js/app.js`
- `src/main/resources/graph-viewer/css/theme.css`
- `src/main/java/fr/inria/corese/gui/component/graph/GraphDisplayWidget.java`
- `src/main/java/fr/inria/corese/gui/feature/data/DataView.java`
- `src/main/java/fr/inria/corese/gui/feature/data/DataViewController.java`
- `src/main/java/fr/inria/corese/gui/feature/result/graph/GraphResultView.java`
- `src/main/java/fr/inria/corese/gui/feature/result/graph/GraphResultController.java`
- `src/main/java/fr/inria/corese/gui/core/service/GraphProjectionService.java`
- `src/main/java/fr/inria/corese/gui/core/service/DefaultReasoningService.java`

## Architecture Snapshot

### Rendering flow

1. JavaFX side computes a JSON-LD snapshot (`GraphProjectionService`).
2. `GraphDisplayWidget` injects this payload into WebView (`kg-graph` web component).
3. `kg-graph.js` parses JSON-LD, builds nodes/links, computes geometry, runs D3 forces, and renders.
4. Graph stats are bridged back to Java for status bars/tooltips.

### Current behavior model

- Named graph coloring is deterministic.
- Built-in reasoning profiles use dedicated preset colors.
- Node primary graph attribution is deterministic and prefers named graphs over default when relevant.
- Legends are rendered by the shared graph widget (global components + named graphs).
- Graph status bars are owned by Java views (Data and Result) and fed by widget stats.

## Main Risks Identified

### High

1. `kg-graph.js` remains very large (2k+ LOC) and centralizes many concerns.
   - Layout, geometry, rendering, overlays, labels, theme, and bridge callbacks are co-located.
   - Risk: regression probability increases for each iteration.

2. Self-loop / cycle readability is sensitive to parameter tuning.
   - Geometry is now improved, but still highly dependent on dense local neighborhoods.
   - Risk: minor changes can reintroduce overlap artifacts.

3. Bridging JS<->Java uses string-built scripts in several places.
   - Refactor started (text blocks and constants), but this remains fragile by nature.
   - Risk: escaping or payload edge cases.

### Medium

4. JS visualization logic has no dedicated automated unit tests.
   - Existing tests focus mainly on Java services and JSON-LD projection invariants.
   - Risk: visual regressions are caught manually only.

5. Overlay/legend/status coherence is split between JS and Java responsibilities.
   - Model is now cleaner than before, but still requires discipline.
   - Risk: drift across views when evolving one side only.

## Refactor & Cleanup Applied

### 1) Naming and readability

- Introduced dedicated built-in profile color presets in `kg-graph.js`.
- Added explicit helpers for graph summary handling:
  - `createEmptyGraphSummary()`
  - `resolveGraphSummary(summary)`
  - `formatLegendCount(value)`

Impact:
- Removed duplicated inline summary object literals.
- Reduced null/shape inconsistencies in legend/status rendering.

### 2) Legend factorization

- Split overlay rendering logic into dedicated methods:
  - `renderGlobalLegend(componentCounts)`
  - `renderNamedGraphLegend(namedGraphs)`

Impact:
- `refreshOverlayPanels()` now orchestrates instead of containing all HTML generation.
- Easier to evolve global/named legends independently.

### 3) Component counters in global legend

- Added per-component counts in the Components legend:
  - Resource
  - Literal
  - Blank Node
  - Predicate Link

Impact:
- Better parity with Named Graph legend counters.
- Faster visual diagnostics for graph composition.

### 4) Self-loop/cycle geometry cleanup

- Reworked self-loop geometry toward elongated tear-drop shapes.
- Improved multi-loop spacing and arrow endpoint separation.
- Moved loop labels farther from nodes and added directional text anchoring.

Impact:
- Reduced overlap around nodes with multiple cycles.
- Better readability in OWL/RDFS dense inferred subgraphs.

### 5) `GraphDisplayWidget` maintainability improvements

- Centralized web component id constant (`GRAPH_ELEMENT_ID`).
- Replaced large string concatenation scripts with text blocks in:
  - `buildGraphInjectionScript(...)`
  - `getSvgContent()` script generation
- Added a shared helper for graph commands:
  - `executeGraphCommand(...)`

Impact:
- More readable script generation.
- Lower risk when evolving command calls (`reset`, `zoom`, `recenter`, `clear`).

## Validation Status

- JS syntax check:
  - `node --check src/main/resources/graph-viewer/js/kg-graph.js` passes.
- Service-level tests continue to pass in this iteration (reasoning/projection suites).

## Remaining Work (Prioritized)

### P1 (next iteration)

1. Extract `kg-graph.js` internals into logical modules/files:
   - geometry helpers
   - summary/legend helpers
   - simulation/layout helpers
   - rendering layer builders

2. Add JS-level test harness for geometry invariants:
   - self-loop label distance from node center
   - non-crossing constraints for 2+ loops on same node
   - deterministic color mapping invariants

### P2

3. Harden JS bridge payload contract with typed schema validation on Java side.
4. Add snapshot regression fixtures for representative datasets:
   - no default graph + named inferences
   - dense OWL cycles
   - large disconnected components

### P3

5. Continue CSS cleanup for cross-view visual coherence (Data/Result/Template).

## Suggested Commit Plan

1. `refactor(graph-viewer): factorize summary/legend rendering and component counters`
2. `refactor(graph-widget): clean JS bridge script generation and graph commands`
3. `docs(graph): refresh audit report and prioritized technical backlog`

