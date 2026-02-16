package fr.inria.corese.gui.core.io;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Utility class for handling file import operations.
 *
 * <p>
 * This is the counterpart to {@link ExportHelper}, providing standardized file
 * selection and loading operations across the application. It handles both UI
 * dialogs and low-level async file reading.
 *
 * <p>
 * <b>Key features:</b>
 *
 * <ul>
 * <li>Consistent FileChooser configuration with extension filtering
 * <li>Asynchronous file reading to prevent UI freezing
 * <li>Standardized success/error feedback with notifications
 * <li>Low-level Task creation for custom workflows
 * </ul>
 *
 * <p>
 * <b>Usage examples:</b>
 *
 *
 * @see ExportHelper for file export operations
 */
public final class ImportHelper {

	private ImportHelper() {
		// Utility class
	}

	/**
	 * Prompts the user to select and load a text file with specified extensions.
	 *
	 * <p>
	 * The file is loaded asynchronously to keep the UI responsive. On success, the
	 * content is passed to the callback on the JavaFX Application Thread.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param allowedExtensions
	 *            List of allowed file extensions (e.g., ".ttl", ".rdf")
	 * @param onSuccess
	 *            Callback to receive the loaded content
	 */
	public static void importTextFile(Window window, List<String> allowedExtensions, Consumer<String> onSuccess) {
		importTextFile(window, allowedExtensions, onSuccess, null);
	}

	/**
	 * Prompts the user to select and load a text file with specified extensions.
	 *
	 * <p>
	 * The file is loaded asynchronously to keep the UI responsive. Callbacks are
	 * invoked on the JavaFX Application Thread.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param allowedExtensions
	 *            List of allowed file extensions (e.g., ".ttl", ".rdf")
	 * @param onSuccess
	 *            Callback to receive the loaded content
	 * @param onFailure
	 *            Callback to receive error (optional, if null shows default error
	 *            notification)
	 */
	public static void importTextFile(Window window, List<String> allowedExtensions, Consumer<String> onSuccess,
			Consumer<Throwable> onFailure) {

		FileChooser fileChooser = createFileChooser(allowedExtensions);
		File file = fileChooser.showOpenDialog(window);

		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			loadFileAsync(file, onSuccess, onFailure);
		}
	}

	/**
	 * Prompts the user to select a file and returns it without loading content.
	 *
	 * <p>
	 * Useful when you need the File object itself (e.g., for tracking file path)
	 * and will handle loading separately.
	 *
	 * @param window
	 *            The parent window for the dialog
	 * @param allowedExtensions
	 *            List of allowed file extensions (e.g., ".ttl", ".rdf")
	 * @return The selected File, or null if the user cancelled
	 */
	public static File selectFile(Window window, List<String> allowedExtensions) {
		FileChooser fileChooser = createFileChooser(allowedExtensions);
		File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
		}
		return file;
	}

	/**
	 * Loads a file asynchronously and invokes callbacks on the JavaFX thread.
	 *
	 * <p>
	 * This can be used directly when you already have a File object and just need
	 * async loading with standardized feedback.
	 *
	 * @param file
	 *            The file to load
	 * @param onSuccess
	 *            Callback to receive the loaded content
	 * @param onFailure
	 *            Callback to receive error (optional, if null shows default error
	 *            notification)
	 */
	public static void loadFileAsync(File file, Consumer<String> onSuccess, Consumer<Throwable> onFailure) {

		Task<String> task = createLoadTask(file);
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading("Import",
				"Importing file " + file.getName() + "...");

		task.setOnSucceeded(event -> Platform.runLater(() -> {
			loadingHandle.close();
			if (onSuccess != null) {
				onSuccess.accept(task.getValue());
			}
			NotificationWidget.getInstance().showSuccess("Imported file: " + file.getName() + ".");
		}));

		task.setOnFailed(event -> Platform.runLater(() -> {
			loadingHandle.close();
			Throwable exception = task.getException();
			if (onFailure != null) {
				onFailure.accept(exception);
			} else {
				String message = exception != null ? exception.getMessage() : "Unknown error";
				NotificationWidget.getInstance().showErrorWithDetails("Import Error", "Import failed: " + message,
						exception);
			}
		}));

		task.setOnCancelled(event -> Platform.runLater(loadingHandle::close));

		AppExecutors.execute(task);
	}

	/**
	 * Creates a Task that loads file content asynchronously.
	 *
	 * <p>
	 * This is the low-level API for cases where you need full control over Task
	 * lifecycle and callbacks. The task reads the entire file using UTF-8 encoding.
	 *
	 * <p>
	 * <b>Important:</b> This method only creates the Task - you must start it
	 * yourself and handle all callbacks. For most use cases, prefer
	 * {@link #loadFileAsync(File, Consumer, Consumer)} which handles everything
	 * automatically.
	 *
	 * @param file
	 *            The file to load
	 * @return A Task that will read the file content when executed
	 * @throws NullPointerException
	 *             if file is null
	 */
	public static Task<String> createLoadTask(File file) {
		if (file == null) {
			throw new NullPointerException("File cannot be null");
		}

		return new Task<String>() {
			@Override
			protected String call() throws IOException {
				updateTitle("Loading " + file.getName());
				updateMessage("Reading file: " + file.getAbsolutePath());
				return Files.readString(file.toPath(), StandardCharsets.UTF_8);
			}
		};
	}

	/**
	 * Loads file content synchronously (blocking).
	 *
	 * <p>
	 * <b>Warning:</b> This method blocks the calling thread until the file is fully
	 * read. Should only be used when blocking is acceptable (e.g., already in a
	 * background thread, tests, or for small files where async overhead is
	 * unnecessary).
	 *
	 * @param file
	 *            The file to load
	 * @return The file content as a String
	 * @throws IOException
	 *             if the file cannot be read
	 * @throws NullPointerException
	 *             if file is null
	 */
	public static String loadFileSync(File file) throws IOException {
		if (file == null) {
			throw new NullPointerException("File cannot be null");
		}
		return Files.readString(file.toPath(), StandardCharsets.UTF_8);
	}

	private static FileChooser createFileChooser(List<String> allowedExtensions) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		FileDialogState.applyInitialDirectory(fileChooser);

		if (allowedExtensions != null && !allowedExtensions.isEmpty()) {
			List<String> normalized = FileTypeSupport.normalizeExtensions(allowedExtensions);
			List<String> patterns = FileTypeSupport.wildcardPatterns(normalized, false);
			String description = String.join(", ", patterns);
			FileChooser.ExtensionFilter filter = FileTypeSupport
					.createExtensionFilter("Allowed Files (" + description + ")", normalized, true);
			fileChooser.getExtensionFilters().add(filter);
			fileChooser.setSelectedExtensionFilter(filter);
		}

		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

		return fileChooser;
	}
}
