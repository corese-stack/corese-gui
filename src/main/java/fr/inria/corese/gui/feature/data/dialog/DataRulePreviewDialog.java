package fr.inria.corese.gui.feature.data.dialog;

import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Modal dialog displaying one rule file as read-only text.
 */
public final class DataRulePreviewDialog {

	private static final String STYLESHEET = "/css/features/data-rule-preview-dialog.css";
	private static final String STYLE_CLASS_DIALOG = "data-rule-preview-dialog";
	private static final String STYLE_CLASS_CONTENT = "data-rule-preview-content";
	private static final String STYLE_CLASS_PATH = "data-rule-preview-path";
	private static final String STYLE_CLASS_EDITOR = "data-rule-preview-editor";
	private static final String DIALOG_SUBTITLE = "Read-only source preview.";

	private DataRulePreviewDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows a read-only preview for one rule source.
	 *
	 * @param ruleLabel
	 *            display label
	 * @param sourcePath
	 *            source path
	 * @param sourceContent
	 *            rule source content
	 */
	public static void show(String ruleLabel, String sourcePath, String sourceContent) {
		String safeLabel = (ruleLabel == null || ruleLabel.isBlank()) ? "Rule" : ruleLabel;
		String safePath = (sourcePath == null || sourcePath.isBlank()) ? "Unknown source" : sourcePath;
		String safeContent = sourceContent == null ? "" : sourceContent;

		Label pathLabel = new Label(safePath);
		pathLabel.getStyleClass().add(STYLE_CLASS_PATH);
		pathLabel.setWrapText(true);

		CodeMirrorWidget previewEditor = new CodeMirrorWidget(true);
		previewEditor.getStyleClass().add(STYLE_CLASS_EDITOR);
		previewEditor.setMode(detectRulePreviewMode(safeContent, safePath));
		previewEditor.setContent(safeContent);
		RoundedClipSupport.applyRoundedClip(previewEditor, 8);
		VBox.setVgrow(previewEditor, Priority.ALWAYS);

		VBox content = new VBox(8, pathLabel, previewEditor);
		content.getStyleClass().add(STYLE_CLASS_CONTENT);
		content.setPadding(new Insets(0));

		Button closeButton = new Button("Close");
		closeButton.setCancelButton(true);
		closeButton.setOnAction(event -> {
			previewEditor.close();
			ModalService.getInstance().hide();
		});

		DialogLayout dialog = new DialogLayout("Rule Preview - " + safeLabel, DIALOG_SUBTITLE, content, closeButton);
		dialog.getStyleClass().add(STYLE_CLASS_DIALOG);
		CssUtils.applyViewStyles(dialog, STYLESHEET);
		dialog.sceneProperty().addListener((observable, previousScene, currentScene) -> {
			if (currentScene == null) {
				previewEditor.close();
			}
		});
		ModalService.getInstance().show(dialog);
	}

	private static SerializationFormat detectRulePreviewMode(String content, String sourcePath) {
		String safeContent = content == null ? "" : content;
		String normalizedContent = safeContent.stripLeading().toLowerCase(Locale.ROOT);
		String normalizedPath = sourcePath == null ? "" : sourcePath.trim().toLowerCase(Locale.ROOT);

		boolean looksLikeXmlRule = normalizedContent.startsWith("<?xml") || normalizedContent.startsWith("<rdf:rdf")
				|| normalizedContent.startsWith("<rule")
				|| (normalizedContent.contains("<rdf:rdf") && normalizedContent.contains("</rdf:rdf>"))
				|| normalizedContent.contains("<![cdata[");
		boolean xmlExtension = normalizedPath.endsWith(".xml") || normalizedPath.endsWith(".rdf")
				|| normalizedPath.endsWith(".owl");

		if (looksLikeXmlRule || xmlExtension) {
			return SerializationFormat.XML;
		}
		return SerializationFormat.SPARQL_QUERY;
	}
}
