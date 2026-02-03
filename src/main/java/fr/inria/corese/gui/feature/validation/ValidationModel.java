package fr.inria.corese.gui.feature.validation;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.manager.CoreseGraphManager;
import fr.inria.corese.gui.core.manager.ShaclManager;
import fr.inria.corese.gui.core.model.ValidationReportItem;
import java.util.List;

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
    public boolean isDataLoaded() {
        return CoreseGraphManager.getInstance().isDataLoaded();
    }

    /**
     * Validates the current data graph against the provided shapes content.
     *
     * @param shapesContent The SHACL shapes in Turtle format.
     * @return The result of the validation.
     */
    public ValidationResult validate(String shapesContent) {
        if (this.lastResult != null) {
            ShaclManager.getInstance().releaseReport(this.lastResult.getReportId());
        }
        this.lastResult = ShaclManager.getInstance().validate(shapesContent);
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
        return ShaclManager.getInstance().formatReport(
            lastResult.getReportId(),
            SerializationFormat.fromString(format)
        );
    }

    /**
     * Retrieves the validation report items from the last result.
     *
     * @return A list of ValidationReportItem objects.
     */
    public List<ValidationReportItem> getValidationReportItems() {
        if (lastResult == null || lastResult.getReportId() == null) {
            return List.of();
        }
        return ShaclManager.getInstance().extractReportItems(lastResult.getReportId());
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
            ShaclManager.getInstance().releaseReport(lastResult.getReportId());
            lastResult = null;
        }
    }
}
