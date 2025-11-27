package fr.inria.corese.gui.view.rule;

import java.util.function.Consumer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class CustomPagination extends HBox {
  private int pageCount = 1;
  private IntegerProperty currentPageIndex = new SimpleIntegerProperty(0);
  private Consumer<Integer> onPageSelected;

  public CustomPagination(int pageCount, Consumer<Integer> onPageSelected) {
    this.pageCount = Math.max(1, pageCount);
    this.onPageSelected = onPageSelected;
    setAlignment(Pos.CENTER);
    setSpacing(4);
    updateButtons();
    currentPageIndex.addListener((obs, oldVal, newVal) -> updateButtons());
  }

  public void setPageCount(int pageCount) {
    this.pageCount = Math.max(1, pageCount);
    updateButtons();
  }

  public int getPageCount() {
    return pageCount;
  }

  public IntegerProperty currentPageIndexProperty() {
    return currentPageIndex;
  }

  public int getCurrentPageIndex() {
    return currentPageIndex.get();
  }

  public void setCurrentPageIndex(int index) {
    if (index >= 0 && index < pageCount) {
      currentPageIndex.set(index);
      if (onPageSelected != null) {
        onPageSelected.accept(index);
      }
    }
  }

  private void updateButtons() {
    getChildren().clear();

    int lastPage = pageCount - 1;
    int curr = getCurrentPageIndex();

    // Show "1"
    addPageButton(0);

    // Show "2" and "3" if they exist
    if (pageCount > 1) addPageButton(1);
    if (pageCount > 2) addPageButton(2);

    if (curr > 4 && pageCount > 5) {
      addEllipsis();
    }

    for (int i = Math.max(3, curr - 1); i <= Math.min(lastPage - 1, curr + 1); i++) {
      if (i > 2 && i < lastPage) {
        addPageButton(i);
      }
    }

    if (curr < lastPage - 3 && pageCount > 5) {
      addEllipsis();
    }

    if (lastPage > 2) {
      addPageButton(lastPage);
    }
  }

  private void addPageButton(int pageIndex) {
    Button btn = new Button(String.valueOf(pageIndex + 1));
    btn.setMinWidth(32);
    btn.setMaxHeight(24);
    btn.setDisable(pageIndex == getCurrentPageIndex());
    btn.getStyleClass().add("pagination-button");
    btn.setOnAction(e -> setCurrentPageIndex(pageIndex));
    getChildren().add(btn);
  }

  private void addEllipsis() {
    Button ellipsis = new Button("...");
    ellipsis.setDisable(true);
    ellipsis.setMinWidth(32);
    ellipsis.setMaxHeight(24);
    ellipsis.getStyleClass().add("pagination-ellipsis");
    getChildren().add(ellipsis);
  }
}
