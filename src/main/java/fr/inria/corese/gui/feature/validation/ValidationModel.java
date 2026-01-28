package fr.inria.corese.gui.feature.validation;

import fr.inria.corese.gui.core.manager.GraphManager;
import fr.inria.corese.gui.core.manager.ShaclManager;
import fr.inria.corese.gui.core.model.ValidationReportItem;






import java.util.List;

/**
 * Model for the Validation feature.
 * Handles the SHACL validation logic and stores the result.
 */
public class ValidationModel {

  private ValidationResult lastResult;

  /**
   * Checks if there is data loaded in the graph to validate.
   *
   * @return true if data is loaded, false otherwise.
   */
  public boolean isDataLoaded() {
    return GraphManager.getInstance().isDataLoaded();
  }

  /**
   * Validates the current data graph against the provided shapes content.
   *
   * @param shapesContent The SHACL shapes in Turtle format.
   * @return The result of the validation.
   */
  public ValidationResult validate(String shapesContent) {
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
    return ShaclManager.getInstance().formatReport(lastResult, format);
  }

  /**
   * Retrieves the validation report items from the last result.
   *
   * @return A list of ValidationReportItem objects.
   */
  public List<ValidationReportItem> getValidationReportItems() {
    return ShaclManager.getInstance().extractReportItems(lastResult);
  }

  public ValidationResult getLastResult() {
    return lastResult;
  }
}
