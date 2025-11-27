// model/ButtonType.java
package fr.inria.corese.gui.enums.button;

public enum ButtonType {
  RUN("Run", "file-button");

  private final String label;
  private final String styleClass;

  ButtonType(String label, String styleClass) {
    this.label = label;
    this.styleClass = styleClass;
  }

  public String getLabel() {
    return label;
  }

  public String getStyleClass() {
    return styleClass;
  }
}
