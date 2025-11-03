package fr.inria.corese.demo.factory.popup;

import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;

public abstract class BasePopup extends Dialog<Void> implements IPopup {
  protected String message;

  public BasePopup() {
    initStyle(StageStyle.DECORATED);
    setResizable(true);
  }

  @Override
  public void closePopup() {
    close();
  }

  @Override
  public void displayPopup() {
    showAndWait();
  }

  @Override
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String getPopupTitle() {
    return getTitle();
  }
}
