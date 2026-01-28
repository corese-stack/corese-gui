package fr.inria.corese.gui.core.factory.popup;

public class DeleteConfirmationPopup extends WarningPopup {
  public DeleteConfirmationPopup() {
    super();
    setMessage("Are you sure you want to delete this item?\nThis action cannot be undone.");
  }
}
