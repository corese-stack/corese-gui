package fr.inria.corese.gui.factory.icon;

import fr.inria.corese.gui.controller.CodeEditorController;
import fr.inria.corese.gui.controller.IconButtonBarController;
import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.model.IconButtonBarModel;
import fr.inria.corese.gui.view.icon.IconButtonBarView;

public class IconButtonBarFactory {

  public static IconButtonBarController create(
      IconButtonBarType type, CodeEditorController parentController) {
    IconButtonBarModel model = new IconButtonBarModel(type.getButtons());
    IconButtonBarView view = new IconButtonBarView();
    // The key is passing the parentController to the constructor here
    return new IconButtonBarController(model, view, parentController);
  }

  public static IconButtonBarController create(IconButtonBarType type) {
    return create(type, null);
  }
}
