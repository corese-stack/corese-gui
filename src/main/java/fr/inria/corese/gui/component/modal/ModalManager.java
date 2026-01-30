package fr.inria.corese.gui.component.modal;

import atlantafx.base.controls.ModalPane;
import javafx.scene.Node;

/**
 * Singleton manager for the application's global ModalPane.
 * <p>
 * This class allows any component to trigger a modal dialog without knowing about the root layout.
 * It wraps an AtlantaFX {@link ModalPane}.
 */
public class ModalManager {
    
    private static ModalManager instance;
    private ModalPane modalPane;

    private ModalManager() {
        // Private constructor
    }

    /**
     * @return The singleton instance of ModalManager.
     */
    public static synchronized ModalManager getInstance() {
        if (instance == null) {
            instance = new ModalManager();
        }
        return instance;
    }

    /**
     * Registers the root ModalPane of the application.
     * Should be called once during main view initialization.
     * 
     * @param modalPane The AtlantaFX ModalPane instance.
     */
    public void setModalPane(ModalPane modalPane) {
        this.modalPane = modalPane;
    }

    /**
     * Displays a node as a modal dialog.
     * 
     * @param content The content to display.
     */
    public void show(Node content) {
        if (modalPane != null) {
            modalPane.show(content);
            content.requestFocus();
        }
    }

    /**
     * Hides the currently active modal.
     */
    public void hide() {
        if (modalPane != null) {
            modalPane.hide();
        }
    }
    
    public ModalPane getModalPane() {
        return modalPane;
    }
}
