package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.shacl.Shacl;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for performing SHACL validation on the shared Corese graph.
 *
 * <p>Manages parsing of SHACL shapes, execution of the validation engine,
 * and caching of validation reports for display/export.
 */
public class ShaclService {

    private static final Logger logger = LoggerFactory.getLogger(ShaclService.class);
    private static final ShaclService INSTANCE = new ShaclService();

    private final Map<String, Graph> reportCache;

    private ShaclService() {
        this.reportCache = new ConcurrentHashMap<>();
    }

    public static ShaclService getInstance() {
        return INSTANCE;
    }

    // ============================================================================================
    // Public API
    // ============================================================================================

    /**
     * Validates the current data in the GraphStore against the provided SHACL shapes.
     *
     * @param shapesContent The SHACL shapes definition in Turtle format.
     * @return A {@link ValidationResult} summary.
     */
    public ValidationResult validate(String shapesContent) {
        if (shapesContent == null || shapesContent.isBlank()) {
            return new ValidationResult(false, null, "Shapes content is empty.");
        }

        try {
            // 1. Prepare Data Graph (from Store)
            Graph dataGraph = GraphStore.getInstance().getGraph();

            // 2. Parse Shapes Graph
            Graph shapesGraph = Graph.create();
            Load.create(shapesGraph).parse(
                new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)), 
                Load.format.TURTLE_FORMAT
            );

            // 3. Run Validation
            Shacl shacl = new Shacl(dataGraph, shapesGraph);
            Graph reportGraph = shacl.eval();
            boolean conforms = shacl.conform(reportGraph);

            // 4. Cache Report
            String reportId = UUID.randomUUID().toString();
            reportCache.put(reportId, reportGraph);

            logger.info("SHACL validation done. Conforms: {}, ReportID: {}", conforms, reportId);
            return new ValidationResult(conforms, reportId, null);

        } catch (Exception e) {
            logger.error("SHACL validation exception", e);
            return new ValidationResult(false, null, "Validation failed: " + e.getMessage());
        }
    }

    /**
     * Formats the validation report graph.
     *
     * @param reportId The ID of the report.
     * @param format   The target serialization format.
     * @return Formatted report or error string.
     */
    public String formatReport(String reportId, SerializationFormat format) {
        Graph reportGraph = reportCache.get(reportId);
        if (reportGraph == null) return "Error: Report not found (ID: " + reportId + ")";
        
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
}