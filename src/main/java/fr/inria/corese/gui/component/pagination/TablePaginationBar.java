package fr.inria.corese.gui.component.pagination;

import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import java.util.function.Consumer;
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
 * A comprehensive bottom toolbar for tables, containing pagination controls,
 * rows-per-page configuration, and total row count.
 *
 * <p>Layout:
 * [Rows/page: Input] [Spacer] [First] [Prev] "Page X / Y" [Next] [Last] [Spacer] [Total rows: N]
 */
public class TablePaginationBar extends HBox {

    private static final String STYLE_CLASS_FOOTER = "table-footer";
    private static final String STYLE_CLASS_SQUARE_INPUT = "square-text-field";
    private static final int SPACING = 10;
    private static final double MIN_INPUT_WIDTH = 45.0;

    private final Consumer<Integer> onPageChange;
    private int pageCount = 1;
    private int currentPageIndex = 0;
    private String lastValidRowsPerPage = "50";

    // UI Components
    private IconButtonWidget firstButton;
    private IconButtonWidget prevButton;
    private IconButtonWidget nextButton;
    private IconButtonWidget lastButton;
    private Label pageIndicatorLabel;
    private TextField pageInput;
    private TextField rowsPerPageInput;
    private Label totalRowsLabel;

    public TablePaginationBar(Consumer<Integer> onPageChange) {
        this.onPageChange = onPageChange;
        initializeView();
        updateControls();
    }

    private void initializeView() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(SPACING);
        this.getStyleClass().add(STYLE_CLASS_FOOTER);
        this.getStylesheets().add(getClass().getResource("/css/table-result.css").toExternalForm());

        // Left Section: Configuration
        Label rowsLabel = new Label("Rows/page:");
        rowsPerPageInput = createSquareTextField("50");
        rowsPerPageInput.setOnAction(e -> handleRowsPerPageCommit());
        rowsPerPageInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.FALSE.equals(newVal)) handleRowsPerPageCommit();
        });
        
        HBox leftBox = new HBox(5, rowsLabel, rowsPerPageInput);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        // Center Section: Navigation
        firstButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.FIRST_PAGE, "First Page", () -> changePage(0)));
        prevButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.PREVIOUS_PAGE, "Previous Page", () -> changePage(currentPageIndex - 1)));
        nextButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.NEXT_PAGE, "Next Page", () -> changePage(currentPageIndex + 1)));
        lastButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.LAST_PAGE, "Last Page", () -> changePage(pageCount - 1)));

        Label pagePrefix = new Label("Page");
        pageInput = createSquareTextField("1");
        pageInput.setOnAction(e -> handlePageInputCommit());
        pageInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.FALSE.equals(newVal)) handlePageInputCommit();
        });
        
        pageIndicatorLabel = new Label("/ 1");

        HBox centerBox = new HBox(5, 
            firstButton, prevButton, 
            pagePrefix, pageInput, pageIndicatorLabel, 
            nextButton, lastButton
        );
        centerBox.setAlignment(Pos.CENTER);

        // Right Section: Information
        totalRowsLabel = new Label("Total rows: 0");
        HBox rightBox = new HBox(totalRowsLabel);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        // Layout Spacers
        Region spacerL = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region();
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        this.getChildren().addAll(leftBox, spacerL, centerBox, spacerR, rightBox);
    }

    private TextField createSquareTextField(String initialText) {
        TextField tf = new TextField(initialText);
        tf.setPrefWidth(MIN_INPUT_WIDTH);
        tf.getStyleClass().add(STYLE_CLASS_SQUARE_INPUT);
        tf.setAlignment(Pos.CENTER);
        
        // Auto-resize logic
        tf.textProperty().addListener((obs, oldVal, newVal) -> adjustWidth(tf));
        // Initial adjustment
        adjustWidth(tf);
        
        return tf;
    }

    private void adjustWidth(TextField tf) {
        Text text = new Text(tf.getText());
        text.setFont(tf.getFont() != null ? tf.getFont() : Font.getDefault());
        double width = text.getLayoutBounds().getWidth() + 35; // Add extra padding to avoid cutoff
        tf.setPrefWidth(Math.max(MIN_INPUT_WIDTH, width));
    }

    private boolean isValidRowsPerPage(String text) {
        try {
            int val = Integer.parseInt(text);
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidPageNumber(String text) {
        try {
            int val = Integer.parseInt(text);
            return val >= 1 && val <= pageCount;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void changePage(int newIndex) {
        if (newIndex < 0 || newIndex >= pageCount) return;
        
        if (newIndex != currentPageIndex) {
            currentPageIndex = newIndex;
            updateControls();
            if (onPageChange != null) {
                onPageChange.accept(currentPageIndex);
            }
        }
    }

    private void handlePageInputCommit() {
        try {
            int page = Integer.parseInt(pageInput.getText()) - 1; 
            if (page >= 0 && page < pageCount) {
                changePage(page);
            } else {
                pageInput.setText(String.valueOf(currentPageIndex + 1));
            }
        } catch (NumberFormatException ex) {
            pageInput.setText(String.valueOf(currentPageIndex + 1));
        }
    }

    private void handleRowsPerPageCommit() {
        if (isValidRowsPerPage(rowsPerPageInput.getText())) {
            lastValidRowsPerPage = rowsPerPageInput.getText();
        } else {
            rowsPerPageInput.setText(lastValidRowsPerPage);
        }
    }

    private void updateControls() {
        boolean isStart = currentPageIndex == 0;
        boolean isEnd = currentPageIndex >= pageCount - 1;

        firstButton.setDisable(isStart);
        prevButton.setDisable(isStart);
        nextButton.setDisable(isEnd);
        lastButton.setDisable(isEnd);

        pageIndicatorLabel.setText("/ " + pageCount);
        
        if (!pageInput.isFocused()) {
            pageInput.setText(String.valueOf(currentPageIndex + 1));
        }
    }

    public void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
        if (currentPageIndex >= this.pageCount) {
            currentPageIndex = this.pageCount - 1;
        }
        updateControls();
    }

    public void setCurrentPageIndex(int index) {
        if (index >= 0 && index < pageCount) {
            this.currentPageIndex = index;
            updateControls();
        }
    }

    public void setTotalRows(String text) {
        totalRowsLabel.setText(text);
    }

    public void setRowsPerPage(String value) {
        rowsPerPageInput.setText(value);
        if (isValidRowsPerPage(value)) {
            lastValidRowsPerPage = value;
        }
    }

    public StringProperty rowsPerPageTextProperty() {
        return rowsPerPageInput.textProperty();
    }
}