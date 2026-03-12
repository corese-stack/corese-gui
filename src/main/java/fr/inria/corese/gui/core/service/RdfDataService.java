package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadFormat;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.gui.core.io.DemoHttpFallbackSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Locale;

/**
 * Service for loading RDF data into the shared graph.
 *
 * <p>
 * This service provides a clean API for RDF data operations, handling I/O and
 * format detection while delegating actual graph operations to the
 * {@link GraphStoreService}.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 * <li>Only one data loading service should coordinate access to the shared
 * graph</li>
 * <li>Prevents concurrent loading issues and resource conflicts</li>
 * <li>Maintains consistent error handling and logging across the
 * application</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * RdfDataService service = RdfDataService.getInstance();
 * try {
 * 	service.loadFile(new File("data.ttl"));
 * } catch (RdfLoadException e) {
 * 	// Handle loading error
 * }
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for coordinated graph access
public class RdfDataService {

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private static final Logger LOGGER = LoggerFactory.getLogger(RdfDataService.class);
	private static final RdfDataService INSTANCE = new RdfDataService();
	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int READ_TIMEOUT_MS = 30_000;
	private static final int READ_LOCK_PARSE_RETRY_MAX_ATTEMPTS = 4;
	private static final long READ_LOCK_PARSE_RETRY_DELAY_MS = 120L;
	private static final String READ_LOCK_PARSING_ERROR_TOKEN = "Read lock while parsing";
	private static final String ACCEPT_HEADER = String.join(", ", "text/turtle", "application/ld+json",
			"application/rdf+xml", "application/n-triples", "application/trig", "text/html", "application/xhtml+xml",
			"*/*");

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	private RdfDataService() {
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Returns the singleton instance of the RDF data service.
	 *
	 * @return The RdfDataService instance.
	 */
	public static RdfDataService getInstance() {
		return INSTANCE;
	}

	/**
	 * Loads an RDF file into the shared graph.
	 *
	 * <p>
	 * Automatically detects the RDF format based on file extension. Supported
	 * formats include Turtle, RDF/XML, N-Triples, JSON-LD, and others.
	 *
	 * @param file
	 *            The RDF file to load.
	 * @throws IllegalArgumentException
	 *             if the file is null or doesn't exist.
	 * @throws RdfLoadException
	 *             if the file cannot be read or parsed.
	 */
	@SuppressWarnings("java:S2139")
	public void loadFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File cannot be null.");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
		}

		LOGGER.info("Loading RDF file: {}", file.getAbsolutePath());

		fr.inria.corese.core.api.Loader.format format = resolveLoadFormat(file.getName());
		if (format == fr.inria.corese.core.api.Loader.format.UNDEF_FORMAT) {
			LOGGER.warn("Could not detect format from extension for {}, Corese will attempt auto-detection.",
					file.getName());
		}

		try {
			parseWithRetry(file.getAbsolutePath(), format, () -> new FileInputStream(file));
			LOGGER.info("Successfully loaded {} triples from file.", GraphStoreService.getInstance().size());
		} catch (IOException | LoadException e) {
			String errorMsg = String.format("Failed to load RDF file '%s': %s", file.getName(), e.getMessage());
			LOGGER.error(errorMsg, e);
			throw new RdfLoadException(errorMsg, e);
		} catch (Throwable throwable) {
			rethrowIfFatal(throwable);
			String details = errorDetails(throwable);
			String errorMsg = String.format("Failed to load RDF file '%s': %s", file.getName(), details);
			LOGGER.error(errorMsg, throwable);
			throw new RdfLoadException(errorMsg, throwable);
		}
	}

	/**
	 * Loads RDF data from a remote URI into the shared graph.
	 *
	 * <p>
	 * The RDF format is automatically detected from extension and/or content-type
	 * when available.
	 *
	 * @param uriString
	 *            the URI to load
	 * @throws IllegalArgumentException
	 *             if URI is null/blank or invalid
	 * @throws RdfLoadException
	 *             if loading/parsing fails
	 */
	@SuppressWarnings("java:S2139")
	public void loadUri(String uriString) {
		if (uriString == null || uriString.isBlank()) {
			throw new IllegalArgumentException("URI cannot be empty.");
		}

		String normalizedUri = uriString.trim();
		URI uri;
		try {
			uri = URI.create(normalizedUri);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid URI: " + normalizedUri, e);
		}

		if (uri.getScheme() == null || uri.getScheme().isBlank()) {
			throw new IllegalArgumentException("URI must include a scheme (e.g., http, https, file): " + normalizedUri);
		}

		LOGGER.info("Loading RDF URI: {}", normalizedUri);
		fr.inria.corese.core.api.Loader.format format = resolveLoadFormat(uri.getPath());
		try {
			parseWithRetry(normalizedUri, format, () -> openUriStream(uri));
			LOGGER.info("Successfully loaded {} triples after URI load.", GraphStoreService.getInstance().size());
		} catch (IOException | LoadException e) {
			String details = DemoHttpFallbackSupport.isSslHandshakeFailure(e)
					? "TLS certificate validation failed. The app retries HTTP only for known demo links under "
							+ DemoHttpFallbackSupport.demoHost() + DemoHttpFallbackSupport.demoPathPrefix()
							+ ". Otherwise, fix the JVM truststore or use http:// when available."
					: e.getMessage();
			String errorMsg = String.format("Failed to load RDF URI '%s': %s", normalizedUri, details);
			LOGGER.error(errorMsg, e);
			throw new RdfLoadException(errorMsg, e);
		} catch (Throwable throwable) {
			rethrowIfFatal(throwable);
			String details = DemoHttpFallbackSupport.isSslHandshakeFailure(throwable)
					? "TLS certificate validation failed. The app retries HTTP only for known demo links under "
							+ DemoHttpFallbackSupport.demoHost() + DemoHttpFallbackSupport.demoPathPrefix()
							+ ". Otherwise, fix the JVM truststore or use http:// when available."
					: errorDetails(throwable);
			String errorMsg = String.format("Failed to load RDF URI '%s': %s", normalizedUri, details);
			LOGGER.error(errorMsg, throwable);
			throw new RdfLoadException(errorMsg, throwable);
		}
	}

	private InputStream openUriStream(URI uri) throws IOException {
		try {
			return openUriStreamInternal(uri);
		} catch (IOException primaryFailure) {
			URI fallbackUri = DemoHttpFallbackSupport.resolveUriAfterSslFailure(uri, primaryFailure);
			if (fallbackUri == null) {
				throw primaryFailure;
			}
			LOGGER.warn("TLS validation failed for {}. Retrying with HTTP fallback {} for known demo host {}.", uri,
					fallbackUri, DemoHttpFallbackSupport.demoHost());
			return openUriStreamInternal(fallbackUri);
		}
	}

	private InputStream openUriStreamInternal(URI uri) throws IOException {
		URLConnection connection = uri.toURL().openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setRequestProperty("Accept", ACCEPT_HEADER);
		return connection.getInputStream();
	}

	private fr.inria.corese.core.api.Loader.format resolveLoadFormat(String sourceNameOrPath) {
		fr.inria.corese.core.api.Loader.format format = LoadFormat.getFormat(sourceNameOrPath);
		if (format != fr.inria.corese.core.api.Loader.format.UNDEF_FORMAT) {
			return format;
		}
		String extension = extractExtension(sourceNameOrPath);
		if (".htm".equals(extension)) {
			return fr.inria.corese.core.api.Loader.format.RDFA_FORMAT;
		}
		return fr.inria.corese.core.api.Loader.format.UNDEF_FORMAT;
	}

	private void parseWithRetry(String sourceLabel, fr.inria.corese.core.api.Loader.format format,
			CheckedInputStreamSupplier streamSupplier) throws IOException, LoadException {
		int attempt = 1;
		while (true) {
			try (InputStream stream = streamSupplier.get()) {
				Load loader = Load.create(GraphStoreService.getInstance().getGraph());
				parseWithBestFormat(loader, stream, format);
				return;
			} catch (LoadException loadException) {
				if (!shouldRetryReadLockConflict(sourceLabel, attempt, loadException)) {
					throw loadException;
				}
				attempt++;
			}
		}
	}

	private boolean shouldRetryReadLockConflict(String sourceLabel, int attempt, LoadException loadException) {
		if (!isReadLockParsingConflict(loadException)) {
			return false;
		}
		if (attempt >= READ_LOCK_PARSE_RETRY_MAX_ATTEMPTS) {
			LOGGER.warn("Read-lock conflict while parsing {} persisted after {} attempts.", sourceLabel, attempt);
			return false;
		}
		long delay = READ_LOCK_PARSE_RETRY_DELAY_MS * attempt;
		LOGGER.warn("Read-lock conflict while parsing {} (attempt {}/{}). Retrying in {} ms.", sourceLabel, attempt,
				READ_LOCK_PARSE_RETRY_MAX_ATTEMPTS, delay);
		try {
			Thread.sleep(delay);
			return true;
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private boolean isReadLockParsingConflict(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.contains(READ_LOCK_PARSING_ERROR_TOKEN)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private void parseWithBestFormat(Load loader, InputStream stream, fr.inria.corese.core.api.Loader.format format)
			throws LoadException {
		if (format == fr.inria.corese.core.api.Loader.format.UNDEF_FORMAT) {
			loader.parse(stream);
			return;
		}
		loader.parse(stream, format);
	}

	private String extractExtension(String sourceNameOrPath) {
		if (sourceNameOrPath == null || sourceNameOrPath.isBlank()) {
			return "";
		}
		int dotIndex = sourceNameOrPath.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex >= sourceNameOrPath.length() - 1) {
			return "";
		}
		return sourceNameOrPath.substring(dotIndex).toLowerCase(Locale.ROOT);
	}

	private String errorDetails(Throwable throwable) {
		if (throwable == null) {
			return "Unknown error.";
		}
		String message = throwable.getMessage();
		if (message != null && !message.isBlank()) {
			return message;
		}
		return throwable.getClass().getSimpleName();
	}

	private void rethrowIfFatal(Throwable throwable) {
		if (throwable instanceof VirtualMachineError virtualMachineError) {
			throw virtualMachineError;
		}
		if (throwable instanceof ThreadDeath threadDeath) {
			throw threadDeath;
		}
		if (throwable instanceof LinkageError linkageError) {
			throw linkageError;
		}
	}

	@FunctionalInterface
	private interface CheckedInputStreamSupplier {
		InputStream get() throws IOException;
	}

	/**
	 * Clears all RDF data from the graph.
	 *
	 * <p>
	 * This operation removes all triples from the graph but keeps the graph
	 * instance intact.
	 */
	public void clearData() {
		LOGGER.info("Clearing all RDF data from graph.");
		GraphStoreService.getInstance().clear();
	}

	/**
	 * Checks if the graph contains any RDF data.
	 *
	 * @return true if the graph has at least one triple, false otherwise.
	 */
	public boolean hasData() {
		return GraphStoreService.getInstance().hasData();
	}

	/**
	 * Returns the number of triples currently in the graph.
	 *
	 * @return The count of RDF triples.
	 */
	public int getTripleCount() {
		return GraphStoreService.getInstance().size();
	}

	// ==============================================================================================
	// Exception Classes
	// ==============================================================================================

	/**
	 * Exception thrown when RDF file loading fails.
	 */
	public static class RdfLoadException extends RuntimeException {
		public RdfLoadException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
