package fr.inria.corese.gui.component.pagination;

import java.util.function.Consumer;

import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * A comprehensive pagination toolbar for tables.
 * 
 * <p>
 * Provides navigation controls, rows-per-page configuration, and total row
 * count display.
 * 
 * <p>
 * Layout structure:
 * 
 * <pre>
 * [Rows/page: Input] [Spacer] [First] [Prev] "Page X / Y" [Next] [Last] [Spacer] [Total rows: N]
 * </pre>
 * 
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * TablePaginationWidget pagination = new TablePaginationWidget(pageIndex -> {
 *     // Handle page change
 *     loadPageData(pageIndex);
 * });
 * 
 * pagination.setPageCount(10);
 * pagination.setTotalRows("Total rows: 500");
 * }</pre>
 */
public class TablePaginationWidget extends HBox {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final String STYLESHEET = "/css/table-pagination-widget.css";
    private static final String STYLE_CLASS_FOOTER = "table-pagination-footer";
    private static final String STYLE_CLASS_SQUARE_INPUT = "pagination-square-input";

    private static final int SPACING = 10;
    private static final int INNER_SPACING = 5;
    private static final double MIN_INPUT_WIDTH = 45.0;
    private static final double INPUT_PADDING = 35.0;
    private static final String DEFAULT_ROWS_PER_PAGE = "50";

    private final Consumer<Integer> onPageChange;
    private int pageCount = 1;
    private int currentPageIndex = 0;
    private String lastValidRowsPerPage = DEFAULT_ROWS_PER_PAGE;

    // UI Components
    private final IconButtonWidget firstButton;
    private final IconButtonWidget prevButton;
    private final IconButtonWidget nextButton;
    private final IconButtonWidget lastButton;
    private final Label pageIndicatorLabel;
    private final TextField pageInput;
    private final TextField rowsPerPageInput;
    private final Label totalRowsLabel;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    /**
     * Creates a new pagination widget.
     * 
     * @param onPageChange Callback invoked when the page changes. Receives the new
     *                     page index (0-based).
     */
    public TablePaginationWidget(Consumer<Integer> onPageChange) {
        this.onPageChange = onPageChange;

        // Initialize UI components
        this.rowsPerPageInput = createSquareTextField(DEFAULT_ROWS_PER_PAGE);
        this.pageInput = createSquareTextField("1");
        this.pageIndicatorLabel = new Label("/ 1");
        this.totalRowsLabel = new Label("Total rows: 0");

        // Initialize navigation buttons
        this.firstButton = new IconButtonWidget(
                new ButtonConfig(ButtonIcon.FIRST_PAGE, "First Page", () -> changePage(0)));
        this.prevButton = new IconButtonWidget(
                new ButtonConfig(ButtonIcon.PREVIOUS_PAGE, "Previous Page", () -> changePage(currentPageIndex - 1)));
        this.nextButton = new IconButtonWidget(
                new ButtonConfig(ButtonIcon.NEXT_PAGE, "Next Page", () -> changePage(currentPageIndex + 1)));
        this.lastButton = new IconButtonWidget(
                new ButtonConfig(ButtonIcon.LAST_PAGE, "Last Page", () -> changePage(pageCount - 1)));

        initialize();
        updateControls();
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Sets the total number of pages.
     * 
     * @param pageCount The total page count (minimum 1).
     */
    public void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
        if (currentPageIndex >= this.pageCount) {
            currentPageIndex = this.pageCount - 1;
        }
        updateControls();
    }

    /**
     * Sets the current page index.
     * 
     * @param index The page index to display (0-based).
     */
    public void setCurrentPageIndex(int index) {
        if (index >= 0 && index < pageCount) {
            this.currentPageIndex = index;
            updateControls();
        }
    }

    /**
     * Updates the total rows display text.
     * 
     * @param text The text to display (e.g., "Total rows: 500").
     */
    public void setTotalRows(String text) {
        totalRowsLabel.setText(text);
    }

    /**
     * Sets the rows per page value.
     * 
     * @param value The number of rows per page as a string.
     */
    public void setRowsPerPage(String value) {
        rowsPerPageInput.setText(value);
        if (isValidRowsPerPage(value)) {
            lastValidRowsPerPage = value;
        }
    }

    /**
     * Returns the text property of the rows per page input field.
     * 
     * @return The StringProperty for binding.
     */
    public StringProperty rowsPerPageTextProperty() {
        return rowsPerPageInput.textProperty();
    }

    // ==============================================================================================
    // Private Methods - Initialization
    // ==============================================================================================

    /**
     * Initializes the widget layout and styling.
     */
    private void initialize() {
        // Apply styling
        this.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        this.getStyleClass().add(STYLE_CLASS_FOOTER);
        this.setAlignment(Pos.CENTER);
        this.setSpacing(SPACING);

        // Setup input handlers
        setupRowsPerPageInput();
        setupPageInput();

        // Build layout
        HBox leftBox = createLeftSection();
        HBox centerBox = createCenterSection();
        HBox rightBox = createRightSection();

        Region spacerLeft = createSpacer();
        Region spacerRight = createSpacer();

        this.getChildren().addAll(leftBox, spacerLeft, centerBox, spacerRight, rightBox);
    }

    /**
     * Creates the left section with rows-per-page configuration.
     */
    private HBox createLeftSection() {
        Label rowsLabel = new Label("Rows/page:");
        HBox leftBox = new HBox(INNER_SPACING, rowsLabel, rowsPerPageInput);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        return leftBox;
    }

    /**
     * Creates the center section with navigation controls.
     */
    private HBox createCenterSection() {
        Label pagePrefix = new Label("Page");

        HBox centerBox = new HBox(INNER_SPACING,
                firstButton, prevButton,
                pagePrefix, pageInput, pageIndicatorLabel,
                nextButton, lastButton);
        centerBox.setAlignment(Pos.CENTER);
        return centerBox;
    }

    /**
     * Creates the right section with total rows information.
     */
    private HBox createRightSection() {
        HBox rightBox = new HBox(totalRowsLabel);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        return rightBox;
    }

    /**
     * Creates a spacer region that grows to fill available space.
     */
    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Sets up the rows-per-page input field with validation handlers.
     */
    private void setupRowsPerPageInput() {
        rowsPerPageInput.setOnAction(e -> handleRowsPerPageCommit());
        rowsPerPageInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.FALSE.equals(newVal)) {
                handleRowsPerPageCommit();
            }
        });
    }

    /**
     * Sets up the page input field with validation handlers.
     */
    private void setupPageInput() {
        pageInput.setOnAction(e -> handlePageInputCommit());
        pageInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.FALSE.equals(newVal)) {
                handlePageInputCommit();
            }
        });
    }

    // ==============================================================================================
    // Private Methods - Text Field Creation
    // ==============================================================================================

    /**
     * Creates a square-styled text field with auto-resize capability.
     * 
     * @param initialText The initial text content.
     * @return The configured TextField.
     */
    private TextField createSquareTextField(String initialText) {
        TextField tf = new TextField(initialText);
        tf.setPrefWidth(MIN_INPUT_WIDTH);
        tf.getStyleClass().add(STYLE_CLASS_SQUARE_INPUT);
        tf.setAlignment(Pos.CENTER);

        // Enable auto-resize based on content
        tf.textProperty().addListener((obs, oldVal, newVal) -> adjustWidth(tf));
        adjustWidth(tf); // Initial adjustment

        return tf;
    }

    /**
     * Adjusts the width of a text field based on its content.
     * 
     * @param tf The TextField to adjust.
     */
    private void adjustWidth(TextField tf) {
        Text text = new Text(tf.getText());
        text.setFont(tf.getFont() != null ? tf.getFont() : Font.getDefault());
        double width = text.getLayoutBounds().getWidth() + INPUT_PADDING;
        tf.setPrefWidth(Math.max(MIN_INPUT_WIDTH, width));
    }

    // ==============================================================================================
    // Private Methods - Validation
    // ==============================================================================================

    /**
     * Validates if the given text represents a valid rows-per-page value.
     * 
     * @param text The text to validate.
     * @return true if valid (positive integer), false otherwise.
     */
    private boolean isValidRowsPerPage(String text) {
        try {
            int val = Integer.parseInt(text);
            return val > 0;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    // ==============================================================================================
    // Private Methods - Navigation
    // ==============================================================================================

    /**
     * Changes to the specified page index.
     * 
     * @param newIndex The new page index (0-based).
     */
    private void changePage(int newIndex) {
        if (newIndex < 0 || newIndex >= pageCount || newIndex == currentPageIndex) {
            return;
        }

        currentPageIndex = newIndex;
        updateControls();

        if (onPageChange != null) {
            onPageChange.accept(currentPageIndex);
        }
    }

    /**
     * Handles commit action for the page input field.
     * Validates and navigates to the entered page number.
     */
    private void handlePageInputCommit() {
        try {
            int page = Integer.parseInt(pageInput.getText()) - 1; // Convert to 0-based index
            if (page >= 0 && page < pageCount) {
                changePage(page);
            } else {
                // Invalid page number, reset to current
                pageInput.setText(String.valueOf(currentPageIndex + 1));
            }
        } catch (NumberFormatException _) {
            // Invalid input, reset to current
            pageInput.setText(String.valueOf(currentPageIndex + 1));
        }
    }

    /**
     * Handles commit action for the rows-per-page input field.
     * Validates and stores the value or reverts to the last valid value.
     */
    private void handleRowsPerPageCommit() {
        if (isValidRowsPerPage(rowsPerPageInput.getText())) {
            lastValidRowsPerPage = rowsPerPageInput.getText();
        } else {
            rowsPerPageInput.setText(lastValidRowsPerPage);
        }
    }

    /**
     * Updates the enabled/disabled state of navigation controls based on current
     * page.
     */
    private void updateControls() {
        boolean isStart = currentPageIndex == 0;
        boolean isEnd = currentPageIndex >= pageCount - 1;

        firstButton.setDisable(isStart);
        prevButton.setDisable(isStart);
        nextButton.setDisable(isEnd);
        lastButton.setDisable(isEnd);

        pageIndicatorLabel.setText("/ " + pageCount);

        // Only update page input if it doesn't have focus (to avoid disrupting user
        // input)
        if (!pageInput.isFocused()) {
            pageInput.setText(String.valueOf(currentPageIndex + 1));
        }
    }
}
