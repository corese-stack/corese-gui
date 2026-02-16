package fr.inria.corese.gui.component.notification;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.service.ModalService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Global toast notification manager.
 *
 * <p>
 * Provides typed notifications (info/success/warning/error), persistent loading
 * notifications, optional action buttons, and stacked enter/exit animations.
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
	private static final String STYLE_CLASS_SPINNER = "notification-spinner";

	private static final Duration ENTER_DURATION = Duration.millis(230);
	private static final Duration EXIT_DURATION = Duration.millis(180);
	private static final Duration STACK_SHIFT_DURATION = Duration.millis(170);
	private static final Duration CLICK_DISMISS_DURATION = Duration.millis(140);

	private static final Duration INFO_DURATION = Duration.seconds(4);
	private static final Duration SUCCESS_DURATION = Duration.seconds(4);
	private static final Duration WARNING_DURATION = Duration.seconds(5);
	private static final Duration ERROR_DURATION = Duration.seconds(7);

	private static final int MAX_VISIBLE_TOASTS = 6;
	private static final double ENTER_OFFSET_Y = 18.0;
	private static final double EXIT_OFFSET_Y = -10.0;
	private static final double STACK_SHIFT_OFFSET_Y = 10.0;
	private static final double TOAST_MIN_WIDTH = 320.0;
	private static final double TOAST_MAX_WIDTH = 420.0;

	private static final String DEFAULT_INFO_TITLE = "Info";
	private static final String DEFAULT_SUCCESS_TITLE = "Success";
	private static final String DEFAULT_WARNING_TITLE = "Warning";
	private static final String DEFAULT_ERROR_TITLE = "Error";
	private static final String DEFAULT_LOADING_TITLE = "Loading";

	private final Map<HBox, ToastState> activeToasts = new HashMap<>();

	private VBox container;

	/**
	 * Handle for a persistent loading toast.
	 */
	public interface LoadingHandle extends AutoCloseable {
		@Override
		void close();
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

	private static final class ToastState {
		private Animation entrance;
		private PauseTransition hold;
		private final AtomicBoolean dismissing = new AtomicBoolean(false);
		private final boolean persistent;

		private ToastState(boolean persistent) {
			this.persistent = persistent;
		}

		private void stopAnimations() {
			if (entrance != null) {
				entrance.stop();
				entrance = null;
			}
			if (hold != null) {
				hold.stop();
				hold = null;
			}
		}
	}

	private final class LoadingToastHandle implements LoadingHandle {
		private final AtomicReference<HBox> toastRef = new AtomicReference<>();
		private final AtomicBoolean closeRequested = new AtomicBoolean(false);

		private void bind(HBox toast) {
			toastRef.set(toast);
			if (closeRequested.get()) {
				dismissToastAsync(toast, true);
			}
		}

		@Override
		public void close() {
			closeRequested.set(true);
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
			ensureContainerStylesheet();
		});
	}

	public void showInfo(String message) {
		show(null, message, Tone.INFO);
	}

	public void showInfo(String title, String message) {
		show(title, message, Tone.INFO);
	}

	public void showSuccess(String message) {
		show(null, message, Tone.SUCCESS);
	}

	public void showSuccess(String title, String message) {
		show(title, message, Tone.SUCCESS);
	}

	public void showWarning(String message) {
		show(null, message, Tone.WARNING);
	}

	public void showWarning(String title, String message) {
		show(title, message, Tone.WARNING);
	}

	public void showError(String message) {
		show(null, message, Tone.ERROR);
	}

	public void showError(String title, String message) {
		show(title, message, Tone.ERROR);
	}

	/**
	 * Displays an error toast with a Details action opening a modal with full
	 * stacktrace.
	 */
	public void showErrorWithDetails(String title, String message, Throwable throwable) {
		String safeTitle = normalizeTitle(title, Tone.ERROR);
		String safeMessage = normalizeMessage(message);
		if (throwable == null) {
			showError(safeTitle, safeMessage);
			return;
		}

		String details = ModalService.formatThrowableDetails(throwable);
		ToastAction action = new ToastAction("Details",
				() -> ModalService.getInstance().showError(safeTitle, safeMessage, details));
		show(new ToastRequest(safeTitle, safeMessage, Tone.ERROR, Tone.ERROR.defaultDuration(), false, true, action));
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
		show(request, handle::bind);
		return handle;
	}

	private void show(String title, String message, Tone tone) {
		String safeTitle = normalizeTitle(title, tone);
		String safeMessage = normalizeMessage(message);
		show(new ToastRequest(safeTitle, safeMessage, tone, tone.defaultDuration(), false, true, null));
	}

	private void show(ToastRequest request) {
		show(request, null);
	}

	private void show(ToastRequest request, java.util.function.Consumer<HBox> onCreated) {
		runOnFxThread(() -> {
			if (container == null) {
				return;
			}
			ensureContainerStylesheet();

			HBox toast = createToast(request);
			ToastState state = new ToastState(request.persistent());
			activeToasts.put(toast, state);

			container.getChildren().add(toast);
			if (onCreated != null) {
				onCreated.accept(toast);
			}

			animateStackOnInsert(toast);
			playToastLifecycle(toast, request, state);
			enforceStackLimit();
		});
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

		if (request.action() != null && request.action().callback() != null) {
			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);

			Button actionButton = new Button(actionLabel(request.action().label()));
			actionButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, STYLE_CLASS_ACTION_BUTTON);
			actionButton.setFocusTraversable(false);
			actionButton.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
			actionButton.setOnAction(event -> request.action().callback().run());

			HBox actions = new HBox(actionButton);
			actions.getStyleClass().add(STYLE_CLASS_ACTIONS);
			actions.setAlignment(Pos.CENTER_RIGHT);
			actions.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

			toast.getChildren().addAll(spacer, actions);
		}

		return toast;
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
		toast.setOpacity(0);
		toast.setTranslateY(ENTER_OFFSET_Y);

		FadeTransition fadeIn = new FadeTransition(ENTER_DURATION, toast);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);

		TranslateTransition slideIn = new TranslateTransition(ENTER_DURATION, toast);
		slideIn.setFromY(ENTER_OFFSET_Y);
		slideIn.setToY(0);
		slideIn.setInterpolator(Interpolator.EASE_OUT);

		ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);
		state.entrance = entrance;

		if (request.dismissible()) {
			toast.setOnMouseClicked(event -> dismissToast(toast, false));
		}

		entrance.setOnFinished(event -> {
			state.entrance = null;
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

	private void enforceStackLimit() {
		if (container == null) {
			return;
		}
		while (container.getChildren().size() > MAX_VISIBLE_TOASTS) {
			HBox candidate = findOldestDismissableToast();
			if (candidate == null) {
				candidate = findOldestToast();
			}
			if (candidate == null) {
				break;
			}
			dismissToast(candidate, true);
		}
	}

	private HBox findOldestDismissableToast() {
		if (container == null) {
			return null;
		}
		for (Node child : container.getChildren()) {
			if (child instanceof HBox toast) {
				ToastState state = activeToasts.get(toast);
				if (state != null && !state.persistent) {
					return toast;
				}
			}
		}
		return null;
	}

	private HBox findOldestToast() {
		if (container == null) {
			return null;
		}
		for (Node child : container.getChildren()) {
			if (child instanceof HBox toast) {
				return toast;
			}
		}
		return null;
	}

	private void dismissToastAsync(HBox toast, boolean fast) {
		runOnFxThread(() -> dismissToast(toast, fast));
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

		Duration duration = fast ? CLICK_DISMISS_DURATION : EXIT_DURATION;

		FadeTransition fadeOut = new FadeTransition(duration, toast);
		fadeOut.setFromValue(toast.getOpacity());
		fadeOut.setToValue(0);

		TranslateTransition slideOut = new TranslateTransition(duration, toast);
		slideOut.setFromY(toast.getTranslateY());
		slideOut.setToY(EXIT_OFFSET_Y);
		slideOut.setInterpolator(Interpolator.EASE_IN);

		ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
		exit.setOnFinished(event -> removeToast(toast));
		exit.play();
	}

	private void removeToast(HBox toast) {
		toast.setOnMouseClicked(null);
		activeToasts.remove(toast);
		if (container != null) {
			container.getChildren().remove(toast);
			animateStackOnRemove();
		}
	}

	private void animateStackOnInsert(HBox insertedToast) {
		if (container == null) {
			return;
		}
		for (Node child : container.getChildren()) {
			if (!(child instanceof HBox toast) || toast == insertedToast) {
				continue;
			}
			TranslateTransition shift = new TranslateTransition(STACK_SHIFT_DURATION, toast);
			shift.setFromY(STACK_SHIFT_OFFSET_Y);
			shift.setToY(0);
			shift.setInterpolator(Interpolator.EASE_OUT);
			shift.play();
		}
	}

	private void animateStackOnRemove() {
		if (container == null) {
			return;
		}
		for (Node child : container.getChildren()) {
			if (!(child instanceof HBox toast)) {
				continue;
			}
			TranslateTransition shift = new TranslateTransition(STACK_SHIFT_DURATION, toast);
			shift.setFromY(-STACK_SHIFT_OFFSET_Y);
			shift.setToY(0);
			shift.setInterpolator(Interpolator.EASE_OUT);
			shift.play();
		}
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
		String normalized = normalizeText(message);
		return normalized;
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
