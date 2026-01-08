package fr.inria.corese.gui.view.icon;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public class IconButtonView extends Button {

  public IconButtonView(IconButtonType type) {
    createIcon(type);
  }

  private void createIcon(IconButtonType type) {
    // Get tooltip based on type
    String tooltipText =
        switch (type) {
          case SAVE -> "Save";
          case OPEN_FILE -> "Open file";
          case EXPORT -> "Export";
          case IMPORT -> "Import";
          case CLEAR -> "Clear";
          case UNDO -> "Undo";
          case REDO -> "Redo";
          case DOCUMENTATION -> "Documentation";
          case ZOOM_IN -> "Zoom in";
          case ZOOM_OUT -> "Zoom out";
          case FULL_SCREEN -> "Full screen";
          case NEW_FILE -> "New file";
          case NEW_FOLDER -> "New folder";
          case OPEN_FOLDER -> "Open folder";
          case CLOSE_FILE_EXPLORER -> "Close file explorer";
          case RELOAD -> "Reload files";
          case LOGS -> "Show logs";
          case DELETE -> "Clear graph";
          case COPY -> "Copy to Clipboard";
          case TEMPLATE -> "Select Template";
          case SPLIT -> "Split View";
          case PLAY -> "Play";
          case PASTE -> "Paste";
        };

    createIconButton(type.getIkon(), tooltipText);
  }

  private void createIconButton(Ikon icon, String tooltipText) {
    FontIcon fontIcon = new FontIcon(icon);
    Tooltip tooltip = new Tooltip(tooltipText);
    Tooltip.install(this, tooltip);

    fontIcon.setIconSize(25);

    // Use AtlantaFX styles
    getStyleClass().add("flat");

    // Bind icon color to button text color to respect theme
    fontIcon.iconColorProperty().bind(textFillProperty());

    setGraphic(fontIcon);
  }

  public void setType(IconButtonType iconButtonType) {
    createIcon(iconButtonType);
  }
}
