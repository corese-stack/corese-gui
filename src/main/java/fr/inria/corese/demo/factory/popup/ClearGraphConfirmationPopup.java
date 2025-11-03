package fr.inria.corese.demo.factory.popup;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class ClearGraphConfirmationPopup implements IPopup {
  private final Alert alert;
  private boolean result;

  public ClearGraphConfirmationPopup() {
    alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Clear Graph Confirmation");
    alert.setHeaderText(null);

    // Définir le bouton OK comme bouton par défaut
    ButtonType okButton = ButtonType.OK;
    ButtonType cancelButton = ButtonType.CANCEL;
    alert.getDialogPane().getButtonTypes().setAll(okButton, cancelButton);

    // Obtenir le bouton physique et le définir comme bouton par défaut
    alert.getDialogPane().lookupButton(okButton).setStyle("-fx-default-button: true;");
  }

  @Override
  public void closePopup() {
    alert.hide();
  }

  @Override
  public void displayPopup() {
    Optional<ButtonType> response = alert.showAndWait();
    result = response.isPresent() && response.get() == ButtonType.OK;
  }

  @Override
  public String getPopupTitle() {
    return "Clear Graph Confirmation";
  }

  @Override
  public void setMessage(String message) {
    alert.setContentText(message);
  }

  public boolean getResult() {
    return result;
  }
}
