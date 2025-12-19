package fr.inria.corese.gui.view;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A Floating Action Button (FAB) component.
 * <p>
 * This button is circular, elevated, and typically used for the primary action on a screen.
 * It supports a loading state where the icon is replaced by a {@link RingProgressIndicator}.
 * </p>
 */
public class FloatingButton extends Button {

    // ==============================================================================================
    // Constants
    // ==============================================================================================

    private static final String STYLESHEET = "/styles/floating-button.css";
    private static final String STYLE_CLASS = "floating-button";
    private static final int ICON_SIZE = 24;
    private static final int PROGRESS_SIZE = 24;

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private final FontIcon fontIcon;
    private final RingProgressIndicator progressIndicator;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    /**
     * Creates a new FloatingButton.
     *
     * @param icon        The icon to display.
     * @param tooltipText The text to display in the tooltip (can be null).
     */
    public FloatingButton(Ikon icon, String tooltipText) {
        this.fontIcon = new FontIcon(icon);
        this.progressIndicator = new RingProgressIndicator();

        initialize(tooltipText);
        setupListeners();
    }

    // ==============================================================================================
    // Initialization
    // ==============================================================================================

    private void initialize(String tooltipText) {
        // Load styles
        getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        getStyleClass().addAll(STYLE_CLASS, Styles.BUTTON_CIRCLE);

        // Configure Icon
        fontIcon.setIconSize(ICON_SIZE);

        // Configure Progress Indicator
        progressIndicator.setMinSize(PROGRESS_SIZE, PROGRESS_SIZE);
        progressIndicator.setMaxSize(PROGRESS_SIZE, PROGRESS_SIZE);

        // Set initial graphic
        setGraphic(fontIcon);

        // Configure Tooltip
        if (tooltipText != null && !tooltipText.isBlank()) {
            setTooltip(new Tooltip(tooltipText));
        }
    }

    private void setupListeners() {
        loading.addListener((obs, oldVal, newVal) -> updateGraphic(newVal));
    }

    // ==============================================================================================
    // Helper Methods
    // ==============================================================================================

    private void updateGraphic(boolean isLoading) {
        if (isLoading) {
            // Ensure progress indicator is indeterminate or set progress if needed
            // For a simple loading state, indeterminate is usually what we want, 
            // but RingProgressIndicator might default to 0. 
            // If it's indeterminate by default or we want -1:
            progressIndicator.setProgress(-1); 
            setGraphic(progressIndicator);
            setDisable(true); // Usually good practice to disable FAB while loading
        } else {
            setGraphic(fontIcon);
            setDisable(false);
        }
    }

    // ==============================================================================================
    // Accessors
    // ==============================================================================================

    /**
     * Sets the loading state of the button.
     *
     * @param loading true to show the progress indicator, false to show the icon.
     */
    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    /**
     * @return true if the button is in loading state.
     */
    public boolean isLoading() {
        return loading.get();
    }

    /**
     * @return The loading property.
     */
    public BooleanProperty loadingProperty() {
        return loading;
    }
}
