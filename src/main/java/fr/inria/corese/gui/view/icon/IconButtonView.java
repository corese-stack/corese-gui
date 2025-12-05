package fr.inria.corese.gui.view.icon;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;

import fr.inria.corese.gui.enums.icon.IconButtonType;

public class IconButtonView extends Button {

  public IconButtonView(IconButtonType type) {
    createIcon(type);
  }

  private void createIcon(IconButtonType type) {
    switch (type) {
      case SAVE -> createIconButton(MaterialDesignC.CONTENT_SAVE_OUTLINE, "Save as");
      case OPEN_FILE -> createIconButton(MaterialDesignF.FILE, "Open file");
      case EXPORT -> createIconButton(MaterialDesignE.EXPORT, "Export");
      case IMPORT -> createIconButton(MaterialDesignI.IMPORT, "Import");
      case CLEAR -> createIconButton(MaterialDesignB.BROOM, "Clear");
      case UNDO -> createIconButton(MaterialDesignU.UNDO, "Undo");
      case REDO -> createIconButton(MaterialDesignR.REDO, "Redo");
      case DOCUMENTATION -> createIconButton(MaterialDesignL.LINK, "Documentation");
      case ZOOM_IN -> createIconButton(MaterialDesignM.MAGNIFY_PLUS_OUTLINE, "Zoom in");
      case ZOOM_OUT -> createIconButton(MaterialDesignM.MAGNIFY_MINUS_OUTLINE, "Zoom out");
      case FULL_SCREEN -> createIconButton(MaterialDesignF.FULLSCREEN, "Full screen");
      case NEW_FILE -> createIconButton(MaterialDesignF.FILE_PLUS_OUTLINE, "New file");
      case NEW_FOLDER -> createIconButton(MaterialDesignF.FOLDER_PLUS_OUTLINE, "New folder");
      case OPEN_FOLDER -> createIconButton(MaterialDesignF.FOLDER_OPEN, "Open folder");
      case CLOSE_FILE_EXPLORER ->
          createIconButton(MaterialDesignF.FOLDER_OUTLINE, "Close file explorer");
      case RELOAD -> createIconButton(MaterialDesignR.REFRESH, "Reload files");
      case LOGS -> createIconButton(MaterialDesignB.BOOK_OPEN_VARIANT, "Show logs");
      case DELETE -> createIconButton(MaterialDesignT.TRASH_CAN, "Clear graph");
      case COPY -> createIconButton(MaterialDesignC.CONTENT_COPY, "Copy to Clipboard");
      case TEMPLATE -> createIconButton(MaterialDesignS.SCRIPT_OUTLINE, "Select Template");
      case SPLIT -> createIconButton(MaterialDesignV.VIEW_SPLIT_VERTICAL, "Split View");
      default -> throw new IllegalArgumentException("Unexpected value: " + type);
    }
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
