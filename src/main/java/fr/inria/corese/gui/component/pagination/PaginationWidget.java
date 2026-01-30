package fr.inria.corese.gui.component.pagination;

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * A reusable pagination component providing navigation controls for paginated data views.
 *
 * <p>Features:
 * <ul>
 *   <li>First, Previous, Next, Last navigation buttons</li>
 *   <li>Direct page input via text field</li>
 *   <li>Current page / Total pages display</li>
 *   <li>Automatic button disabling based on bounds</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * PaginationWidget pagination = new PaginationWidget(10, pageIndex -> {
 *     System.out.println("Switched to page: " + pageIndex);
 *     // load data for pageIndex...
 * });
 * }</pre>
 */
public class PaginationWidget extends HBox {

    // ==============================================================================================
    // Constants
    // ==============================================================================================

    private static final String STYLE_CLASS_CONTAINER = "pagination-container";
    private static final int SPACING = 10;
    private static final int INPUT_WIDTH = 50;

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private final Consumer<Integer> onPageChange;
    private int pageCount;
    private int currentPageIndex;

    private Button firstButton;
    private Button prevButton;
    private Button nextButton;
    private Button lastButton;
    private Label pageLabel;
    private TextField pageInput;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    /**
     * Constructs a new PaginationWidget.
     *
     * @param pageCount    The initial total number of pages.
     * @param onPageChange A callback invoked when the page index changes (0-based).
     */
    public PaginationWidget(int pageCount, Consumer<Integer> onPageChange) {
        this.pageCount = Math.max(1, pageCount);
        this.onPageChange = onPageChange;
        this.currentPageIndex = 0;

        initializeView();
        updateControls();
    }

    // ==============================================================================================
    // Initialization
    // ==============================================================================================

    private void initializeView() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(SPACING);
        this.getStyleClass().add(STYLE_CLASS_CONTAINER);

        // Buttons
        firstButton = createButton("<<", 0);
        prevButton = createButton("<", -1); // Dynamic target
        nextButton = createButton(">", -1); // Dynamic target
        lastButton = createButton(">>", -1); // Dynamic target

        // Overwrite dynamic handlers
        prevButton.setOnAction(e -> changePage(currentPageIndex - 1));
        nextButton.setOnAction(e -> changePage(currentPageIndex + 1));
        lastButton.setOnAction(e -> changePage(pageCount - 1));

        // Label
        pageLabel = new Label();

        // Input
        pageInput = new TextField();
        pageInput.setPrefWidth(INPUT_WIDTH);
        pageInput.setOnAction(e -> handleInputCommit());

        // Spacers
        Region spacerL = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region();
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        this.getChildren().addAll(
            spacerL, firstButton, prevButton, pageLabel, pageInput, nextButton, lastButton, spacerR
        );
    }

    private Button createButton(String text, int targetPage) {
        Button btn = new Button(text);
        if (targetPage >= 0) {
            btn.setOnAction(e -> changePage(targetPage));
        }
        return btn;
    }

    // ==============================================================================================
    // Logic
    // ==============================================================================================

    private void changePage(int newIndex) {
        if (newIndex < 0 || newIndex >= pageCount) return;
        
        // Always notify even if index is "same"? 
        // Usually pagination implies data refresh, so yes, but distinct check optimization is fine.
        if (newIndex != currentPageIndex) {
            currentPageIndex = newIndex;
            updateControls();
            if (onPageChange != null) {
                onPageChange.accept(currentPageIndex);
            }
        }
    }

    private void handleInputCommit() {
        try {
            int page = Integer.parseInt(pageInput.getText()) - 1; // Convert 1-based input to 0-based index
            changePage(page);
        } catch (NumberFormatException ex) {
            // Revert to current page on invalid input
            pageInput.setText(String.valueOf(currentPageIndex + 1));
        }
    }

    private void updateControls() {
        firstButton.setDisable(currentPageIndex == 0);
        prevButton.setDisable(currentPageIndex == 0);
        nextButton.setDisable(currentPageIndex >= pageCount - 1);
        lastButton.setDisable(currentPageIndex >= pageCount - 1);

        pageLabel.setText(String.format("Page %d / %d", currentPageIndex + 1, pageCount));
        
        // Only update input text if it's not focused or if we changed page via buttons
        if (!pageInput.isFocused()) {
            pageInput.setText(String.valueOf(currentPageIndex + 1));
        } else {
             // If focused (user typed Enter), update it to normalize input (e.g. " 1 " -> "1")
             pageInput.setText(String.valueOf(currentPageIndex + 1));
             pageInput.positionCaret(pageInput.getText().length());
        }
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Updates the total number of pages.
     * Resets the current page index to fit within the new bounds if necessary.
     *
     * @param pageCount The new total number of pages.
     */
    public void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
        if (currentPageIndex >= this.pageCount) {
            currentPageIndex = this.pageCount - 1;
        }
        updateControls();
    }

    /**
     * Sets the current page index directly.
     * Updates the view without triggering the onPageChange callback.
     *
     * @param index The 0-based page index.
     */
    public void setCurrentPageIndex(int index) {
        if (index >= 0 && index < pageCount) {
            this.currentPageIndex = index;
            updateControls();
        }
    }

    /**
     * Returns the current page index.
     *
     * @return The 0-based page index.
     */
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }
}