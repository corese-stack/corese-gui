package fr.inria.corese.gui.feature.validation;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.core.service.ShaclService;

/**
 * Model for the Validation feature.
 *
 * <p>Handles the core logic for SHACL validation, including:
 * <ul>
 *   <li>Validating RDF data against SHACL shapes.</li>
 *   <li>Formatting validation reports.</li>
 *   <li>Extracting report items for display.</li>
 * </ul>
 */
public class ValidationModel {

    private ValidationResult lastResult;

    /**
     * Checks if there is data loaded in the graph to validate.
     *
     * @return true if data is loaded, false otherwise.
     */
    public boolean hasData() {
        return RdfDataService.getInstance().hasData();
    }

    /**
     * Validates the current data graph against the provided shapes content.
     *
     * @param shapesContent The SHACL shapes in Turtle format.
     * @return The result of the validation.
     */
    public ValidationResult validate(String shapesContent) {
        if (this.lastResult != null) {
            ShaclService.getInstance().releaseReport(this.lastResult.getReportId());
        }
        this.lastResult = ShaclService.getInstance().validate(shapesContent);
        return this.lastResult;
    }

    /**
     * Formats the last report graph into the specified format.
     *
     * @param format The desired format (e.g., "TURTLE", "JSON-LD").
     * @return The formatted string, or null if no report exists.
     */
    public String formatLastReport(String format) {
        if (lastResult == null || lastResult.getReportId() == null) {
            return null;
        }
        return ShaclService.getInstance().formatReport(
            lastResult.getReportId(),
            SerializationFormat.fromString(format)
        );
    }

    /**
     * Gets the last validation result.
     *
     * @return The validation result.
     */
    public ValidationResult getLastResult() {
        return lastResult;
    }

    /** Releases cached validation report resources. */
    public void dispose() {
        if (lastResult != null) {
            ShaclService.getInstance().releaseReport(lastResult.getReportId());
            lastResult = null;
        }
    }
}