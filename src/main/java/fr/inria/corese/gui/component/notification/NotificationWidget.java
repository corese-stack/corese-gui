package fr.inria.corese.gui.component.notification;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.dialog.ModalService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Global toast notification manager.
 *
 * <p>
 * Provides typed notifications (info/success/warning/error), persistent loading
 * notifications, optional action buttons, and coherent stack animations.
 */
@SuppressWarnings("java:S6548")
public final class NotificationWidget {

	private static final NotificationWidget INSTANCE = new NotificationWidget();

	private static final String STYLESHEET = "/css/components/notification-widget.css";
	private static final String STYLE_CLASS_TOAST = "notification-toast";
	private static final String STYLE_CLASS_ICON = "notification-icon";
	private static final String STYLE_CLASS_LABEL = "notification-label";
	private static final String STYLE_CLASS_TITLE = "notification-title";
	private static final String STYLE_CLASS_MESSAGE = "notification-message";
	private static final String STYLE_CLASS_TEXTS = "notification-texts";
	private static final String STYLE_CLASS_ACTIONS = "notification-actions";
	private static final String STYLE_CLASS_ACTION_BUTTON = "notification-action-button";
	private static final String STYLE_CLASS_CLOSE_BUTTON = "notification-close-button";
	private static final String STYLE_CLASS_SPINNER = "notification-spinner";

	private static final Duration ENTER_DURATION = Duration.millis(230);
	private static final Duration EXIT_DURATION = Duration.millis(180);
	private static final Duration COLLAPSE_DURATION = Duration.millis(180);
	private static final Duration CLICK_DISMISS_DURATION = Duration.millis(140);

	private static final Duration INFO_DURATION = Duration.seconds(4);
	private static final Duration SUCCESS_DURATION = Duration.seconds(4);
	private static final Duration WARNING_DURATION = Duration.seconds(5);
	private static final Duration ERROR_DURATION = Duration.seconds(7);
	private static final Duration LOADING_SHOW_DELAY = Duration.millis(180);

	private static final int MAX_VISIBLE_TOASTS = 6;
	private static final double ENTER_OFFSET_Y = 18.0;
	private static final double EXIT_OFFSET_Y = -10.0;
	private static final double TOAST_MIN_WIDTH = 320.0;
	private static final double TOAST_MAX_WIDTH = 420.0;
	private static final double DEFAULT_TOAST_HEIGHT = 48.0;

	private static final String DEFAULT_INFO_TITLE = "Info";
	private static final String DEFAULT_SUCCESS_TITLE = "Success";
	private static final String DEFAULT_WARNING_TITLE = "Warning";
	private static final String DEFAULT_ERROR_TITLE = "Error";
	private static final String DEFAULT_LOADING_TITLE = "Loading";

	private final Map<HBox, ToastState> activeToasts = new HashMap<>();
	private final Deque<PendingShow> queuedShows = new ArrayDeque<>();

	private VBox container;
	private int dismissAnimationCount = 0;

	/**
	 * Handle for a persistent loading toast.
	 */
	public interface LoadingHandle extends AutoCloseable {
		@Override
		void close();

		/**
		 * Requests loading toast dismissal, then schedules a follow-up action on the
		 * JavaFX thread.
		 */
		default void closeThen(Runnable followUp) {
			close();
			if (followUp != null) {
				Platform.runLater(followUp);
			}
		}
	}

	private enum Tone {
		INFO(Styles.ACCENT, ButtonIcon.NOTIFICATION_INFO, DEFAULT_INFO_TITLE, INFO_DURATION), SUCCESS(Styles.SUCCESS,
				ButtonIcon.NOTIFICATION_SUCCESS, DEFAULT_SUCCESS_TITLE, SUCCESS_DURATION), WARNING(Styles.WARNING,
						ButtonIcon.NOTIFICATION_WARNING, DEFAULT_WARNING_TITLE, WARNING_DURATION), ERROR(Styles.DANGER,
								ButtonIcon.NOTIFICATION_ERROR, DEFAULT_ERROR_TITLE,
								ERROR_DURATION), LOADING(Styles.ACCENT, null, DEFAULT_LOADING_TITLE, Duration.ZERO);

		private final String styleClass;
		private final ButtonIcon icon;
		private final String defaultTitle;
		private final Duration defaultDuration;

		Tone(String styleClass, ButtonIcon icon, String defaultTitle, Duration defaultDuration) {
			this.styleClass = styleClass;
			this.icon = icon;
			this.defaultTitle = defaultTitle;
			this.defaultDuration = defaultDuration;
		}

		String styleClass() {
			return styleClass;
		}

		ButtonIcon icon() {
			return icon;
		}

		String defaultTitle() {
			return defaultTitle;
		}

		Duration defaultDuration() {
			return defaultDuration;
		}
	}

	private record ToastAction(String label, Runnable callback) {
	}

	private record ToastRequest(String title, String message, Tone tone, Duration visibleDuration, boolean persistent,
			boolean dismissible, ToastAction action) {
	}

	private record PendingShow(ToastRequest request, Consumer<HBox> onCreated) {
	}

	private static final class ToastState {
		private final StackPane wrapper;
		private final boolean dismissible;
		private final AtomicBoolean dismissing = new AtomicBoolean(false);
		private Animation lifecycle;
		private PauseTransition hold;
		private Animation dismiss;

		private ToastState(StackPane wrapper, boolean dismissible) {
			this.wrapper = wrapper;
			this.dismissible = dismissible;
		}

		private void stopAnimations() {
			if (lifecycle != null) {
				lifecycle.stop();
				lifecycle = null;
			}
			if (hold != null) {
				hold.stop();
				hold = null;
			}
			if (dismiss != null) {
				dismiss.stop();
				dismiss = null;
			}
		}
	}

	private final class LoadingToastHandle implements LoadingHandle {
		private final AtomicReference<HBox> toastRef = new AtomicReference<>();
		private final AtomicBoolean closeRequested = new AtomicBoolean(false);
		private final AtomicReference<PauseTransition> delayedShowRef = new AtomicReference<>();

		private void bind(HBox toast) {
			toastRef.set(toast);
			if (closeRequested.get()) {
				dismissToastAsync(toast, true);
			}
		}

		private void bindDelayedShow(PauseTransition delayedShow) {
			delayedShowRef.set(delayedShow);
		}

		private void clearDelayedShow(PauseTransition delayedShow) {
			delayedShowRef.compareAndSet(delayedShow, null);
		}

		private boolean isCloseRequested() {
			return closeRequested.get();
		}

		private void dismissToastAsync(HBox toast, boolean fast) {
			if (toast == null) {
				return;
			}
			runOnFxThread(() -> NotificationWidget.this.dismissToast(toast, fast));
		}

		@Override
		public void close() {
			closeRequested.set(true);
			runOnFxThread(() -> {
				PauseTransition delayedShow = delayedShowRef.getAndSet(null);
				if (delayedShow != null) {
					delayedShow.stop();
				}
			});
			HBox toast = toastRef.get();
			if (toast != null) {
				dismissToastAsync(toast, true);
			}
		}
	}

	private NotificationWidget() {
	}

	public static NotificationWidget getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers the notification stack container.
	 */
	public void setContainer(VBox container) {
		runOnFxThread(() -> {
			this.container = container;
			if (this.container != null) {
				this.container.setFillWidth(false);
			}
			ensureContainerStylesheet();
		});
	}

	public void showInfo(String message) {
		showMessage(Tone.INFO, message);
	}

	public void showInfo(String title, String message) {
		showMessage(Tone.INFO, title, message);
	}

	public void showSuccess(String message) {
		showMessage(Tone.SUCCESS, message);
	}

	public void showSuccess(String title, String message) {
		showMessage(Tone.SUCCESS, title, message);
	}

	public void showWarning(String message) {
		showMessage(Tone.WARNING, message);
	}

	public void showWarning(String title, String message) {
		showMessage(Tone.WARNING, title, message);
	}

	public void showError(String message) {
		showMessage(Tone.ERROR, message);
	}

	public void showError(String title, String message) {
		showMessage(Tone.ERROR, title, message);
	}

	public void showPersistentError(String message) {
		showPersistentError(null, message);
	}

	public void showPersistentError(String title, String message) {
		show(title, message, Tone.ERROR, true);
	}

	/**
	 * Displays an error toast with a Details action opening a modal with full
	 * stacktrace.
	 */
	public void showErrorWithDetails(String title, String message, Throwable throwable) {
		showErrorWithDetails(title, message, throwable, false);
	}

	/**
	 * Displays an error toast with a Details action opening a modal with full
	 * stacktrace.
	 *
	 * @param persistent
	 *            true to keep toast visible until user dismisses it
	 */
	public void showErrorWithDetails(String title, String message, Throwable throwable, boolean persistent) {
		String safeTitle = normalizeTitle(title, Tone.ERROR);
		String safeMessage = normalizeMessage(message);
		if (throwable == null) {
			if (persistent) {
				showPersistentError(safeTitle, safeMessage);
			} else {
				showError(safeTitle, safeMessage);
			}
			return;
		}

		String details = ModalService.formatThrowableDetails(throwable);
		ToastAction action = new ToastAction("Details",
				() -> ModalService.getInstance().showError(safeTitle, safeMessage, details));
		Duration duration = persistent ? Duration.ZERO : Tone.ERROR.defaultDuration();
		show(new ToastRequest(safeTitle, safeMessage, Tone.ERROR, duration, persistent, true, action));
	}

	/**
	 * Shows a persistent loading toast. Caller must close the returned handle.
	 */
	public LoadingHandle showLoading(String message) {
		return showLoading(null, message);
	}

	/**
	 * Shows a persistent loading toast with optional title.
	 */
	public LoadingHandle showLoading(String title, String message) {
		LoadingToastHandle handle = new LoadingToastHandle();
		String safeTitle = normalizeTitle(title, Tone.LOADING);
		String safeMessage = normalizeMessage(message);
		ToastRequest request = new ToastRequest(safeTitle, safeMessage, Tone.LOADING, Duration.ZERO, true, false, null);
		scheduleLoadingShow(request, handle);
		return handle;
	}

	private void scheduleLoadingShow(ToastRequest request, LoadingToastHandle handle) {
		runOnFxThread(() -> {
			if (request == null || handle == null) {
				return;
			}
			PauseTransition delayedShow = new PauseTransition(LOADING_SHOW_DELAY);
			handle.bindDelayedShow(delayedShow);
			delayedShow.setOnFinished(event -> {
				handle.clearDelayedShow(delayedShow);
				if (handle.isCloseRequested()) {
					return;
				}
				show(request, handle::bind);
			});
			delayedShow.play();
		});
	}

	private void showMessage(Tone tone, String message) {
		show(null, message, tone);
	}

	private void showMessage(Tone tone, String title, String message) {
		show(title, message, tone);
	}

	private void show(String title, String message, Tone tone) {
		show(title, message, tone, false);
	}

	private void show(String title, String message, Tone tone, boolean persistent) {
		String safeTitle = normalizeTitle(title, tone);
		String safeMessage = normalizeMessage(message);
		Duration duration = persistent ? Duration.ZERO : tone.defaultDuration();
		show(new ToastRequest(safeTitle, safeMessage, tone, duration, persistent, true, null));
	}

	private void show(ToastRequest request) {
		show(request, null);
	}

	private void show(ToastRequest request, Consumer<HBox> onCreated) {
		runOnFxThread(() -> {
			if (container == null || request == null) {
				return;
			}
			if (dismissAnimationCount > 0) {
				queuedShows.addLast(new PendingShow(request, onCreated));
				return;
			}
			showNow(request, onCreated);
		});
	}

	private void showNow(ToastRequest request, Consumer<HBox> onCreated) {
		if (container == null || request == null) {
			return;
		}
		ensureContainerStylesheet();

		HBox toast = createToast(request);
		StackPane wrapper = createWrapper(toast);
		ToastState state = new ToastState(wrapper, request.dismissible());
		activeToasts.put(toast, state);

		container.getChildren().add(wrapper);
		if (onCreated != null) {
			onCreated.accept(toast);
		}

		playToastLifecycle(toast, request, state);
		enforceStackLimit(toast);
	}

	private StackPane createWrapper(HBox toast) {
		StackPane wrapper = new StackPane(toast);
		wrapper.setAlignment(Pos.CENTER_RIGHT);
		wrapper.setPickOnBounds(false);
		wrapper.setMinHeight(0);
		wrapper.setPrefHeight(0);
		wrapper.setMaxHeight(0);
		wrapper.setUserData(toast);
		return wrapper;
	}

	private HBox createToast(ToastRequest request) {
		HBox toast = new HBox(10);
		toast.getStyleClass().addAll(Styles.ELEVATED_2, STYLE_CLASS_TOAST);
		if (request.tone().styleClass() != null) {
			toast.getStyleClass().add(request.tone().styleClass());
		}
		toast.setAlignment(Pos.CENTER_LEFT);
		toast.setMinWidth(TOAST_MIN_WIDTH);
		toast.setMaxWidth(TOAST_MAX_WIDTH);

		Node leadingNode = createLeadingNode(request.tone());

		VBox texts = new VBox(2);
		texts.getStyleClass().add(STYLE_CLASS_TEXTS);
		HBox.setHgrow(texts, Priority.ALWAYS);

		if (!request.title().isBlank()) {
			Label titleLabel = new Label(request.title());
			titleLabel.getStyleClass().addAll(STYLE_CLASS_TITLE, STYLE_CLASS_LABEL, Styles.TEXT_BOLD);
			titleLabel.setWrapText(true);
			texts.getChildren().add(titleLabel);
		}

		if (!request.message().isBlank()) {
			Label messageLabel = new Label(request.message());
			messageLabel.getStyleClass().addAll(STYLE_CLASS_MESSAGE, STYLE_CLASS_LABEL);
			if (request.title().isBlank()) {
				messageLabel.getStyleClass().add(Styles.TEXT_BOLD);
			}
			messageLabel.setWrapText(true);
			texts.getChildren().add(messageLabel);
		}

		if (texts.getChildren().isEmpty()) {
			Label fallback = new Label(request.tone().defaultTitle());
			fallback.getStyleClass().addAll(STYLE_CLASS_LABEL, Styles.TEXT_BOLD);
			texts.getChildren().add(fallback);
		}

		toast.getChildren().addAll(leadingNode, texts);

		if (hasActions(request)) {
			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);

			HBox actions = new HBox(6);
			actions.getStyleClass().add(STYLE_CLASS_ACTIONS);
			actions.setAlignment(Pos.CENTER_RIGHT);
			actions.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

			if (request.action() != null && request.action().callback() != null) {
				actions.getChildren().add(createActionButton(request.action()));
			}

			if (request.dismissible()) {
				actions.getChildren().add(createCloseButton(toast));
			}

			toast.getChildren().addAll(spacer, actions);
		}

		return toast;
	}

	private static boolean hasActions(ToastRequest request) {
		return (request.action() != null && request.action().callback() != null) || request.dismissible();
	}

	private Button createActionButton(ToastAction action) {
		Button actionButton = new Button(actionLabel(action.label()));
		actionButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, STYLE_CLASS_ACTION_BUTTON);
		actionButton.setFocusTraversable(false);
		actionButton.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
		actionButton.setOnAction(event -> action.callback().run());
		return actionButton;
	}

	private Button createCloseButton(HBox toast) {
		Button closeButton = new Button("\u00D7");
		closeButton.getStyleClass().addAll(Styles.FLAT, STYLE_CLASS_CLOSE_BUTTON);
		closeButton.setFocusTraversable(false);
		closeButton.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
		closeButton.setOnAction(event -> dismissToast(toast, false));
		return closeButton;
	}

	private Node createLeadingNode(Tone tone) {
		if (tone == Tone.LOADING) {
			ProgressIndicator spinner = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
			spinner.setMaxSize(18, 18);
			spinner.setPrefSize(18, 18);
			spinner.getStyleClass().add(STYLE_CLASS_SPINNER);
			return spinner;
		}

		FontIcon icon = new FontIcon(tone.icon().getIkon());
		icon.getStyleClass().add(STYLE_CLASS_ICON);
		icon.setIconSize(18);
		if (tone.styleClass() != null) {
			icon.getStyleClass().add(tone.styleClass());
		}
		return icon;
	}

	private void playToastLifecycle(HBox toast, ToastRequest request, ToastState state) {
		if (toast == null || request == null || state == null) {
			return;
		}
		double targetHeight = computeToastHeight(toast);
		Animation expand = createHeightAnimation(state.wrapper, 0, targetHeight, ENTER_DURATION, Interpolator.EASE_OUT);

		toast.setOpacity(0);
		toast.setTranslateY(ENTER_OFFSET_Y);

		FadeTransition fadeIn = new FadeTransition(ENTER_DURATION, toast);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);

		TranslateTransition slideIn = new TranslateTransition(ENTER_DURATION, toast);
		slideIn.setFromY(ENTER_OFFSET_Y);
		slideIn.setToY(0);
		slideIn.setInterpolator(Interpolator.EASE_OUT);

		ParallelTransition entrance = new ParallelTransition(expand, fadeIn, slideIn);
		state.lifecycle = entrance;

		if (request.dismissible()) {
			toast.setOnMouseClicked(event -> dismissToast(toast, false));
		}

		entrance.setOnFinished(event -> {
			state.lifecycle = null;
			state.wrapper.setPrefHeight(Region.USE_COMPUTED_SIZE);
			state.wrapper.setMaxHeight(Region.USE_COMPUTED_SIZE);
			toast.setTranslateY(0);
			if (request.persistent()) {
				return;
			}

			PauseTransition hold = new PauseTransition(request.visibleDuration());
			state.hold = hold;
			hold.setOnFinished(finishEvent -> {
				state.hold = null;
				dismissToast(toast, false);
			});
			hold.play();
		});

		entrance.play();
	}

	private void enforceStackLimit(HBox insertedToast) {
		if (container == null || container.getChildren().size() <= MAX_VISIBLE_TOASTS) {
			return;
		}
		HBox candidate = findOldestOverflowCandidate(insertedToast);
		if (candidate != null) {
			dismissToast(candidate, true);
		}
	}

	private HBox findOldestOverflowCandidate(HBox insertedToast) {
		if (container == null) {
			return null;
		}
		for (Node child : container.getChildren()) {
			if (child instanceof StackPane wrapper) {
				Object userData = wrapper.getUserData();
				if (userData instanceof HBox toast && toast != insertedToast) {
					ToastState state = activeToasts.get(toast);
					if (state != null && state.dismissible && !state.dismissing.get()) {
						return toast;
					}
				}
			}
		}
		return null;
	}

	private void dismissToast(HBox toast, boolean fast) {
		if (toast == null) {
			return;
		}
		ToastState state = activeToasts.get(toast);
		if (state == null || !state.dismissing.compareAndSet(false, true)) {
			return;
		}
		state.stopAnimations();
		dismissAnimationCount++;

		Duration fadeDuration = fast ? CLICK_DISMISS_DURATION : EXIT_DURATION;
		Duration collapseDuration = fast ? CLICK_DISMISS_DURATION : COLLAPSE_DURATION;
		double fromHeight = state.wrapper.getHeight();
		if (fromHeight <= 0.5) {
			fromHeight = computeToastHeight(toast);
		}
		Animation collapse = createHeightAnimation(state.wrapper, fromHeight, 0, collapseDuration,
				Interpolator.EASE_IN);

		FadeTransition fadeOut = new FadeTransition(fadeDuration, toast);
		fadeOut.setFromValue(toast.getOpacity());
		fadeOut.setToValue(0);

		ParallelTransition exit;
		if (fast) {
			exit = new ParallelTransition(collapse, fadeOut);
		} else {
			TranslateTransition slideOut = new TranslateTransition(EXIT_DURATION, toast);
			slideOut.setFromY(toast.getTranslateY());
			slideOut.setToY(EXIT_OFFSET_Y);
			slideOut.setInterpolator(Interpolator.EASE_IN);
			exit = new ParallelTransition(collapse, fadeOut, slideOut);
		}
		state.dismiss = exit;
		exit.setOnFinished(event -> {
			state.dismiss = null;
			removeToast(toast);
			onDismissAnimationFinished();
		});
		exit.play();
	}

	private void removeToast(HBox toast) {
		ToastState state = activeToasts.remove(toast);
		toast.setOnMouseClicked(null);
		if (container != null && state != null) {
			container.getChildren().remove(state.wrapper);
		}
	}

	private void onDismissAnimationFinished() {
		if (dismissAnimationCount > 0) {
			dismissAnimationCount--;
		}
		drainQueuedShows();
	}

	private void drainQueuedShows() {
		if (container == null || dismissAnimationCount > 0 || queuedShows.isEmpty()) {
			return;
		}
		while (dismissAnimationCount == 0 && !queuedShows.isEmpty()) {
			PendingShow pendingShow = queuedShows.pollFirst();
			if (pendingShow == null || pendingShow.request() == null) {
				continue;
			}
			showNow(pendingShow.request(), pendingShow.onCreated());
		}
	}

	private static Animation createHeightAnimation(StackPane wrapper, double fromHeight, double toHeight,
			Duration duration, Interpolator interpolator) {
		return new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(wrapper.prefHeightProperty(), fromHeight),
						new KeyValue(wrapper.maxHeightProperty(), fromHeight)),
				new KeyFrame(duration, new KeyValue(wrapper.prefHeightProperty(), toHeight, interpolator),
						new KeyValue(wrapper.maxHeightProperty(), toHeight, interpolator)));
	}

	private static double computeToastHeight(HBox toast) {
		if (toast == null) {
			return DEFAULT_TOAST_HEIGHT;
		}
		toast.applyCss();
		double preferred = toast.prefHeight(TOAST_MIN_WIDTH);
		if (preferred > 0) {
			return preferred;
		}
		double minimum = toast.minHeight(-1);
		if (minimum > 0) {
			return minimum;
		}
		return DEFAULT_TOAST_HEIGHT;
	}

	private void ensureContainerStylesheet() {
		if (container == null) {
			return;
		}
		java.net.URL resource = getClass().getResource(STYLESHEET);
		if (resource == null) {
			return;
		}
		String stylesheetUrl = resource.toExternalForm();
		if (!container.getStylesheets().contains(stylesheetUrl)) {
			container.getStylesheets().add(stylesheetUrl);
		}
	}

	private static String normalizeTitle(String title, Tone tone) {
		String normalized = normalizeText(title);
		return normalized.isBlank() ? tone.defaultTitle() : normalized;
	}

	private static String normalizeMessage(String message) {
		return normalizeText(message);
	}

	private static String actionLabel(String actionLabel) {
		String normalized = normalizeText(actionLabel);
		return normalized.isBlank() ? "Action" : normalized;
	}

	private static String normalizeText(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\r\n", "\n").replace('\r', '\n').trim();
	}

	private static void runOnFxThread(Runnable action) {
		if (action == null) {
			return;
		}
		if (Platform.isFxApplicationThread()) {
			action.run();
		} else {
			Platform.runLater(action);
		}
	}
}
