package fr.inria.corese.gui.feature.data.dialog;

import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Modal dialog that lets users select rule files to reload.
 */
public final class DataReloadRuleFilesDialog {

	private static final String STYLESHEET = "/css/features/data-reload-sources-dialog.css";
	private static final String STYLE_CLASS_DIALOG = "data-reload-dialog";
	private static final String STYLE_CLASS_CONTENT = "data-reload-dialog-content";
	private static final String STYLE_CLASS_RULE_SCROLL = "data-reload-dialog-source-scroll";
	private static final String STYLE_CLASS_RULE_LIST = "data-reload-dialog-source-list";
	private static final String STYLE_CLASS_RULE_ITEM = "data-reload-dialog-source-item";
	private static final String STYLE_CLASS_INLINE_ERROR = "data-reload-dialog-inline-error";

	private static final String DIALOG_TITLE = "Reload Rule Files";
	private static final String DIALOG_SUBTITLE = "Select rule files to reapply.";
	private static final String EMPTY_SELECTION_MESSAGE = "Select at least one rule file to reload.";
	private static final int RULE_VIEWPORT_HEIGHT = 240;
	private static final int RULE_MIN_VIEWPORT_HEIGHT = 120;
	private static final String VALIDATION_ANIMATION_KEY = "reloadRuleFilesValidationAnimation";
	private static final String BUTTON_ANIMATION_KEY = "reloadRuleFilesButtonAnimation";

	private DataReloadRuleFilesDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows the rule-file selection dialog for reload.
	 *
	 * @param ruleFiles
	 *            loaded rule files
	 * @param onReloadRequested
	 *            callback invoked with selected rule files when user confirms
	 */
	public static void show(List<RuleFileState> ruleFiles, Consumer<List<RuleFileState>> onReloadRequested) {
		Objects.requireNonNull(onReloadRequested, "onReloadRequested must not be null");
		List<RuleFileState> safeRules = ruleFiles == null
				? List.of()
				: ruleFiles.stream().filter(rule -> rule != null && rule.id() != null && !rule.id().isBlank()).toList();

		VBox ruleList = new VBox(8);
		ruleList.getStyleClass().add(STYLE_CLASS_RULE_LIST);
		ruleList.setFillWidth(true);

		Map<CheckBox, RuleFileState> ruleSelections = new LinkedHashMap<>();
		for (RuleFileState rule : safeRules) {
			CheckBox checkBox = createRuleCheckBox(rule, ruleList);
			ruleSelections.put(checkBox, rule);
			ruleList.getChildren().add(checkBox);
		}

		ScrollPane ruleScroll = new ScrollPane(ruleList);
		ruleScroll.getStyleClass().add(STYLE_CLASS_RULE_SCROLL);
		ruleScroll.setFitToWidth(true);
		ruleScroll.setPrefViewportHeight(RULE_VIEWPORT_HEIGHT);
		ruleScroll.setMinViewportHeight(RULE_MIN_VIEWPORT_HEIGHT);
		ruleScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		ruleScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		ruleScroll.setFitToHeight(false);

		VBox content = new VBox(10, ruleScroll);
		content.getStyleClass().add(STYLE_CLASS_CONTENT);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button reloadButton = new Button("Reload");
		Label selectionErrorLabel = new Label(EMPTY_SELECTION_MESSAGE);
		selectionErrorLabel.getStyleClass().add(STYLE_CLASS_INLINE_ERROR);
		selectionErrorLabel.setWrapText(false);
		DialogSelectionStateSupport.prepareCollapsedValidationLabel(selectionErrorLabel);

		Runnable updateSelectionState = () -> {
			boolean anySelected = ruleSelections.keySet().stream().anyMatch(CheckBox::isSelected);
			DialogSelectionStateSupport.updateActionState(anySelected, reloadButton, selectionErrorLabel,
					EMPTY_SELECTION_MESSAGE, VALIDATION_ANIMATION_KEY, BUTTON_ANIMATION_KEY);
		};
		ruleSelections.keySet().forEach(checkBox -> checkBox.selectedProperty()
				.addListener((obs, oldValue, newValue) -> updateSelectionState.run()));
		updateSelectionState.run();

		reloadButton.setOnAction(event -> {
			List<RuleFileState> selectedRules = ruleSelections.entrySet().stream()
					.filter(entry -> entry.getKey().isSelected()).map(Map.Entry::getValue).toList();
			if (selectedRules.isEmpty()) {
				DialogSelectionStateSupport.showValidationMessage(selectionErrorLabel, EMPTY_SELECTION_MESSAGE,
						VALIDATION_ANIMATION_KEY);
				return;
			}
			ModalService.getInstance().hide();
			onReloadRequested.accept(selectedRules);
		});

		DialogLayout dialog = new DialogLayout(DIALOG_TITLE, DIALOG_SUBTITLE, content, selectionErrorLabel,
				cancelButton, reloadButton);
		dialog.getStyleClass().add(STYLE_CLASS_DIALOG);
		CssUtils.applyViewStyles(dialog, STYLESHEET);
		ModalService.getInstance().show(dialog);
	}

	private static CheckBox createRuleCheckBox(RuleFileState rule, VBox ruleList) {
		CheckBox checkBox = new CheckBox();
		checkBox.setSelected(rule.enabled());
		checkBox.setMaxWidth(Double.MAX_VALUE);
		checkBox.prefWidthProperty().bind(ruleList.widthProperty());
		checkBox.getStyleClass().add(STYLE_CLASS_RULE_ITEM);
		DataReloadDialogLabelSupport.applyCheckBoxDisplay(checkBox, ruleList,
				DataReloadDialogLabelSupport.describeRuleFile(rule));
		return checkBox;
	}
}
