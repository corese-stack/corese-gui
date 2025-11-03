package fr.inria.corese.demo.factory.icon;

import fr.inria.corese.demo.controller.CodeEditorController;
import fr.inria.corese.demo.controller.IconButtonBarController;
import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.model.IconButtonBarModel;
import fr.inria.corese.demo.view.icon.IconButtonBarView;

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
