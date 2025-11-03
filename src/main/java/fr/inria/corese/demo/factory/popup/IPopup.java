package fr.inria.corese.demo.factory.popup;

public interface IPopup {
  void closePopup(); // Au lieu de hide()

  void displayPopup(); // Au lieu de show()

  String getPopupTitle(); // Déjà modifié précédemment

  void setMessage(String message);
}
