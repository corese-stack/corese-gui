package fr.inria.corese.gui.enums.icon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum IconButtonBarType {
  DATA(IconButtonType.IMPORT, IconButtonType.EXPORT, IconButtonType.CLEAR),

  RDF_EDITOR(
      IconButtonType.EXPORT,
      IconButtonType.SAVE,
      IconButtonType.OPEN_FILE,
      IconButtonType.CLEAR,
      IconButtonType.UNDO,
      IconButtonType.REDO),

  VALIDATION(
      IconButtonType.SAVE,
      IconButtonType.EXPORT,
      IconButtonType.CLEAR,
      IconButtonType.UNDO,
      IconButtonType.REDO),

  QUERY(IconButtonType.SAVE, IconButtonType.CLEAR, IconButtonType.UNDO, IconButtonType.REDO);

  private final List<IconButtonType> buttons;

  IconButtonBarType(IconButtonType... buttons) {
    this.buttons = Arrays.asList(buttons);
  }

  public List<IconButtonType> getButtons() {
    return Collections.unmodifiableList(buttons);
  }
}
