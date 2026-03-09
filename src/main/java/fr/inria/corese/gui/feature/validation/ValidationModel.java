package fr.inria.corese.gui.feature.validation;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.core.service.ShaclService;

/**
 * Model for the Validation feature.
 *
 * <p>
 * Handles SHACL validation lifecycle and report formatting for the active tab.
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
	 * @param shapesContent
	 *            SHACL shapes content in Turtle format
	 * @return validation result
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
	 * @param format
	 *            desired serialization format
	 * @return formatted report, or null when no report is available
	 */
	public String formatLastReport(SerializationFormat format) {
		if (format == null || lastResult == null || lastResult.getReportId() == null) {
			return null;
		}
		return ShaclService.getInstance().formatReport(lastResult.getReportId(), format);
	}

	/**
	 * Backward-compatible format API for UI callbacks based on labels.
	 *
	 * @param format
	 *            format label (e.g. "Turtle")
	 * @return formatted report, or null when no report is available
	 */
	public String formatLastReport(String format) {
		return formatLastReport(SerializationFormat.fromString(format));
	}

	/**
	 * Returns the triple count of the last cached SHACL report graph.
	 *
	 * @return report triple count, 0 when unavailable
	 */
	public int getLastReportTripleCount() {
		if (lastResult == null || lastResult.getReportId() == null) {
			return 0;
		}
		return ShaclService.getInstance().getReportTripleCount(lastResult.getReportId());
	}

	/**
	 * Gets the last validation result.
	 *
	 * @return validation result
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
