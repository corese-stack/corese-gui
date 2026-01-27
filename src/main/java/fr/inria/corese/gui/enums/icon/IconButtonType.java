package fr.inria.corese.gui.enums.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.*;

/**
 * Enum for icon button types with their corresponding Ikon. This provides a unified way to
 * reference icons throughout the application.
 */
public enum IconButtonType {

  // =============================================================================================
  // Enum Constants
  // =============================================================================================

  SAVE(MaterialDesignC.CONTENT_SAVE_OUTLINE),
  OPEN_FILE(MaterialDesignF.FILE),
  IMPORT(MaterialDesignI.IMPORT),
  EXPORT(MaterialDesignE.EXPORT),
  CLEAR(MaterialDesignB.BROOM),
  UNDO(MaterialDesignU.UNDO),
  REDO(MaterialDesignR.REDO),
  DOCUMENTATION(MaterialDesignL.LINK),
  RELOAD(MaterialDesignR.REFRESH),
  LOGS(MaterialDesignB.BOOK_OPEN_VARIANT),
  DELETE(MaterialDesignT.TRASH_CAN),
  COPY(MaterialDesignC.CONTENT_COPY),
  PLAY(MaterialDesignP.PLAY);

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private final Ikon ikon;

  /**
   * Constructor for IconButtonType.
   *
   * @param ikon The Ikon associated with the button type
   */
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
