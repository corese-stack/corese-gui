package fr.inria.corese.gui.enums.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.*;

/**
 * Enum for icon button types with their corresponding Ikon. This provides a unified way to
 * reference icons throughout the application.
 */
public enum IconButtonType {
  SAVE(MaterialDesignC.CONTENT_SAVE_OUTLINE),
  OPEN_FILE(MaterialDesignF.FILE),
  IMPORT(MaterialDesignI.IMPORT),
  EXPORT(MaterialDesignE.EXPORT),
  CLEAR(MaterialDesignB.BROOM),
  UNDO(MaterialDesignU.UNDO),
  REDO(MaterialDesignR.REDO),
  DOCUMENTATION(MaterialDesignL.LINK),
  ZOOM_IN(MaterialDesignM.MAGNIFY_PLUS_OUTLINE),
  ZOOM_OUT(MaterialDesignM.MAGNIFY_MINUS_OUTLINE),
  FULL_SCREEN(MaterialDesignF.FULLSCREEN),
  NEW_FILE(MaterialDesignF.FILE_PLUS_OUTLINE),
  NEW_FOLDER(MaterialDesignF.FOLDER_PLUS_OUTLINE),
  OPEN_FOLDER(MaterialDesignF.FOLDER_OPEN),
  CLOSE_FILE_EXPLORER(MaterialDesignF.FOLDER_OUTLINE),
  RELOAD(MaterialDesignR.REFRESH),
  LOGS(MaterialDesignB.BOOK_OPEN_VARIANT),
  DELETE(MaterialDesignT.TRASH_CAN),
  COPY(MaterialDesignC.CONTENT_COPY),
  PASTE(MaterialDesignC.CONTENT_PASTE),
  TEMPLATE(MaterialDesignS.SCRIPT_OUTLINE),
  SPLIT(MaterialDesignV.VIEW_SPLIT_VERTICAL),
  PLAY(MaterialDesignP.PLAY);

  private final Ikon ikon;

  IconButtonType(Ikon ikon) {
    this.ikon = ikon;
  }

  /**
   * Returns the Ikon associated with this button type.
   *
   * @return The Ikon instance
   */
  public Ikon getIkon() {
    return ikon;
  }
}
