package fr.inria.corese.gui.core.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.shacl.Shacl;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;

/**
 * Service for performing SHACL validation on the shared Corese graph.
 *
 * <p>
 * This service handles SHACL shape parsing, validation execution, and report
 * caching. It provides a simple API for validating RDF data against SHACL
 * constraints.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 * <li>Validation operations must be coordinated to prevent conflicts</li>
 * <li>Report cache should be centrally managed</li>
 * <li>Ensures consistent validation behavior across the application</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * ShaclService service = ShaclService.getInstance();
 * String shapes = "# SHACL shapes in Turtle format...";
 * ValidationResult result = service.validate(shapes);
 * if (!result.conforms()) {
 * 	String report = service.formatReport(result.reportId(), SerializationFormat.TURTLE);
 * }
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for validation coordination
public class ShaclService {

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private static final Logger LOGGER = LoggerFactory.getLogger(ShaclService.class);
	private static final ShaclService INSTANCE = new ShaclService();

	private final Map<String, Graph> reportCache;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	private ShaclService() {
		this.reportCache = new ConcurrentHashMap<>();
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Returns the singleton instance of the SHACL service.
	 *
	 * @return The ShaclService instance.
	 */
	public static ShaclService getInstance() {
		return INSTANCE;
	}

	/**
	 * Validates the current data in the GraphStoreService against the provided
	 * SHACL shapes.
	 *
	 * @param shapesContent
	 *            The SHACL shapes definition in Turtle format.
	 * @return A {@link ValidationResult} summary.
	 */
	public ValidationResult validate(String shapesContent) {
		if (shapesContent == null || shapesContent.isBlank()) {
			return new ValidationResult(false, null, "Shapes content is empty.");
		}

		try {
			// 1. Prepare Data Graph (from Store)
			Graph dataGraph = GraphStoreService.getInstance().getGraph();

			// 2. Parse Shapes Graph
			Graph shapesGraph = Graph.create();
			Load.create(shapesGraph).parse(new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)),
					Loader.format.TURTLE_FORMAT);

			// 3. Run Validation
			Shacl shacl = new Shacl(dataGraph, shapesGraph);
			Graph reportGraph = shacl.eval();
			boolean conforms = shacl.conform(reportGraph);

			// 4. Cache Report
			String reportId = UUID.randomUUID().toString();
			reportCache.put(reportId, reportGraph);

			LOGGER.info("SHACL validation done. Conforms: {}, ReportID: {}", conforms, reportId);
			return new ValidationResult(conforms, reportId, null);

		} catch (LoadException | EngineException | RuntimeException e) {
			LOGGER.error("SHACL validation exception", e);
			return new ValidationResult(false, null, buildValidationErrorMessage(e));
		}
	}

	/**
	 * Formats the validation report graph.
	 *
	 * @param reportId
	 *            The ID of the report.
	 * @param format
	 *            The target serialization format.
	 * @return Formatted report or error string.
	 */
	public String formatReport(String reportId, SerializationFormat format) {
		Graph reportGraph = reportCache.get(reportId);
		if (reportGraph == null)
			return "Error: Report not found (ID: " + reportId + ")";

		return ResultFormatter.getInstance().formatGraph(reportGraph, format);
	}

	/**
	 * Releases a cached report.
	 */
	public void releaseReport(String reportId) {
		if (reportId != null) {
			reportCache.remove(reportId);
		}
	}

	private static String buildValidationErrorMessage(Throwable throwable) {
		if (throwable == null) {
			return "Validation failed: unknown error.";
		}
		String message = throwable.getMessage();
		Throwable cause = throwable.getCause();
		while ((message == null || message.isBlank()) && cause != null) {
			message = cause.getMessage();
			cause = cause.getCause();
		}
		if (message == null || message.isBlank()) {
			return "Validation failed: " + throwable.getClass().getSimpleName();
		}
		return "Validation failed: " + message;
	}
}
