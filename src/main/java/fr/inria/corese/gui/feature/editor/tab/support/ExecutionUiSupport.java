package fr.inria.corese.gui.feature.editor.tab.support;

import java.util.concurrent.atomic.AtomicBoolean;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.feature.editor.tab.TabContext;
import fr.inria.corese.gui.feature.editor.tab.TabEditorController;
import javafx.application.Platform;
import javafx.scene.control.Tab;

/**
 * Shared UI helpers for cancellable tab-bound executions.
 */
public final class ExecutionUiSupport {

	private ExecutionUiSupport() {
		throw new AssertionError("Utility class");
	}

	public static void completeExecution(TabContext context, NotificationWidget.LoadingHandle loadingHandle,
			AtomicBoolean completionSignaled, Runnable followUp) {
		if (!completionSignaled.compareAndSet(false, true)) {
			return;
		}
		Runnable completion = () -> {
			setExecutionState(context, false);
			if (followUp != null) {
				followUp.run();
			}
		};
		if (loadingHandle == null) {
			completion.run();
			return;
		}
		loadingHandle.closeThen(completion);
	}

	public static void setExecutionState(TabContext context, boolean running) {
		if (context == null) {
			return;
		}
		if (Platform.isFxApplicationThread()) {
			context.executionRunningProperty().set(running);
			return;
		}
		Platform.runLater(() -> context.executionRunningProperty().set(running));
	}

	public static void hideResultPaneIfSelected(TabEditorController tabEditorController, Tab sourceTab) {
		if (tabEditorController == null || sourceTab == null) {
			return;
		}
		Runnable hideAction = () -> {
			if (sourceTab.equals(tabEditorController.getSelectedTab())) {
				tabEditorController.hideResultPane();
			}
		};
		if (Platform.isFxApplicationThread()) {
			hideAction.run();
			return;
		}
		Platform.runLater(hideAction);
	}
}
