package fr.inria.corese.gui.feature.main;

import atlantafx.base.controls.ModalPane;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.dialog.ModalService;
import fr.inria.corese.gui.core.view.AbstractView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * MainView defines the primary layout of the application's user interface.
 *
 * <p>
 * Structure:
 * <ul>
 * <li>A {@link StackPane} as the root layout.</li>
 * <li>A {@link BorderPane} containing the Navigation Sidebar and Content Area
 * (Base Layer).</li>
 * <li>A {@link ModalPane} for global dialogs (Overlay Layer).</li>
 * <li>A {@link VBox} for notifications (Topmost Layer).</li>
 * </ul>
 */
public final class MainView extends AbstractView {

	private static final String STYLESHEET_PATH = System.getProperty("corese.styles.mainView",
			"/css/features/main-view.css");

	// Layout Containers
	private final VBox navigationContainer = new VBox();
	private final BorderPane contentArea = new BorderPane();
	private final VBox notificationContainer = new VBox(10);
	private final ModalPane modalPane = new ModalPane();

	public MainView() {
		super(new StackPane(), STYLESHEET_PATH);
		initializeLayout();
	}

	private void initializeLayout() {
		StackPane root = (StackPane) getRoot();

		// 1. Main Layout (Sidebar + Content)
		BorderPane mainLayout = new BorderPane();
		mainLayout.setLeft(navigationContainer);
		mainLayout.setCenter(contentArea);

		// 2. Notification Layer
		notificationContainer.setAlignment(Pos.BOTTOM_RIGHT);
		notificationContainer.setPickOnBounds(false); // Allow clicks through transparent areas

		// 3. Assemble Root Stack
		// Order matters: Bottom -> Top
		root.getChildren().addAll(mainLayout, modalPane, notificationContainer);

		StackPane.setAlignment(notificationContainer, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(notificationContainer, new Insets(20));

		// 4. Initialize Global Managers
		ModalService.getInstance().setModalPane(modalPane);
		NotificationWidget.getInstance().setContainer(notificationContainer);
	}

	public void setNavigationRoot(Parent navigationRoot) {
		navigationContainer.getChildren().setAll(navigationRoot);
		VBox.setVgrow(navigationRoot, Priority.ALWAYS);
	}

	public void setContent(AbstractView contentView) {
		contentArea.setCenter(contentView.getRoot());
	}
}
