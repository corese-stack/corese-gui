package fr.inria.corese.gui.component.graph;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.utils.AppExecutors;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.beans.value.ChangeListener;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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
	private static final String STYLE_CLASS_LAG_SUGGESTION = "graph-lag-suggestion";
	private static final String STYLE_CLASS_LAG_SUGGESTION_MESSAGE = "graph-lag-suggestion-message";
	private static final String STYLE_CLASS_LAG_SUGGESTION_ACTION = "graph-lag-suggestion-action";
	private static final int MASK_FADE_MS = 140;
	private static final int DEFAULT_MAX_AUTO_RENDER_CHARS = 0;
	private static final String SAFETY_TITLE = "Graph preview paused";
	private static final String SAFETY_ACTION = "Display anyway";
	private static final String SAFETY_HINT = "Automatic preview is paused to keep the application responsive.";
	private static final String SAFETY_RISK_HINT = "Manual rendering can freeze the interface on large graphs.";
	private static final String LAG_ACTION = "Pause preview";
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
	private final HBox lagSuggestionBar;
	private final Label lagSuggestionLabel;
	private final Button lagSuggestionPauseButton;
	private final Label safetyMessageLabel;
	private final Button safetyActionButton;
	private final HBox safetyActionsBox;
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
	private final ChangeListener<Number> maxAutoRenderTriplesChangeListener = (obs, oldValue, newValue) -> {
		if (!disposed && newValue != null) {
			setMaxAutoRenderTriples(newValue.intValue());
		}
	};

	private boolean pageLoaded = false;
	private String pendingJsonLdData = null;
	private int pendingTripleCountHint = -1;
	private String blockedJsonLdData = null;
	private int blockedTripleCountHint = -1;
	private String lastRequestedJsonLdData = null;
	private int lastRequestedTripleCountHint = -1;
	private int maxAutoRenderChars = DEFAULT_MAX_AUTO_RENDER_CHARS;
	private int maxAutoRenderTriples = ThemeManager.getDefaultGraphAutoRenderTriplesLimit();
	private boolean hasRenderedGraph = false;
	private int lastKnownTripleCount = 0;
	private int lastKnownNamedGraphCount = 0;
	private GraphRenderStatus currentRenderStatus = GraphRenderStatus.normal();
	private Consumer<GraphStats> onGraphStatsChanged = stats -> {
	};
	private Consumer<GraphRenderStatus> onRenderStatusChanged = status -> {
	};
	private Runnable onManualRenderRequested = null;

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
		this.lagSuggestionLabel = new Label();
		this.lagSuggestionPauseButton = new Button(LAG_ACTION);
		this.lagSuggestionBar = createLagSuggestionBar();
		this.safetyMessageLabel = new Label();
		this.safetyActionButton = new Button(SAFETY_ACTION);
		this.safetyActionsBox = new HBox(safetyActionButton);
		this.safetyOverlay = createSafetyOverlay();
		setMaxAutoRenderTriples(themeManager.getGraphAutoRenderTriplesLimit());

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
		StackPane.setAlignment(lagSuggestionBar, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(lagSuggestionBar, new Insets(12));
		container.getChildren().addAll(webView, loadingOverlay, lagSuggestionBar, safetyOverlay);

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
		safetyActionsBox.getStyleClass().add(STYLE_CLASS_SAFETY_ACTIONS);
		safetyActionsBox.setAlignment(Pos.CENTER_RIGHT);

		VBox card = new VBox(10, titleLabel, safetyMessageLabel, safetyActionsBox);
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

	private HBox createLagSuggestionBar() {
		lagSuggestionLabel.getStyleClass().add(STYLE_CLASS_LAG_SUGGESTION_MESSAGE);
		lagSuggestionLabel.setWrapText(false);
		lagSuggestionLabel.setMaxWidth(Double.MAX_VALUE);
		lagSuggestionLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
		HBox.setHgrow(lagSuggestionLabel, Priority.ALWAYS);

		lagSuggestionPauseButton.getStyleClass().addAll(STYLE_CLASS_LAG_SUGGESTION_ACTION, Styles.DANGER);
		lagSuggestionPauseButton.setOnAction(event -> handlePauseSuggestedByLag());

		HBox bar = new HBox(10, lagSuggestionLabel, lagSuggestionPauseButton);
		bar.getStyleClass().addAll(STYLE_CLASS_LAG_SUGGESTION, "floating-panel");
		bar.setAlignment(Pos.CENTER_LEFT);
		bar.setFillHeight(false);
		bar.setMaxHeight(Region.USE_PREF_SIZE);
		bar.setMaxWidth(Double.MAX_VALUE);
		bar.setManaged(false);
		bar.setVisible(false);
		return bar;
	}

	private void initializeListeners() {
		// Capture JavaScript alerts for debugging
		webEngine.setOnAlert(event -> LOGGER.debug("[JS Alert] {}", event.getData()));

		webEngine.getLoadWorker().stateProperty().addListener(loadStateListener);
		webEngine.getLoadWorker().exceptionProperty().addListener(loadExceptionListener);

		themeManager.themeProperty().addListener(themeChangeListener);
		themeManager.accentColorProperty().addListener(accentColorChangeListener);
		themeManager.graphAutoRenderTriplesLimitProperty().addListener(maxAutoRenderTriplesChangeListener);
	}

	private void handleSceneAttachment(Scene newScene) {
		if (disposed) {
			return;
		}
		if (newScene != null && !pageLoaded && blockedJsonLdData == null && !isLoadWorkerRunningOrSucceeded()) {
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
				int tripleCountHint = pendingTripleCountHint;
				pendingJsonLdData = null;
				pendingTripleCountHint = -1;
				displayGraph(jsonLdData, tripleCountHint);
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

	public enum GraphRenderMode {
		NORMAL, DEGRADED, PAUSED
	}

	public record GraphRenderStatus(GraphRenderMode mode, String summary, List<String> details) {

		public GraphRenderStatus {
			mode = mode == null ? GraphRenderMode.NORMAL : mode;
			summary = normalizeSummary(mode, summary);
			details = details == null
					? List.of()
					: details.stream().map(line -> line == null ? "" : line.trim()).filter(line -> !line.isBlank())
							.toList();
		}

		public static GraphRenderStatus normal() {
			return new GraphRenderStatus(GraphRenderMode.NORMAL, "Standard rendering", List.of());
		}

		public static GraphRenderStatus degraded(String summary, List<String> details) {
			return new GraphRenderStatus(GraphRenderMode.DEGRADED, summary, details);
		}

		public static GraphRenderStatus paused(String summary, List<String> details) {
			return new GraphRenderStatus(GraphRenderMode.PAUSED, summary, details);
		}

		private static String normalizeSummary(GraphRenderMode mode, String summary) {
			if (summary != null && !summary.isBlank()) {
				return summary.trim();
			}
			return switch (mode) {
				case NORMAL -> "Standard rendering";
				case DEGRADED -> "Performance mode enabled";
				case PAUSED -> "Automatic preview paused";
			};
		}
	}

	public void setOnGraphStatsChanged(Consumer<GraphStats> listener) {
		onGraphStatsChanged = listener == null ? stats -> {
		} : listener;
	}

	public void setOnRenderStatusChanged(Consumer<GraphRenderStatus> listener) {
		onRenderStatusChanged = listener == null ? status -> {
		} : listener;
		GraphRenderStatus snapshot = currentRenderStatus;
		if (Platform.isFxApplicationThread()) {
			onRenderStatusChanged.accept(snapshot);
			return;
		}
		Platform.runLater(() -> onRenderStatusChanged.accept(snapshot));
	}

	public void setOnManualRenderRequested(Runnable listener) {
		onManualRenderRequested = listener;
	}

	public void setBorderVisible(boolean visible) {
		if (!runOnFxThreadOrDefer(() -> setBorderVisible(visible))) {
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
		displayGraph(jsonLdData, -1);
	}

	/**
	 * Displays an RDF graph from JSON-LD formatted data with a triple-count hint.
	 *
	 * @param jsonLdData
	 *            RDF data in JSON-LD format (null or empty clears the view)
	 * @param tripleCountHint
	 *            optional known triple count (&lt; 0 when unknown)
	 */
	public void displayGraph(String jsonLdData, int tripleCountHint) {
		displayGraphInternal(jsonLdData, tripleCountHint, false, false);
	}

	/**
	 * Displays an RDF graph while bypassing automatic preview guardrails.
	 *
	 * <p>
	 * Intended for explicit user action ("Display anyway"), so heavy previews can be
	 * rendered without a second confirmation cycle.
	 */
	public void displayGraphForced(String jsonLdData, int tripleCountHint) {
		displayGraphInternal(jsonLdData, tripleCountHint, true, true);
	}

	private void displayGraphInternal(String jsonLdData, int tripleCountHint, boolean bypassAutoPreviewGuardrails,
			boolean forceLoadingOverlay) {
		if (disposed) {
			return;
		}
		int normalizedTripleCountHint = Math.max(-1, tripleCountHint);
		if (!runOnFxThreadOrDefer(() -> displayGraphInternal(jsonLdData, normalizedTripleCountHint,
				bypassAutoPreviewGuardrails, forceLoadingOverlay))) {
			return;
		}

		if (jsonLdData == null || jsonLdData.isBlank()) {
			clear();
			return;
		}
		boolean sameAsLoadedRequest = pageLoaded && pendingJsonLdData == null && blockedJsonLdData == null
				&& jsonLdData.equals(lastRequestedJsonLdData)
				&& normalizedTripleCountHint == lastRequestedTripleCountHint;
		if (sameAsLoadedRequest || jsonLdData.equals(pendingJsonLdData) || jsonLdData.equals(blockedJsonLdData)) {
			return;
		}

		if (!bypassAutoPreviewGuardrails && shouldDeferAutomaticRender(jsonLdData, normalizedTripleCountHint)) {
			deferGraphRendering(jsonLdData, normalizedTripleCountHint);
			return;
		}

		renderGraph(jsonLdData, normalizedTripleCountHint, forceLoadingOverlay || bypassAutoPreviewGuardrails);
	}

	/**
	 * Shows a paused preview state without serializing a new JSON-LD snapshot.
	 *
	 * <p>
	 * Used when the graph is known to be above the auto-preview threshold and the
	 * current view should stay responsive.
	 */
	public void pausePreviewForLargeGraph(int tripleCountHint) {
		pausePreviewForLargeGraph(tripleCountHint, 0);
	}

	public void pausePreviewForLargeGraph(int tripleCountHint, int namedGraphCountHint) {
		if (disposed) {
			return;
		}
		int normalizedTripleCountHint = Math.max(0, tripleCountHint);
		int normalizedNamedGraphCountHint = Math.max(0, namedGraphCountHint);
		if (!runOnFxThreadOrDefer(
				() -> pausePreviewForLargeGraph(normalizedTripleCountHint, normalizedNamedGraphCountHint))) {
			return;
		}
		renderRequestCounter.incrementAndGet();
		hideLagSuggestion();
		blockedJsonLdData = null;
		blockedTripleCountHint = normalizedTripleCountHint;
		pendingJsonLdData = null;
		pendingTripleCountHint = -1;
		hideLoadingOverlay();
		if (normalizedTripleCountHint > 0) {
			notifyGraphStatsChanged(normalizedTripleCountHint, normalizedNamedGraphCountHint);
		}
		boolean manualRenderAvailable = hasManualRenderAction();
		notifyRenderStatusChanged(buildPausedRenderStatus(-1, normalizedTripleCountHint, manualRenderAvailable));
		showSafetyOverlay(-1, normalizedTripleCountHint, manualRenderAvailable);
	}

	private void renderGraph(String jsonLdData, int tripleCountHint, boolean forceLoadingOverlay) {
		if (disposed) {
			return;
		}
		hideSafetyOverlay();
		hideLagSuggestion();
		blockedJsonLdData = null;
		blockedTripleCountHint = -1;
		long requestId = renderRequestCounter.incrementAndGet();
		if (forceLoadingOverlay || !hasRenderedGraph || !pageLoaded) {
			showLoadingOverlay();
		}

		if (!pageLoaded) {
			pendingJsonLdData = jsonLdData;
			pendingTripleCountHint = tripleCountHint;
			if (getScene() == null) {
				return;
			}
			if (!isLoadWorkerRunningOrScheduled()) {
				loadGraphPage();
			}
			return;
		}

		lastRequestedJsonLdData = jsonLdData;
		lastRequestedTripleCountHint = tripleCountHint;
		prepareGraphInjectionAsync(requestId, jsonLdData);
	}

	private boolean shouldDeferAutomaticRender(String jsonLdData, int tripleCountHint) {
		if (maxAutoRenderTriples > 0 && tripleCountHint > maxAutoRenderTriples) {
			return true;
		}
		return maxAutoRenderChars > 0 && jsonLdData.length() > maxAutoRenderChars;
	}

	private void deferGraphRendering(String jsonLdData, int tripleCountHint) {
		if (disposed) {
			return;
		}
		renderRequestCounter.incrementAndGet();
		hideLagSuggestion();
		blockedJsonLdData = jsonLdData;
		blockedTripleCountHint = tripleCountHint;
		pendingJsonLdData = null;
		pendingTripleCountHint = -1;
		hideLoadingOverlay();
		if (tripleCountHint > 0) {
			notifyGraphStatsChanged(tripleCountHint, 0);
		}
		notifyRenderStatusChanged(buildPausedRenderStatus(jsonLdData.length(), tripleCountHint, true));
		showSafetyOverlay(jsonLdData.length(), tripleCountHint, true);
	}

	private void renderBlockedGraph() {
		if (disposed) {
			return;
		}
		if (blockedJsonLdData != null && !blockedJsonLdData.isBlank()) {
			String jsonLdData = blockedJsonLdData;
			int tripleCountHint = blockedTripleCountHint;
			renderGraph(jsonLdData, tripleCountHint, true);
			return;
		}
		if (onManualRenderRequested == null) {
			return;
		}
		safetyActionButton.setDisable(true);
		showLoadingOverlay();
		loadingOverlay.toFront();
		try {
			onManualRenderRequested.run();
		} catch (Exception e) {
			hideLoadingOverlay();
			safetyActionButton.setDisable(false);
			LOGGER.warn("Manual graph render callback failed", e);
		}
	}

	public void notifyManualRenderFailed() {
		if (disposed) {
			return;
		}
		if (!runOnFxThreadOrDefer(this::notifyManualRenderFailed)) {
			return;
		}
		hideLoadingOverlay();
		if (safetyOverlay.isVisible()) {
			safetyActionButton.setDisable(!hasManualRenderAction());
		}
	}

	private void showSafetyOverlay(int jsonChars, int tripleCountHint, boolean allowManualRender) {
		safetyMessageLabel.setText(buildSafetyMessage(jsonChars, tripleCountHint));
		safetyActionButton.setDisable(!allowManualRender);
		safetyActionButton.setManaged(allowManualRender);
		safetyActionButton.setVisible(allowManualRender);
		safetyActionsBox.setManaged(allowManualRender);
		safetyActionsBox.setVisible(allowManualRender);
		setGraphCanvasVisible(false);
		safetyOverlay.setManaged(true);
		safetyOverlay.setVisible(true);
	}

	private boolean hasManualRenderAction() {
		return onManualRenderRequested != null;
	}

	private String buildSafetyMessage(int jsonChars, int tripleCountHint) {
		StringBuilder message = new StringBuilder(SAFETY_HINT);
		if (maxAutoRenderTriples > 0 && tripleCountHint > maxAutoRenderTriples) {
			message.append(String.format(Locale.ROOT, "%nDetected %d triples (limit: %d).", tripleCountHint,
					maxAutoRenderTriples));
		}
		if (maxAutoRenderChars > 0 && jsonChars > maxAutoRenderChars) {
			message.append(String.format(Locale.ROOT, "%nSerialized graph size: %,d chars (limit: %,d).", jsonChars,
					maxAutoRenderChars));
		}
		message.append('\n').append("You can adjust this threshold in Settings > Appearance > Graph Preview.");
		message.append('\n').append(SAFETY_RISK_HINT);
		return message.toString();
	}

	private GraphRenderStatus buildPausedRenderStatus(int jsonChars, int tripleCountHint,
			boolean manualRenderAvailable) {
		List<String> details = new ArrayList<>();
		if (maxAutoRenderTriples > 0 && tripleCountHint > maxAutoRenderTriples) {
			details.add(String.format(Locale.ROOT, "Detected %d triples (auto-preview limit: %d).", tripleCountHint,
					maxAutoRenderTriples));
		}
		if (jsonChars >= 0 && maxAutoRenderChars > 0 && jsonChars > maxAutoRenderChars) {
			details.add(String.format(Locale.ROOT, "Serialized graph size: %,d chars (limit: %,d).", jsonChars,
					maxAutoRenderChars));
		}
		details.add("Threshold can be changed in Settings > Appearance > Graph Preview.");
		if (manualRenderAvailable) {
			details.add("Use \"Display anyway\" to force rendering on demand.");
		} else {
			details.add("Preview payload is skipped at this size to keep the UI responsive.");
		}
		details.add("Manual rendering can freeze the interface on very large graphs.");
		return GraphRenderStatus.paused("Automatic preview paused", details);
	}

	private void hideSafetyOverlay() {
		safetyOverlay.setVisible(false);
		safetyOverlay.setManaged(false);
		setGraphCanvasVisible(true);
		hideLagSuggestion();
	}

	private void setGraphCanvasVisible(boolean visible) {
		webView.setVisible(visible);
		webView.setManaged(visible);
	}

	private void showLagSuggestion(double avgFps) {
		double safeFps = Math.max(0.0, avgFps);
		if (disposed || safeFps <= 0) {
			return;
		}
		if (!hasRenderedGraph || safetyOverlay.isVisible() || loadingOverlay.isVisible()) {
			return;
		}
		if (currentRenderStatus.mode() == GraphRenderMode.PAUSED) {
			return;
		}
		String text = String.format(Locale.ROOT, "Rendering is slow (~%.1f FPS).", safeFps);
		lagSuggestionLabel.setText(text);
		lagSuggestionBar.setManaged(true);
		lagSuggestionBar.setVisible(true);
	}

	private void hideLagSuggestion() {
		lagSuggestionBar.setVisible(false);
		lagSuggestionBar.setManaged(false);
	}

	private void handlePauseSuggestedByLag() {
		hideLagSuggestion();
		int tripleCountHint = Math.max(0, lastKnownTripleCount);
		int namedGraphCountHint = Math.max(0, lastKnownNamedGraphCount);
		if (tripleCountHint <= 0) {
			tripleCountHint = Math.max(0, lastRequestedTripleCountHint);
		}
		if (tripleCountHint <= 0) {
			tripleCountHint = Math.max(0, blockedTripleCountHint);
		}
		pausePreviewForLargeGraph(tripleCountHint, namedGraphCountHint);
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
		hideLagSuggestion();
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
		String script = GraphDisplayScripts.buildGraphInjectionScript(base64Json, renderRequestId, GRAPH_ELEMENT_ID);

		if (!executeScriptSafe(script)) {
			hideLoadingOverlay();
		}
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
		String script = GraphDisplayScripts.buildGraphCommandScript(GRAPH_ELEMENT_ID, commandScript);
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
		String script = GraphDisplayScripts.buildThemeScript(webTheme);

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
		lastKnownTripleCount = Math.max(0, stats.tripleCount());
		lastKnownNamedGraphCount = Math.max(0, stats.namedGraphCount());
		if (Platform.isFxApplicationThread()) {
			onGraphStatsChanged.accept(stats);
			return;
		}
		Platform.runLater(() -> onGraphStatsChanged.accept(stats));
	}

	private void notifyRenderStatusChanged(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		if (safeStatus.equals(currentRenderStatus)) {
			return;
		}
		currentRenderStatus = safeStatus;
		if (Platform.isFxApplicationThread()) {
			onRenderStatusChanged.accept(safeStatus);
			return;
		}
		Platform.runLater(() -> onRenderStatusChanged.accept(safeStatus));
	}

	private static GraphRenderMode parseRenderMode(String modeValue) {
		if (modeValue == null || modeValue.isBlank()) {
			return GraphRenderMode.NORMAL;
		}
		String normalized = modeValue.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "degraded" -> GraphRenderMode.DEGRADED;
			case "paused" -> GraphRenderMode.PAUSED;
			default -> GraphRenderMode.NORMAL;
		};
	}

	public void clear() {
		if (!runOnFxThreadOrDefer(this::clear)) {
			return;
		}
		if (disposed) {
			return;
		}
		renderRequestCounter.incrementAndGet();
		hideSafetyOverlay();
		hideLagSuggestion();
		blockedJsonLdData = null;
		blockedTripleCountHint = -1;
		pendingJsonLdData = null;
		pendingTripleCountHint = -1;
		lastRequestedJsonLdData = null;
		lastRequestedTripleCountHint = -1;
		hasRenderedGraph = false;
		lastKnownTripleCount = 0;
		lastKnownNamedGraphCount = 0;
		hideLoadingOverlay();
		clearRenderedGraph();
		notifyRenderStatusChanged(GraphRenderStatus.normal());
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

	public void setMaxAutoRenderTriples(int maxAutoRenderTriples) {
		this.maxAutoRenderTriples = Math.max(0, maxAutoRenderTriples);
	}

	public int getMaxAutoRenderTriples() {
		return maxAutoRenderTriples;
	}

	/** Releases WebView resources and detaches global listeners. */
	@Override
	public void close() {
		if (!runOnFxThreadOrDefer(this::close)) {
			return;
		}
		if (disposed) {
			return;
		}

		disposed = true;
		renderRequestCounter.incrementAndGet();
		pendingJsonLdData = null;
		pendingTripleCountHint = -1;
		blockedJsonLdData = null;
		blockedTripleCountHint = -1;
		lastRequestedJsonLdData = null;
		lastRequestedTripleCountHint = -1;
		hasRenderedGraph = false;
		lastKnownTripleCount = 0;
		lastKnownNamedGraphCount = 0;
		hideSafetyOverlay();
		hideLagSuggestion();
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

	private boolean runOnFxThreadOrDefer(Runnable action) {
		if (Platform.isFxApplicationThread()) {
			return true;
		}
		Platform.runLater(action);
		return false;
	}

	private boolean isLoadWorkerRunningOrScheduled() {
		Worker.State state = webEngine.getLoadWorker().getState();
		return state == Worker.State.RUNNING || state == Worker.State.SCHEDULED;
	}

	private boolean isLoadWorkerRunningOrSucceeded() {
		Worker.State state = webEngine.getLoadWorker().getState();
		return state == Worker.State.RUNNING || state == Worker.State.SUCCEEDED;
	}

	private void unregisterListeners() {
		sceneProperty().removeListener(sceneAttachmentListener);
		webEngine.getLoadWorker().stateProperty().removeListener(loadStateListener);
		webEngine.getLoadWorker().exceptionProperty().removeListener(loadExceptionListener);

		themeManager.themeProperty().removeListener(themeChangeListener);
		themeManager.accentColorProperty().removeListener(accentColorChangeListener);
		themeManager.graphAutoRenderTriplesLimitProperty().removeListener(maxAutoRenderTriplesChangeListener);
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
			String script = GraphDisplayScripts.buildSvgExportScript(GRAPH_ELEMENT_ID, background);
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
	@SuppressWarnings("java:S5738")
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
			Platform.runLater(() -> {
				hasRenderedGraph = true;
				hideLoadingOverlay();
			});
		}

		public void onGraphRenderFailed(String requestId, String message) {
			if (disposed) {
				return;
			}
			if (!isCurrentRenderRequest(requestId)) {
				return;
			}
			LOGGER.warn("Graph rendering failed: {}", message);
			Platform.runLater(() -> {
				hasRenderedGraph = false;
				hideLoadingOverlay();
				notifyRenderStatusChanged(GraphRenderStatus.paused("Rendering failed",
						List.of(message == null || message.isBlank() ? "Unknown rendering error." : message.trim())));
			});
		}

		private boolean isCurrentRenderRequest(String requestId) {
			return GraphBridgeParsing.isCurrentRenderRequest(requestId, renderRequestCounter.get());
		}

		public void onGraphStatsUpdated(String tripleCountValue, String namedGraphCountValue) {
			if (disposed) {
				return;
			}
			int tripleCount = GraphBridgeParsing.parseNonNegativeInt(tripleCountValue);
			int namedGraphCount = GraphBridgeParsing.parseNonNegativeInt(namedGraphCountValue);
			notifyGraphStatsChanged(tripleCount, namedGraphCount);
		}

		public void onGraphStatsUpdated(String tripleCountValue, String namedGraphCountValue,
				Object namedGraphStatsValue) {
			if (disposed) {
				return;
			}
			int tripleCount = GraphBridgeParsing.parseNonNegativeInt(tripleCountValue);
			int namedGraphCount = GraphBridgeParsing.parseNonNegativeInt(namedGraphCountValue);
			List<GraphStats.NamedGraphStat> namedGraphStats = GraphBridgeParsing
					.parseNamedGraphStats(namedGraphStatsValue);
			notifyGraphStatsChanged(tripleCount, namedGraphCount, namedGraphStats);
		}

		public void onGraphRenderProfileUpdated(String modeValue, String summaryValue) {
			onGraphRenderProfileUpdated(modeValue, summaryValue, null);
		}

		public void onGraphRenderProfileUpdated(String modeValue, String summaryValue, Object detailsValue) {
			if (disposed) {
				return;
			}
			GraphRenderMode mode = parseRenderMode(modeValue);
			String summary = GraphBridgeParsing.parseTrimmedString(summaryValue);
			List<String> details = GraphBridgeParsing.parseStringList(detailsValue);
			notifyRenderStatusChanged(new GraphRenderStatus(mode, summary, details));
		}

		public void onGraphLagDetected(String averageFpsValue, String nodeCountValue, String tripleCountValue) {
			if (disposed) {
				return;
			}
			double avgFps = GraphBridgeParsing.parseNonNegativeDouble(averageFpsValue);
			Platform.runLater(() -> showLagSuggestion(avgFps));
		}

		public void onGraphLagCleared() {
			if (disposed) {
				return;
			}
			Platform.runLater(GraphDisplayWidget.this::hideLagSuggestion);
		}
	}
}
