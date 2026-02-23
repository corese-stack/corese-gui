# Graph Viewer JS Integration Guide

This note summarizes the graph display stack and the files to export if you want to embed the Corese graph view in a pure JS app.

## 1) Quick audit (current architecture)

Graph display is split into two layers:

- Java wrapper layer (JavaFX):
  - `src/main/java/fr/inria/corese/gui/component/graph/GraphDisplayWidget.java`
  - `src/main/java/fr/inria/corese/gui/component/graph/GraphDisplayScripts.java`
  - `src/main/java/fr/inria/corese/gui/component/graph/GraphBridgeParsing.java`
- Web layer (standalone viewer, D3 + custom element):
  - `src/main/resources/graph-viewer/graph-viewer.html`
  - `src/main/resources/graph-viewer/lib/d3.min.js`
  - `src/main/resources/graph-viewer/js/kg-graph.js`
  - `src/main/resources/graph-viewer/js/app.js`
  - `src/main/resources/graph-viewer/css/theme.css`

The reusable part for external JS integration is the **web layer**.

## 2) Files to export for your friend (JS app)

Mandatory:

- `src/main/resources/graph-viewer/graph-viewer.html`
- `src/main/resources/graph-viewer/lib/d3.min.js`
- `src/main/resources/graph-viewer/js/kg-graph.js`
- `src/main/resources/graph-viewer/js/app.js`
- `src/main/resources/graph-viewer/css/theme.css`

Not needed in a pure JS app:

- `src/main/java/fr/inria/corese/gui/component/graph/GraphDisplayWidget.java`
- `src/main/java/fr/inria/corese/gui/component/graph/GraphDisplayScripts.java`
- `src/main/java/fr/inria/corese/gui/component/graph/GraphBridgeParsing.java`
- `src/main/java/fr/inria/corese/gui/core/service/GraphProjectionService.java`
- JavaFX CSS (`src/main/resources/css/components/graph-display-widget.css`, etc.)

## 3) Minimal embed contract

Your host page must include:

- one `<kg-graph>` element (default id: `myGraph`)
- one global tooltip element: `<div id="global-tooltip" class="kg-tooltip"></div>`
- scripts loaded in this order: D3 -> `kg-graph.js` -> `app.js`

## 4) Public JS API

Available global functions from `app.js`:

- `window.renderGraphFromJson(jsonLdString, requestId?, graphElementId?)`
- `window.renderGraphFromBase64(base64Json, requestId?)`
- `window.setTheme(isDark, accentColor?, themeName?)`
- `window.applyTheme(isDark)` (legacy alias)
- `window.getGraphElement(elementId?)`

`requestId` is optional in non-Java usage.

## 5) Minimal usage example

```html
<link rel="stylesheet" href="css/theme.css" />
<script src="lib/d3.min.js"></script>
<script src="js/kg-graph.js"></script>
<script src="js/app.js"></script>

<div id="container">
  <kg-graph id="myGraph"></kg-graph>
</div>
<div id="global-tooltip" class="kg-tooltip"></div>

<script>
  window.renderGraphFromJson(jsonLdPayload, "demo-1");
  window.setTheme(false, "#1f6feb", "primer");
</script>
```

## 6) Notes for integration

- Java bridge callbacks are optional; without `window.bridge`, viewer still works.
- For custom element id, pass it in `renderGraphFromJson(..., ..., "yourGraphId")`.
- Input payload must be valid JSON-LD JSON string.
