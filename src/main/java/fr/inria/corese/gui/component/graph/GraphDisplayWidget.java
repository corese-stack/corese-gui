package fr.inria.corese.gui.component.graph;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.theme.AppThemeRegistry;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.theme.ThemeManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.util.Duration;

/**
 * A reusable JavaFX widget for visualizing RDF graphs.
 *
 * <p>
 * This widget provides:
 *
 * <ul>
 * <li>WebView-based graph rendering using D3.js and kg-graph web component
 * <li>JSON-LD data injection and visualization
 * <li>Interactive graph manipulation (zoom, reset)
 * <li>Automatic theme synchronization
 * <li>Lazy loading - waits for scene attachment before initialization
 * </ul>
 *
 * <p>
 * <b>Usage example:</b>
 *
 * <pre>{@code
 * GraphDisplayWidget widget = new GraphDisplayWidget();
 * widget.displayGraph(jsonLdData);
 * widget.resetLayout();
 * }</pre>
 */
public class GraphDisplayWidget extends VBox {

	// ==============================================================================================
	// Constants
	// ==============================================================================================

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphDisplayWidget.class);
	private static final String COMMON_STYLESHEET = "/css/common/common.css";
	private static final String STYLESHEET = "/css/components/graph-display-widget.css";
	private static final String GRAPH_HTML_PATH = "/graph-viewer/graph-viewer.html";
	private static final String STYLE_CLASS_CONTAINER = "graph-display-container";
	private static final String STYLE_CLASS_WEBVIEW = "graph-display-webview";
	private static final String STYLE_CLASS_LOADING_MASK = "graph-loading-mask";
	private static final String STYLE_CLASS_SAFETY_OVERLAY = "graph-safety-overlay";
	private static final String STYLE_CLASS_SAFETY_CARD = "graph-safety-card";
	private static final String STYLE_CLASS_SAFETY_TITLE = "graph-safety-title";
	private static final String STYLE_CLASS_SAFETY_MESSAGE = "graph-safety-message";
	private static final String STYLE_CLASS_SAFETY_ACTION = "graph-safety-action";
	private static final String STYLE_CLASS_SAFETY_ACTIONS = "graph-safety-actions";
	private static final int MASK_FADE_MS = 140;
	private static final int DEFAULT_MAX_AUTO_RENDER_CHARS = 1_500_000;
	private static final String SAFETY_TITLE = "Graph preview paused";
	private static final String SAFETY_ACTION = "Display anyway";
	private static final String SAFETY_HINT = "The graph is large and automatic rendering is disabled to keep the application responsive.";

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private final WebView webView;
	private final WebEngine webEngine;
	private final StackPane container;
	private final Region loadingMask;
	private final StackPane safetyOverlay;
	private final Label safetyMessageLabel;
	private final Button safetyActionButton;
	private final JavaBridge bridge = new JavaBridge();

	private boolean pageLoaded = false;
	private String pendingJsonLdData = null;
	private String blockedJsonLdData = null;
	private int maxAutoRenderChars = DEFAULT_MAX_AUTO_RENDER_CHARS;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/**
	 * Constructs a new GraphDisplayWidget. The HTML page is loaded when attached to
	 * a scene.
	 */
	public GraphDisplayWidget() {
		this.webView = new WebView();
		this.webEngine = webView.getEngine();
		this.container = new StackPane();
		this.loadingMask = new Region();
		this.safetyMessageLabel = new Label();
		this.safetyActionButton = new Button(SAFETY_ACTION);
		this.safetyOverlay = createSafetyOverlay();

		initializeLayout();
		initializeListeners();

		// Wait for the widget to be attached to a scene before loading
		sceneProperty().addListener((obs, oldScene, newScene) -> {
			Worker.State currentState = webEngine.getLoadWorker().getState();
			if (newScene != null && !pageLoaded && blockedJsonLdData == null && currentState != Worker.State.RUNNING
					&& currentState != Worker.State.SUCCEEDED) {
				Platform.runLater(this::loadGraphPage);
			}
		});
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private void initializeLayout() {
		CssUtils.applyViewStyles(this, COMMON_STYLESHEET);
		CssUtils.applyViewStyles(this, STYLESHEET);

		webView.setContextMenuEnabled(false);
		webView.setPrefWidth(Region.USE_COMPUTED_SIZE);
		webView.setPrefHeight(Region.USE_COMPUTED_SIZE);
		webView.setMinHeight(0);
		webView.setMinWidth(0);

		container.getStyleClass().add(STYLE_CLASS_CONTAINER);
		webView.getStyleClass().add(STYLE_CLASS_WEBVIEW);
		loadingMask.getStyleClass().add(STYLE_CLASS_LOADING_MASK);
		loadingMask.setOpacity(1);
		loadingMask.setVisible(true);
		loadingMask.setMouseTransparent(true);

		container.getChildren().addAll(webView, loadingMask, safetyOverlay);

		setVgrow(container, Priority.ALWAYS);
		getChildren().add(container);
	}

	private StackPane createSafetyOverlay() {
		Label titleLabel = new Label(SAFETY_TITLE);
		titleLabel.getStyleClass().add(STYLE_CLASS_SAFETY_TITLE);

		safetyMessageLabel.getStyleClass().add(STYLE_CLASS_SAFETY_MESSAGE);
		safetyMessageLabel.setWrapText(true);

		safetyActionButton.getStyleClass().addAll(STYLE_CLASS_SAFETY_ACTION, Styles.ACCENT);
		safetyActionButton.setOnAction(event -> renderBlockedGraph());
		HBox actions = new HBox(safetyActionButton);
		actions.getStyleClass().add(STYLE_CLASS_SAFETY_ACTIONS);
		actions.setAlignment(Pos.CENTER_RIGHT);

		VBox card = new VBox(10, titleLabel, safetyMessageLabel, actions);
		card.getStyleClass().addAll(STYLE_CLASS_SAFETY_CARD, "floating-panel");
		card.setPrefHeight(Region.USE_COMPUTED_SIZE);
		card.setMinHeight(Region.USE_PREF_SIZE);
		card.setMaxHeight(Region.USE_PREF_SIZE);
		card.setMaxWidth(Region.USE_PREF_SIZE);

		StackPane overlay = new StackPane(card);
		overlay.getStyleClass().add(STYLE_CLASS_SAFETY_OVERLAY);
		overlay.setAlignment(Pos.CENTER);
		overlay.setVisible(false);
		overlay.setManaged(false);
		return overlay;
	}

	private void initializeListeners() {
		// Capture JavaScript alerts for debugging
		webEngine.setOnAlert(event -> LOGGER.debug("[JS Alert] {}", event.getData()));

		webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
			if (newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED) {
				pageLoaded = false;
				showLoadingMask();
			} else if (newState == Worker.State.SUCCEEDED) {
				onPageLoaded();
			} else if (newState == Worker.State.FAILED) {
				LOGGER.error("Failed to load graph visualization");
				pageLoaded = false;
				showLoadingMask();
			}
		});

		webEngine.getLoadWorker().exceptionProperty().addListener((obs, old, newEx) -> {
			if (newEx != null) {
				LOGGER.error("WebView load error", newEx);
			}
		});

		ThemeManager.getInstance().themeProperty()
				.addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));

		ThemeManager.getInstance().accentColorProperty()
				.addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));
	}

	@SuppressWarnings("removal")
	private void onPageLoaded() {
		try {
			// Inject Java Bridge into JavaScript
			// Using deprecated JSObject as there's no official alternative yet
			JSObject window = (JSObject) webEngine.executeScript("window");
			window.setMember("bridge", bridge);

			pageLoaded = true;
			updateTheme();

			webEngine.executeScript("if(window.setupBridge) window.setupBridge();");

			if (pendingJsonLdData != null) {
				displayGraph(pendingJsonLdData);
				pendingJsonLdData = null;
			}
			hideLoadingMask();
		} catch (Exception e) {
			LOGGER.error("Error during graph page initialization", e);
		}
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Displays an RDF graph from JSON-LD formatted data.
	 *
	 * @param jsonLdData
	 *            The RDF data in JSON-LD format (null or empty clears the view)
	 */
	public void displayGraph(String jsonLdData) {
		if (jsonLdData == null || jsonLdData.isBlank()) {
			clear();
			return;
		}

		if (shouldDeferAutomaticRender(jsonLdData)) {
			deferGraphRendering(jsonLdData);
			return;
		}

		renderGraph(jsonLdData);
	}

	private void renderGraph(String jsonLdData) {
		hideSafetyOverlay();
		blockedJsonLdData = null;

		if (!pageLoaded) {
			pendingJsonLdData = jsonLdData;
			if (webEngine.getLoadWorker().getState() != Worker.State.RUNNING) {
				loadGraphPage();
			}
			return;
		}

		Platform.runLater(() -> injectGraphData(jsonLdData));
	}

	private boolean shouldDeferAutomaticRender(String jsonLdData) {
		return maxAutoRenderChars > 0 && jsonLdData.length() > maxAutoRenderChars;
	}

	private void deferGraphRendering(String jsonLdData) {
		blockedJsonLdData = jsonLdData;
		pendingJsonLdData = null;
		clearRenderedGraph();
		showSafetyOverlay(jsonLdData.length());
	}

	private void renderBlockedGraph() {
		if (blockedJsonLdData == null || blockedJsonLdData.isBlank()) {
			return;
		}
		String jsonLdData = blockedJsonLdData;
		renderGraph(jsonLdData);
	}

	private void showSafetyOverlay(int payloadLength) {
		String formattedSize = String.format("%,d", payloadLength);
		String formattedThreshold = String.format("%,d", maxAutoRenderChars);
		safetyMessageLabel.setText(String.format("%s%nPayload size: %s chars (limit: %s chars).", SAFETY_HINT,
				formattedSize, formattedThreshold));
		safetyOverlay.setManaged(true);
		safetyOverlay.setVisible(true);
	}

	private void hideSafetyOverlay() {
		safetyOverlay.setVisible(false);
		safetyOverlay.setManaged(false);
	}

	private void loadGraphPage() {
		try {
			String htmlPath = getClass().getResource(GRAPH_HTML_PATH).toExternalForm();
			showLoadingMask();
			webEngine.load(htmlPath);
		} catch (Exception e) {
			LOGGER.error("Failed to load graph HTML resource", e);
		}
	}

	private void showLoadingMask() {
		loadingMask.setOpacity(1);
		loadingMask.setVisible(true);
	}

	private void hideLoadingMask() {
		FadeTransition fade = new FadeTransition(Duration.millis(MASK_FADE_MS), loadingMask);
		fade.setFromValue(loadingMask.getOpacity());
		fade.setToValue(0);
		fade.setOnFinished(event -> loadingMask.setVisible(false));
		fade.play();
	}

	private void injectGraphData(String jsonLdData) {
		// Double-check page is still loaded and is the correct page
		if (!pageLoaded) {
			return;
		}

		String currentLocation = webEngine.getLocation();
		if (currentLocation == null || currentLocation.contains("about:blank")) {
			return;
		}

		// Use Base64 encoding to avoid escaping issues with JSON data
		String base64Json = Base64.getEncoder().encodeToString(jsonLdData.getBytes(StandardCharsets.UTF_8));

		String script = "(function() {" + "  try {" + "    var el = document.getElementById('myGraph');"
				+ "    if (!el) return;" + "    var decoded = decodeURIComponent(escape(window.atob('" + base64Json
				+ "')));" + "    el.jsonld = decoded;" + "  } catch(e) { console.error('Graph injection error:', e); }"
				+ "})();";

		executeScriptSafe(script);
	}

	/** Resets the graph layout to its initial state. */
	public void resetLayout() {
		executeScriptSafe("document.getElementById('myGraph').reset();");
	}

	/** Zooms in on the graph. */
	public void zoomIn() {
		executeScriptSafe("document.getElementById('myGraph').zoomIn();");
	}

	/** Zooms out of the graph. */
	public void zoomOut() {
		executeScriptSafe("document.getElementById('myGraph').zoomOut();");
	}

	private void updateTheme() {
		ThemeManager tm = ThemeManager.getInstance();
		boolean isDark = false;
		String themeName = "default";

		if (tm.getTheme() != null) {
			AppThemeRegistry appTheme = AppThemeRegistry.fromTheme(tm.getTheme());
			if (appTheme != null) {
				isDark = appTheme.isDark();
				themeName = appTheme.getBaseName().toLowerCase();
			}
		}

		if (!pageLoaded) {
			return;
		}

		Color accent = tm.getAccentColor();
		String hexAccent = CssUtils.toHex(accent);

		String script = String.format("if(window.setTheme) window.setTheme(%b, '%s', '%s');", isDark, hexAccent,
				themeName);

		executeScriptSafe(script);
	}

	private void executeScriptSafe(String script) {
		if (!pageLoaded) {
			return;
		}
		try {
			webEngine.executeScript(script);
		} catch (Exception e) {
			LOGGER.error("JS Execution error: {}", e.getMessage(), e);
		}
	}

	public void clear() {
		hideSafetyOverlay();
		blockedJsonLdData = null;
		pendingJsonLdData = null;
		clearRenderedGraph();
	}

	private void clearRenderedGraph() {
		executeScriptSafe("if(document.getElementById('myGraph')) document.getElementById('myGraph').jsonld = null;");
	}

	public void setMaxAutoRenderChars(int maxAutoRenderChars) {
		this.maxAutoRenderChars = Math.max(0, maxAutoRenderChars);
	}

	public int getMaxAutoRenderChars() {
		return maxAutoRenderChars;
	}

	/**
	 * Retrieves the current SVG content from the graph visualization.
	 *
	 * @return The SVG content as a string, or null if not available
	 */
	public String getSvgContent() {
		if (!pageLoaded) {
			LOGGER.warn("Cannot get SVG: page not loaded");
			return null;
		}
		String background = getBackgroundHex();
		try {
			Object result = webEngine.executeScript("(function() {" + "  var el = document.getElementById('myGraph');"
					+ "  if (!el) return null;" + "  if (typeof el.exportSvg === 'function') { return el.exportSvg(); }"
					+ "  if (!el.shadowRoot) return null;" + "  var svg = el.shadowRoot.querySelector('svg');"
					+ "  if (!svg) return null;" + "  var clone = svg.cloneNode(true);" + "  try {"
					+ "    var bbox = svg.getBBox();" + "    var padding = 40;" + "    var x = bbox.x - padding;"
					+ "    var y = bbox.y - padding;" + "    var width = bbox.width + 2 * padding;"
					+ "    var height = bbox.height + 2 * padding;"
					+ "    clone.setAttribute('viewBox', x + ' ' + y + ' ' + width + ' ' + height);"
					+ "    clone.setAttribute('width', width);" + "    clone.setAttribute('height', height);"
					+ "    var bg = document.createElementNS('http://www.w3.org/2000/svg', 'rect');"
					+ "    bg.setAttribute('x', x);" + "    bg.setAttribute('y', y);"
					+ "    bg.setAttribute('width', width);" + "    bg.setAttribute('height', height);"
					+ "    bg.setAttribute('fill', '" + background + "');"
					+ "    clone.insertBefore(bg, clone.firstChild);" + "  } catch(e) {"
					+ "    console.warn('Could not adjust SVG bounds:', e);" + "  }"
					+ "  var serializer = new XMLSerializer();" + "  return serializer.serializeToString(clone);"
					+ "})();");
			return result != null ? result.toString() : null;
		} catch (Exception e) {
			LOGGER.error("Error getting SVG content: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Returns the resolved background color of the graph container as hex.
	 *
	 * @return The background color in #RRGGBB format.
	 */
	public String getBackgroundHex() {
		try {
			container.applyCss();
			if (container.getBackground() != null && !container.getBackground().getFills().isEmpty()) {
				var fill = container.getBackground().getFills().get(0).getFill();
				if (fill instanceof Color color) {
					return CssUtils.toHex(color);
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Unable to resolve graph background color", e);
		}
		return "#FFFFFF";
	}

	/**
	 * Bridge class for communication from JavaScript to Java. Exposed as
	 * 'window.bridge' in the WebView environment.
	 */
	public class JavaBridge {
		/**
		 * Logs an information message from JavaScript.
		 *
		 * @param message
		 *            The message to log.
		 */
		public void log(String message) {
			LOGGER.debug("[JS] {}", message);
		}

		/**
		 * Logs an error message from JavaScript.
		 *
		 * @param message
		 *            The error message to log.
		 */
		public void error(String message) {
			LOGGER.error("[JS] {}", message);
		}
	}
}
