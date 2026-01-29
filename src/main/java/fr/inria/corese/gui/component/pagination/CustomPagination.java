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
 * A reusable pagination component with "First", "Previous", "Next", "Last" buttons
 * and a direct page input field.
 */
public class CustomPagination extends HBox {

  private final Consumer<Integer> onPageChange;
  private int pageCount;
  private int currentPageIndex;

  private Button firstButton;
  private Button prevButton;
  private Button nextButton;
  private Button lastButton;
  private Label pageLabel;
  private TextField pageInput;

  public CustomPagination(int pageCount, Consumer<Integer> onPageChange) {
    this.pageCount = pageCount;
    this.onPageChange = onPageChange;
    this.currentPageIndex = 0;

    this.setAlignment(Pos.CENTER);
    this.setSpacing(10);
    this.getStyleClass().add("pagination-container");

    initializeComponents();
    updateControls();
  }

  private void initializeComponents() {
    firstButton = new Button("<<");
    prevButton = new Button("<");
    nextButton = new Button(">");
    lastButton = new Button(">>");

    firstButton.setOnAction(e -> changePage(0));
    prevButton.setOnAction(e -> changePage(currentPageIndex - 1));
    nextButton.setOnAction(e -> changePage(currentPageIndex + 1));
    lastButton.setOnAction(e -> changePage(pageCount - 1));

    pageLabel = new Label();
    updatePageLabel();

    pageInput = new TextField();
    pageInput.setPrefWidth(50);
    pageInput.setOnAction(
        e -> {
          try {
            int page = Integer.parseInt(pageInput.getText()) - 1;
            changePage(page);
          } catch (NumberFormatException ex) {
            pageInput.setText(String.valueOf(currentPageIndex + 1));
          }
        });

    Region spacerL = new Region();
    HBox.setHgrow(spacerL, Priority.ALWAYS);
    Region spacerR = new Region();
    HBox.setHgrow(spacerR, Priority.ALWAYS);

    this.getChildren()
        .addAll(
            spacerL, firstButton, prevButton, pageLabel, pageInput, nextButton, lastButton, spacerR);
  }

  private void changePage(int newIndex) {
    if (newIndex < 0 || newIndex >= pageCount) return;
    currentPageIndex = newIndex;
    updateControls();
    if (onPageChange != null) {
      onPageChange.accept(currentPageIndex);
    }
  }

  public void setPageCount(int pageCount) {
    this.pageCount = pageCount;
    if (currentPageIndex >= pageCount) {
      currentPageIndex = Math.max(0, pageCount - 1);
    }
    updateControls();
  }

  public void setCurrentPageIndex(int index) {
    if (index >= 0 && index < pageCount) {
      this.currentPageIndex = index;
      updateControls();
    }
  }

  public int getCurrentPageIndex() {
    return currentPageIndex;
  }

  private void updateControls() {
    firstButton.setDisable(currentPageIndex == 0);
    prevButton.setDisable(currentPageIndex == 0);
    nextButton.setDisable(currentPageIndex >= pageCount - 1);
    lastButton.setDisable(currentPageIndex >= pageCount - 1);

    updatePageLabel();
    pageInput.setText(String.valueOf(currentPageIndex + 1));
  }

  private void updatePageLabel() {
    pageLabel.setText(String.format("Page %d / %d", currentPageIndex + 1, pageCount));
  }
}
