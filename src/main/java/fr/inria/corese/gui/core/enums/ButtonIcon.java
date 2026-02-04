package fr.inria.corese.gui.core.enums;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import fr.inria.corese.gui.core.config.ButtonConfig;

/**
 * Enumeration of button icons used throughout the application.
 *
 * <p>
 * This enum provides a centralized mapping between semantic button actions and
 * their visual icon
 * representations. It serves as an abstraction layer over the underlying icon
 * library (Material
 * Design Icons), making it easy to:
 *
 * <ul>
 * <li>Change icons globally by modifying only this enum
 * <li>Ensure consistent icon usage across the application
 * <li>Switch to a different icon library if needed
 * </ul>
 *
 * <p>
 * <b>Design rationale:</b> The enum name is {@code ButtonIcon} (not
 * {@code IconButtonType})
 * because it represents the <i>icon itself</i>, not a "type of button". This
 * shorter, more semantic
 * naming improves code readability.
 *
 * <p>
 * <b>Usage example:</b>
 *
 * <pre>{@code
 * ButtonConfig config = new ButtonConfig(
 *     ButtonIcon.SAVE,
 *     "Save File",
 *     () -> saveFile());
 * }</pre>
 *
 * @see ButtonConfig
 * @see fr.inria.corese.gui.core.ButtonFactory
 */
public enum ButtonIcon {

  // ===============================================================================
  // Enum Constants - File Operations
  // ===============================================================================

  /** Icon for save/save-as operations. */
  SAVE(MaterialDesignC.CONTENT_SAVE_OUTLINE),

  /** Icon for opening files. */
  OPEN_FILE(MaterialDesignF.FILE),

  /** Icon for importing data/files. */
  IMPORT(MaterialDesignI.IMPORT),

  /** Icon for exporting data/files. */
  EXPORT(MaterialDesignE.EXPORT),

  // ===============================================================================
  // Enum Constants - Editor Operations
  // ===============================================================================

  /** Icon for clearing/deleting content. */
  CLEAR(MaterialDesignB.BROOM),

  /** Icon for undo operations. */
  UNDO(MaterialDesignU.UNDO),

  /** Icon for redo operations. */
  REDO(MaterialDesignR.REDO),

  /** Icon for zoom in operations. */
  ZOOM_IN(MaterialDesignM.MAGNIFY_PLUS_OUTLINE),

  /** Icon for zoom out operations. */
  ZOOM_OUT(MaterialDesignM.MAGNIFY_MINUS_OUTLINE),

  // ===============================================================================
  // Enum Constants - Navigation & Information
  // ===============================================================================

  /** Icon for documentation/help links. */
  DOCUMENTATION(MaterialDesignL.LINK),

  /** Icon for reload/refresh operations. */
  RELOAD(MaterialDesignR.REFRESH),

  /** Icon for viewing logs. */
  LOGS(MaterialDesignB.BOOK_OPEN_VARIANT),

  /** Icon for first page navigation. */
  FIRST_PAGE(MaterialDesignC.CHEVRON_DOUBLE_LEFT),

  /** Icon for previous page navigation. */
  PREVIOUS_PAGE(MaterialDesignC.CHEVRON_LEFT),

  /** Icon for next page navigation. */
  NEXT_PAGE(MaterialDesignC.CHEVRON_RIGHT),

  /** Icon for last page navigation. */
  LAST_PAGE(MaterialDesignC.CHEVRON_DOUBLE_RIGHT),

  /** Close Window */
  CLOSE_WINDOW(MaterialDesignW.WINDOW_CLOSE),

  // ===============================================================================
  // Enum Constants - Actions
  // ===============================================================================

  /** Icon for delete/remove operations. */
  DELETE(MaterialDesignT.TRASH_CAN),

  /** Icon for copy to clipboard operations. */
  COPY(MaterialDesignC.CONTENT_COPY),

  /** Icon for play/execute/run operations. */
  PLAY(MaterialDesignP.PLAY);

  // ===============================================================================
  // Fields
  // ===============================================================================

  /** The underlying Ikonli icon instance. */
  private final Ikon ikon;

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Creates a button icon with the specified Ikonli icon.
   *
   * @param ikon The Ikonli icon to associate with this button icon
   */
  ButtonIcon(Ikon ikon) {
    this.ikon = ikon;
  }

  // ===============================================================================
  // Public API
  // ===============================================================================

  /**
   * Returns the Ikonli icon associated with this button icon.
   *
   * @return The Ikonli icon instance
   */
  public Ikon getIkon() {
    return ikon;
  }
}
