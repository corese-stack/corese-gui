package fr.inria.corese.gui.factory.icon;

import fr.inria.corese.gui.controller.CodeEditorController;
import fr.inria.corese.gui.controller.IconButtonBarController;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.model.IconButtonBarModel;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import java.util.List;

public class IconButtonBarFactory {

  public static IconButtonBarController create(
      List<IconButtonType> buttons, CodeEditorController parentController) {
    IconButtonBarModel model = new IconButtonBarModel(buttons);
    IconButtonBarView view = new IconButtonBarView();
    // The key is passing the parentController to the constructor here
    return new IconButtonBarController(model, view, parentController);
  }

  public static IconButtonBarController create(List<IconButtonType> buttons) {
    return create(buttons, null);
  }
}
