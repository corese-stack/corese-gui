package fr.inria.corese.gui.core.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import javafx.concurrent.Task;

/**
 * Service for loading file contents asynchronously.
 *
 * <p>This service handles file I/O operations in a non-blocking manner, ensuring that the UI
 * remains responsive even when loading large files or files from slow storage/network.
 *
 * <p><b>Responsibilities:</b>
 *
 * <ul>
 *   <li>Abstract file I/O operations from controllers
 *   <li>Provide asynchronous file loading via JavaFX Task
 *   <li>Handle encoding and file system details
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * Task<String> task = FileLoaderService.loadFileAsync(file);
 * task.setOnSucceeded(event -> {
 *     String content = task.getValue();
 *     // Process content
 * });
 * task.setOnFailed(event -> {
 *     Throwable error = task.getException();
 *     // Handle error
 * });
 * new Thread(task).start();
 * }</pre>
 */
public class FileLoaderService {

  private FileLoaderService() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a Task that loads file content asynchronously.
   *
   * <p>The task reads the entire file content into a String using UTF-8 encoding. This method does
   * not start the task; the caller is responsible for executing it in a background thread.
   *
   * <p><b>Important:</b> The returned task should be executed in a daemon thread to avoid
   * preventing application shutdown.
   *
   * @param file The file to load
   * @return A Task that will read the file content when executed
   * @throws NullPointerException if file is null
   */
  public static Task<String> loadFileAsync(File file) {
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
   * <p><b>Warning:</b> This method blocks the calling thread until the file is fully read. Should
   * only be used when blocking is acceptable (e.g., in background threads, tests, or for small
   * files where async is overkill).
   *
   * @param file The file to load
   * @return The file content as a String
   * @throws IOException if the file cannot be read
   * @throws NullPointerException if file is null
   */
  public static String loadFileSync(File file) throws IOException {
    if (file == null) {
      throw new NullPointerException("File cannot be null");
    }
    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
  }
}
