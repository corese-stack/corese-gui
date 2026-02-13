package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.input.UriInputListWidget;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.service.ModalService;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Modal dialog that collects one or more RDF URIs to load.
 */
public final class DataUriLoadDialog {

	private static final String DIALOG_TITLE = "Load RDF from URI";
	private static final String DIALOG_SUBTITLE = "Provide one or more RDF URIs to load into the graph.";
	private static final String URI_PROMPT = "http://example.org/data.ttl";
	private static final String INVALID_URI_MESSAGE = "Please fix invalid URI(s) highlighted in red.";
	private static final String EMPTY_URI_MESSAGE = "Please provide at least one URI.";

	private DataUriLoadDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows the URI loading dialog.
	 *
	 * @param onLoadRequested
	 *            callback receiving non-empty distinct URIs when user confirms
	 */
	public static void show(Consumer<List<String>> onLoadRequested) {
		Objects.requireNonNull(onLoadRequested, "onLoadRequested must not be null");

		UriInputListWidget uriInputWidget = new UriInputListWidget(URI_PROMPT);

		VBox content = new VBox(uriInputWidget);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button loadButton = new Button("Load");
		loadButton.getStyleClass().add(Styles.ACCENT);
		loadButton.setOnAction(event -> {
			if (uriInputWidget.highlightInvalidUris()) {
				uriInputWidget.showValidationError(INVALID_URI_MESSAGE);
				return;
			}
			List<String> urisToLoad = uriInputWidget.getDistinctNonBlankUris();
			if (urisToLoad.isEmpty()) {
				uriInputWidget.showValidationError(EMPTY_URI_MESSAGE);
				return;
			}
			uriInputWidget.clearValidationError();
			ModalService.getInstance().hide();
			onLoadRequested.accept(urisToLoad);
		});

		ModalService.getInstance()
				.show(new DialogLayout(DIALOG_TITLE, DIALOG_SUBTITLE, content, cancelButton, loadButton));
	}
}
