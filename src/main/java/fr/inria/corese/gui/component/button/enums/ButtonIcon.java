package fr.inria.corese.gui.component.button.enums;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import fr.inria.corese.gui.component.button.config.ButtonConfig;

/**
 * Enumeration of button icons used throughout the application.
 *
 * <p>
 * This enum provides a centralized mapping between semantic button actions and
 * their visual icon representations. It serves as an abstraction layer over the
 * underlying icon library (Material Design Icons), making it easy to:
 *
 * <ul>
 * <li>Change icons globally by modifying only this enum
 * <li>Ensure consistent icon usage across the application
 * <li>Switch to a different icon library if needed
 * </ul>
 *
 * <p>
 * <b>Design rationale:</b> The enum name is {@code ButtonIcon} (not
 * {@code IconButtonType}) because it represents the <i>icon itself</i>, not a
 * "type of button". This shorter, more semantic naming improves code
 * readability.
 *
 * <p>
 * <b>Usage example:</b>
 *
 * @see ButtonConfig
 * @see fr.inria.corese.gui.component.button.factory.ButtonFactory
 */
public enum ButtonIcon {

    // ===============================================================================
    // Enum Constants - File Operations
    // ===============================================================================

    /** Icon for save/save-as operations. */
    SAVE(MaterialDesignC.CONTENT_SAVE_OUTLINE),

    /** Icon for opening files. */
    OPEN_FILE(MaterialDesignF.FOLDER_OPEN_OUTLINE),

    /** Icon for creating a new editor tab. */
    NEW_TAB(MaterialDesignP.PLUS),

    /** Icon for importing data/files. */
    IMPORT(MaterialDesignI.IMPORT),

    /** Icon for exporting data/files. */
    EXPORT(MaterialDesignE.EXPORT),

    // ===============================================================================
    // Enum Constants - Editor Operations
    // ===============================================================================

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

    /** Icon for re-energizing graph layout forces. */
    LAYOUT_FORCE(MaterialDesignL.LIGHTNING_BOLT_OUTLINE),

    /** Icon for navigating to the data view. */
    NAV_DATA(MaterialDesignD.DATABASE),

    /** Icon for navigating to the query view. */
    NAV_QUERY(MaterialDesignM.MAGNIFY),

    /** Icon for navigating to the validation view. */
    NAV_VALIDATION(MaterialDesignS.SHIELD_CHECK),

    /** Icon for navigating to settings view. */
    NAV_SETTINGS(MaterialDesignC.COG),

    /** Icon for collapsing/expanding side navigation. */
    NAV_TOGGLE(MaterialDesignC.CHEVRON_DOUBLE_LEFT),

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
    // Enum Constants - Views & Status
    // ===============================================================================

    /** Icon for textual result tabs. */
    RESULT_TEXT(MaterialDesignF.FILE_DOCUMENT_OUTLINE),

    /** Icon for tabular result tabs. */
    RESULT_TABLE(MaterialDesignT.TABLE_LARGE),

    /** Icon for graph result tabs. */
    RESULT_GRAPH(MaterialDesignG.GRAPH_OUTLINE),

    /** Icon for query empty states. */
    EMPTY_QUERY(MaterialDesignF.FILE_DOCUMENT_OUTLINE),

    /** Icon for validation empty states. */
    EMPTY_VALIDATION(MaterialDesignS.SHIELD_CHECK_OUTLINE),

    /** Icon for "new" action buttons in empty states. */
    EMPTY_ACTION_NEW(MaterialDesignP.PLUS),

    /** Icon for "open/load" action buttons in empty states. */
    EMPTY_ACTION_OPEN(MaterialDesignF.FOLDER_OPEN),

    /** Icon for light theme mode. */
    THEME_LIGHT(MaterialDesignW.WEATHER_SUNNY),

    /** Icon for dark theme mode. */
    THEME_DARK(MaterialDesignW.WEATHER_NIGHT),

    /** Icon for website links. */
    LINK_WEBSITE(MaterialDesignW.WEB),

    /** Icon for GitHub links. */
    LINK_GITHUB(MaterialDesignG.GITHUB),

    /** Icon for issue tracker links. */
    LINK_ISSUES(MaterialDesignA.ALERT_CIRCLE_OUTLINE),

    /** Icon for forum/community links. */
    LINK_FORUM(MaterialDesignF.FORUM_OUTLINE),

    /** Icon for template actions in colored action buttons. */
    TEMPLATE(MaterialDesignF.FILE_DOCUMENT_MULTIPLE),

    /** Icon for informational notifications. */
    NOTIFICATION_INFO(MaterialDesignI.INFORMATION_OUTLINE),

    /** Icon for success notifications. */
    NOTIFICATION_SUCCESS(MaterialDesignC.CHECK_CIRCLE_OUTLINE),

    /** Icon for warning notifications. */
    NOTIFICATION_WARNING(MaterialDesignA.ALERT_OUTLINE),

    /** Icon for error notifications. */
    NOTIFICATION_ERROR(MaterialDesignC.CLOSE_CIRCLE_OUTLINE),

    // ===============================================================================
    // Enum Constants - Actions
    // ===============================================================================

    /** Icon for copy to clipboard operations. */
    COPY(MaterialDesignC.CONTENT_COPY),

    /** Icon for copy selection operations. */
    COPY_SELECTION(MaterialDesignC.CONTENT_DUPLICATE),

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
     * @param ikon
     *            The Ikonli icon to associate with this button icon
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
