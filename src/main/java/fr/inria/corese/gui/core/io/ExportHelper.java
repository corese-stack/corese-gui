package fr.inria.corese.gui.core.io;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;

/**
 * Utility class to handle common file export operations.
 *
 * <p>
 * Standardizes the behavior of "Export" features across the application:
 *
 * <ul>
 * <li>Consistent FileChooser configuration
 * <li>Automatic extension handling
 * <li>Asynchronous writing to prevent UI freezing
 * <li>Standardized success/error feedback
 * </ul>
 */
public final class ExportHelper {

	private ExportHelper() {
		// Utility class
	}

	/**
	 * Prompts the user to save text content to a file with the specified format.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param content
	 *            The text content to save
	 * @param format
	 *            The target serialization format
	 */
	public static void exportText(Window window, String content, SerializationFormat format) {
		// Treat null as empty content and allow exporting empty files (valid use case)
		String contentToExport = (content != null) ? content : "";

		SerializationFormat safeFormat = (format != null) ? format : SerializationFormat.TURTLE;

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Result As");

		String extension = safeFormat.getExtension();
		String baseName = DefaultFileNameResolver.resultBaseName(safeFormat);
		fileChooser.setInitialFileName(baseName);
		FileDialogState.applyInitialDirectory(fileChooser);

		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				safeFormat.getLabel() + " file (*" + extension + ")", "*" + extension);

		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

		File file = fileChooser.showSaveDialog(window);

		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			// Enforce extension
			if (!file.getName().toLowerCase().endsWith(extension)) {
				file = new File(file.getAbsolutePath() + extension);
			}

			writeFileAsync(file, contentToExport);
		}
	}

	/**
	 * Prompts the user to save content, allowing selection from multiple supported
	 * formats.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param formats
	 *            List of supported serialization formats
	 * @param contentProvider
	 *            Function that generates the content string for a selected format
	 */
	public static void exportResult(Window window, List<SerializationFormat> formats,
			Function<SerializationFormat, String> contentProvider) {

		if (formats == null || formats.isEmpty()) {
			return;
		}

		FileChooser fileChooser = createFileChooser(formats);
		File file = fileChooser.showSaveDialog(window);

		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			SerializationFormat selectedFormat = determineFormat(file, fileChooser, formats);
			File finalFile = enforceExtension(file, selectedFormat);
			AppExecutors.execute(() -> {
				String content = contentProvider.apply(selectedFormat);
				if (content == null) {
					Platform.runLater(
							() -> NotificationWidget.getInstance().showError("Export Failed: No content available."));
					return;
				}
				writeFileAsync(finalFile, content);
			});
		}
	}

	private static FileChooser createFileChooser(List<SerializationFormat> formats) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Result");
		fileChooser.setInitialFileName(DefaultFileNameResolver.resultBaseName(formats));
		FileDialogState.applyInitialDirectory(fileChooser);

		for (SerializationFormat format : formats) {
			String ext = format.getExtension();
			FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(format.getLabel() + " (*" + ext + ")",
					"*" + ext);
			fileChooser.getExtensionFilters().add(filter);
		}

		return fileChooser;
	}

	private static SerializationFormat determineFormat(File file, FileChooser fileChooser,
			List<SerializationFormat> formats) {

		FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();

		if (selectedFilter != null) {
			SerializationFormat formatFromFilter = findFormatByFilter(selectedFilter, formats);
			if (formatFromFilter != null) {
				return formatFromFilter;
			}
		}

		// Fallback: check extension
		SerializationFormat formatFromExtension = findFormatByExtension(file.getName(), formats);
		return formatFromExtension != null ? formatFromExtension : formats.get(0);
	}

	private static SerializationFormat findFormatByFilter(FileChooser.ExtensionFilter filter,
			List<SerializationFormat> formats) {
		for (SerializationFormat fmt : formats) {
			if (filter.getDescription().contains(fmt.getLabel())) {
				return fmt;
			}
		}
		return null;
	}

	private static SerializationFormat findFormatByExtension(String fileName, List<SerializationFormat> formats) {
		String lowerName = fileName.toLowerCase();
		for (SerializationFormat fmt : formats) {
			if (lowerName.endsWith(fmt.getExtension())) {
				return fmt;
			}
		}
		return null;
	}

	private static File enforceExtension(File file, SerializationFormat format) {
		if (!file.getName().toLowerCase().endsWith(format.getExtension())) {
			return new File(file.getAbsolutePath() + format.getExtension());
		}
		return file;
	}

	/**
	 * Prompts the user to save SVG content to a file.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param svgContent
	 *            The SVG content to save
	 */
	public static void exportSvg(Window window, String svgContent) {
		String contentToExport = (svgContent != null) ? svgContent : "";

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Graph As SVG");
		fileChooser.setInitialFileName(DefaultFileNameResolver.graphBaseName());
		FileDialogState.applyInitialDirectory(fileChooser);

		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SVG file (*.svg)", "*.svg");

		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

		File file = fileChooser.showSaveDialog(window);

		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			if (!file.getName().toLowerCase().endsWith(".svg")) {
				file = new File(file.getAbsolutePath() + ".svg");
			}
			writeFileAsync(file, contentToExport);
		}
	}

	/**
	 * Prompts the user to export the current graph in SVG, PNG, or PDF format.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param svgContent
	 *            The SVG content to export
	 */
	public static void exportGraph(Window window, String svgContent) {
		String contentToExport = (svgContent != null) ? svgContent : "";

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Graph");
		fileChooser.setInitialFileName(DefaultFileNameResolver.graphBaseName());
		FileDialogState.applyInitialDirectory(fileChooser);

		FileChooser.ExtensionFilter svgFilter = new FileChooser.ExtensionFilter("SVG file (*.svg)", "*.svg");
		FileChooser.ExtensionFilter pngFilter = new FileChooser.ExtensionFilter("PNG image (*.png)", "*.png");
		FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF document (*.pdf)", "*.pdf");
		fileChooser.getExtensionFilters().addAll(svgFilter, pngFilter, pdfFilter,
				new FileChooser.ExtensionFilter("All Files", "*.*"));

		File file = fileChooser.showSaveDialog(window);
		if (file == null) {
			return;
		}

		FileDialogState.updateLastDirectory(file);
		GraphExportFormat format = determineGraphFormat(file, fileChooser.getSelectedExtensionFilter(), svgFilter,
				pngFilter, pdfFilter);
		File finalFile = enforceExtension(file, format.extension);

		switch (format) {
			case SVG -> writeFileAsync(finalFile, contentToExport);
			case PNG -> transcodeSvgToPng(finalFile, contentToExport);
			case PDF -> transcodeSvgToPdf(finalFile, contentToExport);
		}
	}

	private static void writeFileAsync(File file, String content) {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws Exception {
				Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
				return null;
			}

			@Override
			protected void succeeded() {
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess("File saved successfully"));
			}

			@Override
			protected void failed() {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Export Failed: " + getException().getMessage()));
			}
		};

		AppExecutors.execute(task);
	}

	private static void transcodeSvgToPng(File file, String svgContent) {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws Exception {
				TranscoderInput input = new TranscoderInput(new StringReader(svgContent));
				try (var outputStream = Files.newOutputStream(file.toPath())) {
					TranscoderOutput output = new TranscoderOutput(outputStream);
					PNGTranscoder transcoder = new PNGTranscoder();
					SvgDimensions dims = parseSvgDimensions(svgContent);
					if (dims != null) {
						transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, dims.width * 2f);
						transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, dims.height * 2f);
					}
					transcoder.transcode(input, output);
				}
				return null;
			}

			@Override
			protected void succeeded() {
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess("File saved successfully"));
			}

			@Override
			protected void failed() {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Export Failed: " + getException().getMessage()));
			}
		};

		AppExecutors.execute(task);
	}

	private static void transcodeSvgToPdf(File file, String svgContent) {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws Exception {
				TranscoderInput input = new TranscoderInput(new StringReader(svgContent));
				try (var outputStream = Files.newOutputStream(file.toPath())) {
					TranscoderOutput output = new TranscoderOutput(outputStream);
					PDFTranscoder transcoder = new PDFTranscoder();
					transcoder.transcode(input, output);
				}
				return null;
			}

			@Override
			protected void succeeded() {
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess("File saved successfully"));
			}

			@Override
			protected void failed() {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Export Failed: " + getException().getMessage()));
			}
		};

		AppExecutors.execute(task);
	}

	private static GraphExportFormat determineGraphFormat(File file, FileChooser.ExtensionFilter selectedFilter,
			FileChooser.ExtensionFilter svgFilter, FileChooser.ExtensionFilter pngFilter,
			FileChooser.ExtensionFilter pdfFilter) {
		if (selectedFilter != null) {
			if (selectedFilter == pngFilter) {
				return GraphExportFormat.PNG;
			}
			if (selectedFilter == pdfFilter) {
				return GraphExportFormat.PDF;
			}
			if (selectedFilter == svgFilter) {
				return GraphExportFormat.SVG;
			}
		}

		String name = file.getName().toLowerCase();
		if (name.endsWith(".png")) {
			return GraphExportFormat.PNG;
		}
		if (name.endsWith(".pdf")) {
			return GraphExportFormat.PDF;
		}
		return GraphExportFormat.SVG;
	}

	private static File enforceExtension(File file, String extension) {
		if (!file.getName().toLowerCase().endsWith(extension)) {
			return new File(file.getAbsolutePath() + extension);
		}
		return file;
	}

	private static SvgDimensions parseSvgDimensions(String svgContent) {
		if (svgContent == null || svgContent.isBlank()) {
			return null;
		}
		var widthMatch = java.util.regex.Pattern.compile("width=\\\"([0-9.]+)").matcher(svgContent);
		var heightMatch = java.util.regex.Pattern.compile("height=\\\"([0-9.]+)").matcher(svgContent);
		if (widthMatch.find() && heightMatch.find()) {
			try {
				float width = Float.parseFloat(widthMatch.group(1));
				float height = Float.parseFloat(heightMatch.group(1));
				if (width > 0 && height > 0) {
					return new SvgDimensions(width, height);
				}
			} catch (NumberFormatException _) {
				return null;
			}
		}
		return null;
	}

	private record SvgDimensions(float width, float height) {
	}

	private enum GraphExportFormat {
		SVG(".svg"), PNG(".png"), PDF(".pdf");

		private final String extension;

		GraphExportFormat(String extension) {
			this.extension = extension;
		}
	}
}
