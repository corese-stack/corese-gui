package fr.inria.corese.gui.enums.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.*;

/**
 * Enumeration of button icons used throughout the application.
 *
 * <p>This enum provides a centralized mapping between semantic button actions and their visual icon
 * representations. It serves as an abstraction layer over the underlying icon library (Material
 * Design Icons), making it easy to:
 *
 * <ul>
 *   <li>Change icons globally by modifying only this enum
 *   <li>Ensure consistent icon usage across the application
 *   <li>Switch to a different icon library if needed
 * </ul>
 *
 * <p><b>Design rationale:</b> The enum name is {@code ButtonIcon} (not {@code IconButtonType})
 * because it represents the <i>icon itself</i>, not a "type of button". This shorter, more semantic
 * naming improves code readability.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * ButtonConfig config = new ButtonConfig(
 *     ButtonIcon.SAVE,
 *     "Save File",
 *     () -> saveFile()
 * );
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

  // ===============================================================================
  // Enum Constants - Navigation & Information
  // ===============================================================================

  /** Icon for documentation/help links. */
  DOCUMENTATION(MaterialDesignL.LINK),

  /** Icon for reload/refresh operations. */
  RELOAD(MaterialDesignR.REFRESH),

  /** Icon for viewing logs. */
  LOGS(MaterialDesignB.BOOK_OPEN_VARIANT),

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
