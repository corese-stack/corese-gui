package fr.inria.corese.gui.component.graph;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.theme.AppThemeRegistry;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.utils.AppExecutors;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final String STYLE_CLASS_LOADING_OVERLAY = "graph-loading-overlay";
	private static final String STYLE_CLASS_LOADING_MASK = "graph-loading-mask";
	private static final String STYLE_CLASS_LOADING_CONTENT = "graph-loading-content";
	private static final String STYLE_CLASS_LOADING_INDICATOR = "graph-loading-indicator";
	private static final String STYLE_CLASS_LOADING_LABEL = "graph-loading-label";
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
	private static final String SAFETY_HINT = "A large graph was detected and automatic preview is paused to keep the application responsive.";
	private static final String SAFETY_RISK_HINT = "Manual rendering can freeze the interface or exhaust available memory.";
	private static final String LOADING_HINT = "Rendering graph preview...";
	private static final double LOADING_INDICATOR_SIZE = 36;

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private final WebView webView;
	private final WebEngine webEngine;
	private final StackPane container;
	private final StackPane loadingOverlay;
	private final StackPane safetyOverlay;
	private final Label safetyMessageLabel;
	private final Button safetyActionButton;
	private final JavaBridge bridge = new JavaBridge();
	private final AtomicLong renderRequestCounter = new AtomicLong();

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
		this.loadingOverlay = createLoadingOverlay();
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
		container.getChildren().addAll(webView, loadingOverlay, safetyOverlay);

		setVgrow(container, Priority.ALWAYS);
		getChildren().add(container);
	}

	private StackPane createLoadingOverlay() {
		Region backdrop = new Region();
		backdrop.getStyleClass().add(STYLE_CLASS_LOADING_MASK);
		backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		backdrop.setMouseTransparent(true);

		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.getStyleClass().add(STYLE_CLASS_LOADING_INDICATOR);
		progressIndicator.setMaxSize(LOADING_INDICATOR_SIZE, LOADING_INDICATOR_SIZE);
		progressIndicator.setPrefSize(LOADING_INDICATOR_SIZE, LOADING_INDICATOR_SIZE);

		Label loadingLabel = new Label(LOADING_HINT);
		loadingLabel.getStyleClass().add(STYLE_CLASS_LOADING_LABEL);

		VBox content = new VBox(8, progressIndicator, loadingLabel);
		content.getStyleClass().add(STYLE_CLASS_LOADING_CONTENT);
		content.setAlignment(Pos.CENTER);
		content.setMouseTransparent(true);

		StackPane overlay = new StackPane(backdrop, content);
		overlay.getStyleClass().add(STYLE_CLASS_LOADING_OVERLAY);
		overlay.setVisible(true);
		overlay.setManaged(true);
		overlay.setOpacity(1);
		return overlay;
	}

	private StackPane createSafetyOverlay() {
		Label titleLabel = new Label(SAFETY_TITLE);
		titleLabel.getStyleClass().add(STYLE_CLASS_SAFETY_TITLE);

		safetyMessageLabel.getStyleClass().add(STYLE_CLASS_SAFETY_MESSAGE);
		safetyMessageLabel.setWrapText(true);

		safetyActionButton.getStyleClass().addAll(STYLE_CLASS_SAFETY_ACTION, Styles.DANGER);
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
				showLoadingOverlay();
			} else if (newState == Worker.State.SUCCEEDED) {
				onPageLoaded();
			} else if (newState == Worker.State.FAILED) {
				LOGGER.error("Failed to load graph visualization");
				pageLoaded = false;
				showLoadingOverlay();
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
				String jsonLdData = pendingJsonLdData;
				pendingJsonLdData = null;
				displayGraph(jsonLdData);
			} else {
				hideLoadingOverlay();
			}
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
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> displayGraph(jsonLdData));
			return;
		}

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
		long requestId = renderRequestCounter.incrementAndGet();
		showLoadingOverlay();

		if (!pageLoaded) {
			pendingJsonLdData = jsonLdData;
			if (webEngine.getLoadWorker().getState() != Worker.State.RUNNING) {
				loadGraphPage();
			}
			return;
		}

		prepareGraphInjectionAsync(requestId, jsonLdData);
	}

	private boolean shouldDeferAutomaticRender(String jsonLdData) {
		return maxAutoRenderChars > 0 && jsonLdData.length() > maxAutoRenderChars;
	}

	private void deferGraphRendering(String jsonLdData) {
		renderRequestCounter.incrementAndGet();
		blockedJsonLdData = jsonLdData;
		pendingJsonLdData = null;
		hideLoadingOverlay();
		clearRenderedGraph();
		showSafetyOverlay();
	}

	private void renderBlockedGraph() {
		if (blockedJsonLdData == null || blockedJsonLdData.isBlank()) {
			return;
		}
		String jsonLdData = blockedJsonLdData;
		renderGraph(jsonLdData);
	}

	private void showSafetyOverlay() {
		safetyMessageLabel.setText(String.format("%s%n%s", SAFETY_HINT, SAFETY_RISK_HINT));
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
			showLoadingOverlay();
			webEngine.load(htmlPath);
		} catch (Exception e) {
			LOGGER.error("Failed to load graph HTML resource", e);
		}
	}

	private void showLoadingOverlay() {
		loadingOverlay.setOpacity(1);
		loadingOverlay.setVisible(true);
		loadingOverlay.setManaged(true);
	}

	private void hideLoadingOverlay() {
		if (!loadingOverlay.isVisible()) {
			return;
		}
		FadeTransition fade = new FadeTransition(Duration.millis(MASK_FADE_MS), loadingOverlay);
		fade.setFromValue(loadingOverlay.getOpacity());
		fade.setToValue(0);
		fade.setOnFinished(event -> {
			loadingOverlay.setVisible(false);
			loadingOverlay.setManaged(false);
		});
		fade.play();
	}

	private void prepareGraphInjectionAsync(long requestId, String jsonLdData) {
		AppExecutors.execute(() -> {
			String base64Json = Base64.getEncoder().encodeToString(jsonLdData.getBytes(StandardCharsets.UTF_8));
			Platform.runLater(() -> injectGraphData(requestId, jsonLdData, base64Json));
		});
	}

	private void injectGraphData(long requestId, String jsonLdData, String base64Json) {
		if (requestId != renderRequestCounter.get()) {
			return;
		}

		// Double-check page is still loaded and is the correct page
		if (!pageLoaded) {
			pendingJsonLdData = jsonLdData;
			return;
		}

		String currentLocation = webEngine.getLocation();
		if (currentLocation == null || currentLocation.contains("about:blank")) {
			pendingJsonLdData = jsonLdData;
			return;
		}

		String renderRequestId = String.valueOf(requestId);
		String script = buildGraphInjectionScript(base64Json, renderRequestId);

		if (!executeScriptSafe(script)) {
			hideLoadingOverlay();
		}
	}

	private static String buildGraphInjectionScript(String base64Json, String requestId) {
		return "(function() {" + "  if (window.renderGraphFromBase64) {" + "    window.renderGraphFromBase64('"
				+ base64Json + "', '" + requestId + "');" + "    return;" + "  }" + "  try {"
				+ "    var el = document.getElementById('myGraph');"
				+ "    if (!el) throw new Error('Graph component not found');"
				+ "    var decoded = decodeURIComponent(escape(window.atob('" + base64Json + "')));"
				+ "    el.jsonld = decoded;"
				+ "    if (window.bridge && typeof window.bridge.onGraphRenderComplete === 'function') {"
				+ "      window.bridge.onGraphRenderComplete('" + requestId + "');" + "    }" + "  } catch(e) {"
				+ "    if (window.bridge && typeof window.bridge.onGraphRenderFailed === 'function') {"
				+ "      window.bridge.onGraphRenderFailed('" + requestId
				+ "', String(e && e.message ? e.message : e));" + "    }" + "  }" + "})();";
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

	private boolean executeScriptSafe(String script) {
		if (!pageLoaded) {
			return false;
		}
		try {
			webEngine.executeScript(script);
			return true;
		} catch (Exception e) {
			LOGGER.error("JS Execution error: {}", e.getMessage(), e);
			return false;
		}
	}

	public void clear() {
		renderRequestCounter.incrementAndGet();
		hideSafetyOverlay();
		blockedJsonLdData = null;
		pendingJsonLdData = null;
		hideLoadingOverlay();
		clearRenderedGraph();
	}

	private boolean isCurrentRenderRequest(String requestId) {
		if (requestId == null || requestId.isBlank()) {
			return false;
		}
		try {
			return Long.parseLong(requestId) == renderRequestCounter.get();
		} catch (NumberFormatException ignored) {
			return false;
		}
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

		public void onGraphRenderComplete(String requestId) {
			if (!isCurrentRenderRequest(requestId)) {
				return;
			}
			Platform.runLater(GraphDisplayWidget.this::hideLoadingOverlay);
		}

		public void onGraphRenderFailed(String requestId, String message) {
			if (!isCurrentRenderRequest(requestId)) {
				return;
			}
			LOGGER.warn("Graph rendering failed: {}", message);
			Platform.runLater(GraphDisplayWidget.this::hideLoadingOverlay);
		}
	}
}
