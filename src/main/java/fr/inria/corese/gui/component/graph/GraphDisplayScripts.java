package fr.inria.corese.gui.component.graph;

import fr.inria.corese.gui.core.theme.ThemeManager;

/**
 * Factory methods for JavaScript snippets executed by
 * {@link GraphDisplayWidget}.
 */
final class GraphDisplayScripts {

	private GraphDisplayScripts() {
		// Utility class
	}

	static String buildGraphInjectionScript(String base64Json, String requestId, String graphElementId) {
		String safeBase64Json = escapeForJsSingleQuoted(base64Json);
		String safeRequestId = escapeForJsSingleQuoted(requestId);
		String safeGraphElementId = escapeForJsSingleQuoted(graphElementId);
		return """
				(function() {
				  if (window.renderGraphFromBase64) {
				    window.renderGraphFromBase64('%s', '%s');
				    return;
				  }
				  try {
				    var el = document.getElementById('%s');
				    if (!el) throw new Error('Graph component not found');
				    var decoded = decodeURIComponent(escape(window.atob('%s')));
				    el.jsonld = decoded;
				    if (window.bridge && typeof window.bridge.onGraphRenderComplete === 'function') {
				      window.bridge.onGraphRenderComplete('%s');
				    }
				  } catch (e) {
				    if (window.bridge && typeof window.bridge.onGraphRenderFailed === 'function') {
				      window.bridge.onGraphRenderFailed('%s', String(e && e.message ? e.message : e));
				    }
				  }
				})();
				""".formatted(safeBase64Json, safeRequestId, safeGraphElementId, safeBase64Json, safeRequestId,
				safeRequestId);
	}

	static String buildGraphCommandScript(String graphElementId, String commandScript) {
		String safeGraphElementId = escapeForJsSingleQuoted(graphElementId);
		return "var el=document.getElementById('%s'); if(el){ %s }".formatted(safeGraphElementId, commandScript);
	}

	static String buildThemeScript(ThemeManager.WebThemeInfo webTheme) {
		return "if(window.setTheme) window.setTheme(%b, '%s', '%s');".formatted(webTheme.dark(),
				escapeForJsSingleQuoted(webTheme.accentHex()), escapeForJsSingleQuoted(webTheme.themeName()));
	}

	static String buildSvgExportScript(String graphElementId, String backgroundHex) {
		return """
				(function() {
				  var el = document.getElementById('%s');
				  if (!el) return null;
				  if (typeof el.exportSvg === 'function') { return el.exportSvg(); }
				  if (!el.shadowRoot) return null;
				  var svg = el.shadowRoot.querySelector('svg');
				  if (!svg) return null;
				  var clone = svg.cloneNode(true);
				  try {
				    var bbox = svg.getBBox();
				    var padding = 40;
				    var x = bbox.x - padding;
				    var y = bbox.y - padding;
				    var width = bbox.width + 2 * padding;
				    var height = bbox.height + 2 * padding;
				    clone.setAttribute('viewBox', x + ' ' + y + ' ' + width + ' ' + height);
				    clone.setAttribute('width', width);
				    clone.setAttribute('height', height);
				    var bg = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
				    bg.setAttribute('x', x);
				    bg.setAttribute('y', y);
				    bg.setAttribute('width', width);
				    bg.setAttribute('height', height);
				    bg.setAttribute('fill', '%s');
				    clone.insertBefore(bg, clone.firstChild);
				  } catch (e) {
				    console.warn('Could not adjust SVG bounds:', e);
				  }
				  var serializer = new XMLSerializer();
				  return serializer.serializeToString(clone);
				})();
				""".formatted(escapeForJsSingleQuoted(graphElementId), escapeForJsSingleQuoted(backgroundHex));
	}

	static String escapeForJsSingleQuoted(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'");
	}
}
