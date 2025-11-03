package fr.inria.corese.demo.model;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.model.codeEditor.CodeEditorModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconButtonBarModel {
  private final Map<IconButtonType, Boolean> buttonStates = new HashMap<>();
  private CodeEditorModel codeEditorModel;
  private final List<IconButtonType> availableButtons;

  public IconButtonBarModel(List<IconButtonType> buttons) {
    this.availableButtons = buttons;
    buttons.forEach(button -> buttonStates.put(button, true));
  }

  public List<IconButtonType> getAvailableButtons() {
    return availableButtons;
  }

  public void setButtonEnabled(IconButtonType type, boolean enabled) {
    buttonStates.put(type, enabled);
  }

  public boolean isButtonEnabled(IconButtonType type) {
    return buttonStates.getOrDefault(type, false);
  }

  public void setCodeEditorModel(CodeEditorModel codeEditorModel) {
    this.codeEditorModel = codeEditorModel;
  }

  public CodeEditorModel getCodeEditorModel() {
    return codeEditorModel;
  }
}
