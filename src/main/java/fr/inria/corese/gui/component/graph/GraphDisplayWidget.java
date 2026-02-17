package fr.inria.corese.gui.component.graph;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.utils.AppExecutors;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.beans.value.ChangeListener;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import atlantafx.base.theme.Theme;

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
public class GraphDisplayWidget extends VBox implements AutoCloseable {

	// ==============================================================================================
	// Constants
	// ==============================================================================================

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphDisplayWidget.class);
	private static final String COMMON_STYLESHEET = "/css/common/common.css";
	private static final String STYLESHEET = "/css/components/graph-display-widget.css";
	private static final String GRAPH_HTML_PATH = "/graph-viewer/graph-viewer.html";
	private static final String GRAPH_ELEMENT_ID = "myGraph";
	private static final String STYLE_CLASS_CONTAINER = "graph-display-container";
	private static final String STYLE_CLASS_WEBVIEW = "graph-display-webview";
	private static final String STYLE_CLASS_BORDERLESS = "graph-display-borderless";
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
	private final ThemeManager themeManager;
	private final JavaBridge bridge = new JavaBridge();
	private final AtomicLong renderRequestCounter = new AtomicLong();
	private boolean disposed = false;
	private final ChangeListener<Scene> sceneAttachmentListener = (obs, oldScene,
			newScene) -> handleSceneAttachment(newScene);
	private final ChangeListener<Worker.State> loadStateListener = (obs, oldState,
			newState) -> handleLoadStateChange(newState);
	private final ChangeListener<Throwable> loadExceptionListener = (obs, oldEx, newEx) -> {
		if (newEx != null && !disposed) {
			LOGGER.error("WebView load error", newEx);
		}
	};
	private final ChangeListener<Theme> themeChangeListener = (obs, oldTheme, newTheme) -> {
		if (!disposed) {
			Platform.runLater(this::updateTheme);
		}
	};
	private final ChangeListener<Color> accentColorChangeListener = (obs, oldColor, newColor) -> {
		if (!disposed) {
			Platform.runLater(this::updateTheme);
		}
	};

	private boolean pageLoaded = false;
	private String pendingJsonLdData = null;
	private String blockedJsonLdData = null;
	private String lastRequestedJsonLdData = null;
	private int maxAutoRenderChars = DEFAULT_MAX_AUTO_RENDER_CHARS;
	private boolean hasRenderedGraph = false;
	private Consumer<GraphStats> onGraphStatsChanged = stats -> {
	};

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/**
	 * Constructs a new GraphDisplayWidget. The HTML page is loaded when attached to
	 * a scene.
	 */
	public GraphDisplayWidget() {
		this.themeManager = ThemeManager.getInstance();
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
		sceneProperty().addListener(sceneAttachmentListener);
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
		webView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

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

		webEngine.getLoadWorker().stateProperty().addListener(loadStateListener);
		webEngine.getLoadWorker().exceptionProperty().addListener(loadExceptionListener);

		themeManager.themeProperty().addListener(themeChangeListener);
		themeManager.accentColorProperty().addListener(accentColorChangeListener);
	}

	private void handleSceneAttachment(Scene newScene) {
		if (disposed) {
			return;
		}
		Worker.State currentState = webEngine.getLoadWorker().getState();
		if (newScene != null && !pageLoaded && blockedJsonLdData == null && currentState != Worker.State.RUNNING
				&& currentState != Worker.State.SUCCEEDED) {
			Platform.runLater(this::loadGraphPage);
		}
	}

	private void handleLoadStateChange(Worker.State newState) {
		if (disposed) {
			return;
		}
		if (newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED) {
			pageLoaded = false;
			hasRenderedGraph = false;
			showLoadingOverlay();
		} else if (newState == Worker.State.SUCCEEDED) {
			onPageLoaded();
		} else if (newState == Worker.State.FAILED) {
			LOGGER.error("Failed to load graph visualization");
			pageLoaded = false;
			showLoadingOverlay();
		}
	}

	@SuppressWarnings("removal")
	private void onPageLoaded() {
		if (disposed) {
			return;
		}
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

	public record GraphStats(int tripleCount, int namedGraphCount, List<NamedGraphStat> namedGraphStats) {

		public record NamedGraphStat(String graphId, int tripleCount) {
			public NamedGraphStat {
				graphId = graphId == null ? "" : graphId.trim();
				tripleCount = Math.max(0, tripleCount);
			}
		}

		public GraphStats {
			tripleCount = Math.max(0, tripleCount);
			namedGraphCount = Math.max(0, namedGraphCount);
			namedGraphStats = namedGraphStats == null ? List.of() : List.copyOf(namedGraphStats);
		}

		public GraphStats(int tripleCount, int namedGraphCount) {
			this(tripleCount, namedGraphCount, List.of());
		}
	}

	public void setOnGraphStatsChanged(Consumer<GraphStats> listener) {
		onGraphStatsChanged = listener == null ? stats -> {
		} : listener;
	}

	public void setBorderVisible(boolean visible) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> setBorderVisible(visible));
			return;
		}
		if (visible) {
			getStyleClass().remove(STYLE_CLASS_BORDERLESS);
			return;
		}
		if (!getStyleClass().contains(STYLE_CLASS_BORDERLESS)) {
			getStyleClass().add(STYLE_CLASS_BORDERLESS);
		}
	}

	/**
	 * Displays an RDF graph from JSON-LD formatted data.
	 *
	 * @param jsonLdData
	 *            The RDF data in JSON-LD format (null or empty clears the view)
	 */
	public void displayGraph(String jsonLdData) {
		if (disposed) {
			return;
		}
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> displayGraph(jsonLdData));
			return;
		}

		if (jsonLdData == null || jsonLdData.isBlank()) {
			clear();
			return;
		}
		boolean sameAsLoadedRequest = pageLoaded && pendingJsonLdData == null && blockedJsonLdData == null
				&& jsonLdData.equals(lastRequestedJsonLdData);
		if (sameAsLoadedRequest || jsonLdData.equals(pendingJsonLdData) || jsonLdData.equals(blockedJsonLdData)) {
			return;
		}

		if (shouldDeferAutomaticRender(jsonLdData)) {
			deferGraphRendering(jsonLdData);
			return;
		}

		renderGraph(jsonLdData);
	}

	private void renderGraph(String jsonLdData) {
		if (disposed) {
			return;
		}
		hideSafetyOverlay();
		blockedJsonLdData = null;
		long requestId = renderRequestCounter.incrementAndGet();
		if (!hasRenderedGraph || !pageLoaded) {
			showLoadingOverlay();
		}

		if (!pageLoaded) {
			pendingJsonLdData = jsonLdData;
			if (getScene() == null) {
				return;
			}
			if (webEngine.getLoadWorker().getState() != Worker.State.RUNNING
					&& webEngine.getLoadWorker().getState() != Worker.State.SCHEDULED) {
				loadGraphPage();
			}
			return;
		}

		lastRequestedJsonLdData = jsonLdData;
		prepareGraphInjectionAsync(requestId, jsonLdData);
	}

	private boolean shouldDeferAutomaticRender(String jsonLdData) {
		return maxAutoRenderChars > 0 && jsonLdData.length() > maxAutoRenderChars;
	}

	private void deferGraphRendering(String jsonLdData) {
		if (disposed) {
			return;
		}
		renderRequestCounter.incrementAndGet();
		blockedJsonLdData = jsonLdData;
		pendingJsonLdData = null;
		hideLoadingOverlay();
		clearRenderedGraph();
		showSafetyOverlay();
	}

	private void renderBlockedGraph() {
		if (disposed) {
			return;
		}
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
		if (disposed) {
			return;
		}
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
			Platform.runLater(() -> {
				if (!disposed) {
					injectGraphData(requestId, jsonLdData, base64Json);
				}
			});
		});
	}

	private void injectGraphData(long requestId, String jsonLdData, String base64Json) {
		if (disposed) {
			return;
		}
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
		String safeBase64Json = escapeForJsSingleQuoted(base64Json);
		String safeRequestId = escapeForJsSingleQuoted(requestId);
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
				""".formatted(safeBase64Json, safeRequestId, GRAPH_ELEMENT_ID, safeBase64Json, safeRequestId,
				safeRequestId);
	}

	private static String escapeForJsSingleQuoted(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'");
	}

	/** Resets the graph layout to its initial state. */
	public void resetLayout() {
		executeGraphCommand("el.reset();");
	}

	/** Zooms in on the graph. */
	public void zoomIn() {
		executeGraphCommand("el.zoomIn();");
	}

	/** Zooms out of the graph. */
	public void zoomOut() {
		executeGraphCommand("el.zoomOut();");
	}

	/** Re-centers and fits the graph within the current viewport. */
	public void centerView() {
		executeGraphCommand("el.recenter();");
	}

	private void executeGraphCommand(String commandScript) {
		if (commandScript == null || commandScript.isBlank()) {
			return;
		}
		String script = String.format("var el=document.getElementById('%s'); if(el){ %s }", GRAPH_ELEMENT_ID,
				commandScript);
		executeScriptSafe(script);
	}

	private void updateTheme() {
		if (disposed) {
			return;
		}

		if (!pageLoaded) {
			return;
		}

		ThemeManager.WebThemeInfo webTheme = themeManager.getWebThemeInfo();
		String script = String.format("if(window.setTheme) window.setTheme(%b, '%s', '%s');", webTheme.dark(),
				webTheme.accentHex(), webTheme.themeName());

		executeScriptSafe(script);
	}

	private boolean executeScriptSafe(String script) {
		if (disposed || !pageLoaded) {
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

	private void notifyGraphStatsChanged(int tripleCount, int namedGraphCount) {
		notifyGraphStatsChanged(tripleCount, namedGraphCount, List.of());
	}

	private void notifyGraphStatsChanged(int tripleCount, int namedGraphCount,
			List<GraphStats.NamedGraphStat> namedGraphStats) {
		GraphStats stats = new GraphStats(tripleCount, namedGraphCount, namedGraphStats);
		if (Platform.isFxApplicationThread()) {
			onGraphStatsChanged.accept(stats);
			return;
		}
		Platform.runLater(() -> onGraphStatsChanged.accept(stats));
	}

	public void clear() {
		renderRequestCounter.incrementAndGet();
		hideSafetyOverlay();
		blockedJsonLdData = null;
		pendingJsonLdData = null;
		lastRequestedJsonLdData = null;
		hasRenderedGraph = false;
		hideLoadingOverlay();
		clearRenderedGraph();
		notifyGraphStatsChanged(0, 0);
	}

	private void clearRenderedGraph() {
		executeGraphCommand("el.jsonld = null;");
	}

	public void setMaxAutoRenderChars(int maxAutoRenderChars) {
		this.maxAutoRenderChars = Math.max(0, maxAutoRenderChars);
	}

	public int getMaxAutoRenderChars() {
		return maxAutoRenderChars;
	}

	/** Releases WebView resources and detaches global listeners. */
	@Override
	public void close() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(this::close);
			return;
		}
		if (disposed) {
			return;
		}

		disposed = true;
		renderRequestCounter.incrementAndGet();
		pendingJsonLdData = null;
		blockedJsonLdData = null;
		lastRequestedJsonLdData = null;
		hasRenderedGraph = false;
		hideSafetyOverlay();
		loadingOverlay.setVisible(false);
		loadingOverlay.setManaged(false);
		loadingOverlay.setOpacity(0);
		unregisterListeners();
		webEngine.setOnAlert(null);

		// Stop graph rendering and release heavy page resources.
		try {
			webEngine.load("about:blank");
		} catch (Exception e) {
			LOGGER.debug("Unable to unload graph web view", e);
		}
		pageLoaded = false;
	}

	private void unregisterListeners() {
		sceneProperty().removeListener(sceneAttachmentListener);
		webEngine.getLoadWorker().stateProperty().removeListener(loadStateListener);
		webEngine.getLoadWorker().exceptionProperty().removeListener(loadExceptionListener);

		themeManager.themeProperty().removeListener(themeChangeListener);
		themeManager.accentColorProperty().removeListener(accentColorChangeListener);
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
			String script = """
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
					""".formatted(escapeForJsSingleQuoted(GRAPH_ELEMENT_ID), escapeForJsSingleQuoted(background));
			Object result = webEngine.executeScript(script);
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
	@SuppressWarnings({"java:S5738", "removal"})
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
			if (disposed) {
				return;
			}
			if (!isCurrentRenderRequest(requestId)) {
				return;
			}
			hasRenderedGraph = true;
			Platform.runLater(GraphDisplayWidget.this::hideLoadingOverlay);
		}

		public void onGraphRenderFailed(String requestId, String message) {
			if (disposed) {
				return;
			}
			if (!isCurrentRenderRequest(requestId)) {
				return;
			}
			LOGGER.warn("Graph rendering failed: {}", message);
			hasRenderedGraph = false;
			Platform.runLater(GraphDisplayWidget.this::hideLoadingOverlay);
		}

		private boolean isCurrentRenderRequest(String requestId) {
			if (requestId == null || requestId.isBlank()) {
				return false;
			}
			try {
				return Long.parseLong(requestId) == renderRequestCounter.get();
			} catch (NumberFormatException _) {
				return false;
			}
		}

		public void onGraphStatsUpdated(String tripleCountValue, String namedGraphCountValue) {
			if (disposed) {
				return;
			}
			int tripleCount = parseNonNegativeInt(tripleCountValue);
			int namedGraphCount = parseNonNegativeInt(namedGraphCountValue);
			notifyGraphStatsChanged(tripleCount, namedGraphCount);
		}

		public void onGraphStatsUpdated(String tripleCountValue, String namedGraphCountValue,
				Object namedGraphStatsValue) {
			if (disposed) {
				return;
			}
			int tripleCount = parseNonNegativeInt(tripleCountValue);
			int namedGraphCount = parseNonNegativeInt(namedGraphCountValue);
			List<GraphStats.NamedGraphStat> namedGraphStats = parseNamedGraphStats(namedGraphStatsValue);
			notifyGraphStatsChanged(tripleCount, namedGraphCount, namedGraphStats);
		}

		private List<GraphStats.NamedGraphStat> parseNamedGraphStats(Object value) {
			if (!(value instanceof JSObject statsArray)) {
				return List.of();
			}
			int size = parseArrayLength(statsArray);
			if (size <= 0) {
				return List.of();
			}

			List<GraphStats.NamedGraphStat> stats = new ArrayList<>(size);
			for (int index = 0; index < size; index++) {
				try {
					GraphStats.NamedGraphStat stat = parseNamedGraphStat(statsArray.getSlot(index));
					if (stat != null && !stat.graphId().isBlank()) {
						stats.add(stat);
					}
				} catch (Exception _) {
					// Ignore malformed entry and keep the remaining stats.
				}
			}
			return stats;
		}

		private int parseArrayLength(JSObject arrayObject) {
			try {
				return parseNonNegativeInt(arrayObject.getMember("length"));
			} catch (Exception _) {
				return 0;
			}
		}

		private GraphStats.NamedGraphStat parseNamedGraphStat(Object value) {
			if (!(value instanceof JSObject statObject)) {
				return null;
			}
			String graphId = parseStringMember(statObject, "id");
			if (graphId.isBlank()) {
				return null;
			}
			int tripleCount = parseNonNegativeInt(readMember(statObject, "linkCount"));
			return new GraphStats.NamedGraphStat(graphId, tripleCount);
		}

		private String parseStringMember(JSObject object, String memberName) {
			Object value = readMember(object, memberName);
			return value == null ? "" : String.valueOf(value).trim();
		}

		private Object readMember(JSObject object, String memberName) {
			try {
				return object.getMember(memberName);
			} catch (Exception _) {
				return null;
			}
		}

		private int parseNonNegativeInt(Object value) {
			if (value == null) {
				return 0;
			}
			if (value instanceof Number number) {
				return Math.max(0, number.intValue());
			}
			return parseNonNegativeInt(String.valueOf(value));
		}

		private int parseNonNegativeInt(String value) {
			if (value == null || value.isBlank()) {
				return 0;
			}
			String normalized = value.trim();
			try {
				return Math.max(0, Integer.parseInt(normalized));
			} catch (NumberFormatException _) {
				try {
					return Math.max(0, (int) Math.floor(Double.parseDouble(normalized)));
				} catch (NumberFormatException _) {
					return 0;
				}
			}
		}
	}
}
