package fr.inria.corese.gui.feature.main.navigation;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.core.enums.ViewId;
import fr.inria.corese.gui.utils.ThemeManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * JavaFX view for the sidebar navigation.
 *
 * <p>Displays:
 *
 * <ul>
 *   <li>a Corese logo at the top
 *   <li>navigation buttons that trigger callbacks when clicked
 *   <li>a toggle button (expand/collapse) that can be collapsed/expanded
 * </ul>
 *
 * <p>Animations are delegated to {@link NavigationBarAnimations}.
 */
public final class NavigationBarView {

  private static final Logger LOGGER = Logger.getLogger(NavigationBarView.class.getName());

  // ===== Constants =====

  private static final String LOGO_IMAGE_PATH = "/images/corese_logo.png";
  private static final double LOGO_SIZE_EXPANDED = 130;
  private static final double LOGO_SIZE_COLLAPSED = 50;

  private static final double SIDEBAR_WIDTH_EXPANDED = 180;
  private static final double SIDEBAR_WIDTH_COLLAPSED = 60;

  private static final Color SIDEBAR_BACKGROUND_LIGHT = Color.web("#f8f9fa");
  private static final Color SIDEBAR_BACKGROUND_DARK = Color.web("#2b2d30");

  private static final Color BUTTON_HOVER_LIGHT = Color.web("#e0e0e0");
  private static final Color BUTTON_HOVER_DARK = Color.web("#3c3f41");

  private static final Color ACTIVE_INDICATOR_LIGHT = Color.web("#007bff");
  private static final Color ACTIVE_INDICATOR_DARK = Color.web("#4a9eff");

  private static final String STYLE_CLASS_NAV_BUTTON = "nav-button";
  private static final String STYLE_CLASS_ACTIVE = "active";

  // ===== Properties =====

  /** Internal property tracking collapse state. */
  private final BooleanProperty collapsed = new SimpleBooleanProperty(false);

  /** Property tracking the currently active view button. */
  private final ObjectProperty<ViewId> activeView =
      new SimpleObjectProperty<>(ViewId.QUERY);

  // ===== UI Components =====

  private final VBox root = new VBox();
  private final StackPane logoContainer = new StackPane();
  private final ImageView logoImageView = new ImageView();
  private final VBox navButtonsContainer = new VBox();
  private final StackPane toggleButtonContainer = new StackPane();

  /** Map to keep track of navigation buttons for styling. */
  private final Map<ViewId, HBox> navButtons = new HashMap<>();

  // ===== Callbacks =====

  /** Handler to be invoked when a navigation button is clicked. */
  private Consumer<ViewId> navigationHandler;

  /** Handler to be invoked when the toggle button is clicked. */
  private Runnable onToggle;

  /** Handler to be invoked when the logo is clicked. */
  private Runnable onLogoClick;

  // ===== Constructor =====

  public NavigationBarView() {
    setupLogoArea();
    setupNavigationButtons();
    setupToggleButton();

    buildLayout();
    applyCurrentTheme();
    setupThemeListener();

    // Set initial collapsed state
    setCollapsed(false);
  }

  // ===== Logo Setup =====

  private void setupLogoArea() {
    logoImageView.setPreserveRatio(true);
    logoImageView.setSmooth(true);

    try {
      var logoUrl = getClass().getResource(LOGO_IMAGE_PATH);
      if (logoUrl == null) {
        LOGGER.warning("Logo image not found: " + LOGO_IMAGE_PATH);
      } else {
        logoImageView.setImage(new Image(logoUrl.toExternalForm()));
      }
    } catch (Exception e) {
      LOGGER.warning("Failed to load logo: " + e.getMessage());
    }

    // Make logo clickable with hand cursor
    logoContainer.setCursor(Cursor.HAND);
    logoContainer.setOnMouseClicked(event -> {
      if (onLogoClick != null) {
        onLogoClick.run();
      }
    });

    // Initial size
    logoImageView.setFitWidth(LOGO_SIZE_EXPANDED);
    logoImageView.setFitHeight(LOGO_SIZE_EXPANDED);

    logoContainer.getChildren().add(logoImageView);
    VBox.setMargin(logoContainer, new Insets(15, 0, 25, 0));
  }

  // ===== Navigation Buttons =====

  private void setupNavigationButtons() {
    navButtonsContainer.setSpacing(5);
    navButtonsContainer.setAlignment(Pos.TOP_CENTER);
    navButtonsContainer.setPadding(new Insets(0));

    // Create buttons for each view
    createNavButton(ViewId.QUERY, MaterialDesign.MDI_CODE_BRACES, "Query Editor");
    createNavButton(ViewId.SETTINGS, MaterialDesign.MDI_SETTINGS, "Settings");
    createNavButton(
        ViewId.VALIDATION, MaterialDesign.MDI_CHECK_CIRCLE_OUTLINE, "Validation");

    // Set default active view
    setActiveView(ViewId.QUERY);
  }

  /**
   * Creates a navigation button for the given view.
   *
   * @param viewId the view identifier
   * @param icon the FontAwesome icon
   * @param tooltipText the tooltip text
   */
  private void createNavButton(ViewId viewId, MaterialDesign icon, String tooltipText) {
    var iconNode = new FontIcon(icon);
    iconNode.setIconSize(24);
    iconNode.getStyleClass().add("nav-icon");

    var label = new Label(tooltipText);
    label.getStyleClass().add("nav-label");

    var button = new HBox(10, iconNode, label);
    button.setAlignment(Pos.CENTER_LEFT);
    button.setPadding(new Insets(12, 15, 12, 15));
    button.setCursor(Cursor.HAND);
    button.getStyleClass().add(STYLE_CLASS_NAV_BUTTON);

    button.setOnMouseClicked(
        event -> {
          if (navigationHandler != null) {
            navigationHandler.accept(viewId);
          }
        });

    Tooltip tooltip = new Tooltip(tooltipText);
    Tooltip.install(button, tooltip);

    navButtons.put(viewId, button);
    navButtonsContainer.getChildren().add(button);
  }

  // ===== Toggle Button =====

  private void setupToggleButton() {
    // Chevron icon (changes direction based on collapsed state)
    var chevronIcon = new FontIcon(MaterialDesign.MDI_CHEVRON_LEFT);
    chevronIcon.setIconSize(20);
    chevronIcon.getStyleClass().add("toggle-icon");

    // Rotate chevron when collapsed
    collapsed.addListener(
        (obs, oldVal, newVal) -> {
          if (newVal) {
            chevronIcon.setIconCode(MaterialDesign.MDI_CHEVRON_RIGHT);
          } else {
            chevronIcon.setIconCode(MaterialDesign.MDI_CHEVRON_LEFT);
          }
        });

    var toggleButton = new HBox(chevronIcon);
    toggleButton.setAlignment(Pos.CENTER);
    toggleButton.setPadding(new Insets(10));
    toggleButton.setCursor(Cursor.HAND);
    toggleButton.getStyleClass().add("toggle-button");

    toggleButton.setOnMouseClicked(
        event -> {
          setCollapsed(!collapsed.get());
          if (onToggle != null) {
            onToggle.run();
          }
        });

    Tooltip toggleTooltip = new Tooltip("Toggle Sidebar");
    Tooltip.install(toggleButton, toggleTooltip);

    var separator = new Separator();
    separator.setPadding(new Insets(10, 0, 10, 0));

    toggleButtonContainer.getChildren().addAll(separator, toggleButton);
    toggleButtonContainer.setAlignment(Pos.BOTTOM_CENTER);
  }

  // ===== Layout =====

  private void buildLayout() {
    root.setAlignment(Pos.TOP_CENTER);
    root.setPadding(new Insets(0));

    // Logo at top
    root.getChildren().add(logoContainer);

    // Navigation buttons in the middle (grows to fill space)
    VBox.setVgrow(navButtonsContainer, Priority.ALWAYS);
    root.getChildren().add(navButtonsContainer);

    // Toggle button at bottom
    root.getChildren().add(toggleButtonContainer);

    // Initial width
    root.setPrefWidth(SIDEBAR_WIDTH_EXPANDED);
    root.setMinWidth(SIDEBAR_WIDTH_EXPANDED);
    root.setMaxWidth(SIDEBAR_WIDTH_EXPANDED);
  }

  // ===== Theme Handling =====

  private void applyCurrentTheme() {
    Theme currentTheme = ThemeManager.getInstance().getTheme();
    boolean isDark = currentTheme != null && ThemeManager.getInstance().isDarkTheme(currentTheme.getName());
    Color bgColor = isDark ? SIDEBAR_BACKGROUND_DARK : SIDEBAR_BACKGROUND_LIGHT;

    root.setBackground(new Background(new BackgroundFill(bgColor, null, null)));

    // Update button styles
    for (HBox button : navButtons.values()) {
      updateButtonStyle(button, isDark);
    }
  }

  private void updateButtonStyle(HBox button, boolean isDark) {
    Color hoverColor = isDark ? BUTTON_HOVER_DARK : BUTTON_HOVER_LIGHT;

    button.setOnMouseEntered(
        event -> {
          if (!button.getStyleClass().contains(STYLE_CLASS_ACTIVE)) {
            button.setBackground(new Background(new BackgroundFill(hoverColor, null, null)));
          }
        });

    button.setOnMouseExited(
        event -> {
          if (!button.getStyleClass().contains(STYLE_CLASS_ACTIVE)) {
            button.setBackground(Background.EMPTY);
          }
        });
  }

  private void setupThemeListener() {
    ThemeManager.getInstance()
        .themeProperty()
        .addListener((obs, oldVal, newVal) -> applyCurrentTheme());
  }

  // ===== Public API =====

  /** Returns the root node for embedding. */
  public Parent getRoot() {
    return root;
  }

  /**
   * Sets the collapsed state of the sidebar.
   *
   * @param collapsed true to collapse, false to expand
   */
  public void setCollapsed(boolean collapsed) {
    this.collapsed.set(collapsed);

    if (collapsed) {
      NavigationBarAnimations.animateCollapse(
          root, logoImageView, navButtons, SIDEBAR_WIDTH_COLLAPSED, LOGO_SIZE_COLLAPSED);
    } else {
      NavigationBarAnimations.animateExpand(
          root, logoImageView, navButtons, SIDEBAR_WIDTH_EXPANDED, LOGO_SIZE_EXPANDED);
    }
  }

  /** Returns whether the sidebar is currently collapsed. */
  public boolean isCollapsed() {
    return collapsed.get();
  }

  /**
   * Highlights the given view as active.
   *
   * @param viewId the view to highlight
   */
  public void setActiveView(ViewId viewId) {
    Objects.requireNonNull(viewId, "viewId cannot be null");

    // Remove active styling from all buttons
    for (var entry : navButtons.entrySet()) {
      var button = entry.getValue();
      button.getStyleClass().remove(STYLE_CLASS_ACTIVE);
      button.setBackground(Background.EMPTY);
    }

    // Add active styling to the selected button
    var activeButton = navButtons.get(viewId);
    if (activeButton != null) {
      activeButton.getStyleClass().add(STYLE_CLASS_ACTIVE);

      Theme currentTheme = ThemeManager.getInstance().getTheme();
      boolean isDark = currentTheme != null && ThemeManager.getInstance().isDarkTheme(currentTheme.getName());
      Color indicatorColor = isDark ? ACTIVE_INDICATOR_DARK : ACTIVE_INDICATOR_LIGHT;

      activeButton.setBackground(
          new Background(
              new BackgroundFill(
                  isDark ? BUTTON_HOVER_DARK : BUTTON_HOVER_LIGHT, null, null)));

      // Add left border indicator
      activeButton.setBorder(
          new Border(
              new BorderStroke(
                  indicatorColor,
                  Color.TRANSPARENT,
                  Color.TRANSPARENT,
                  Color.TRANSPARENT,
                  BorderStrokeStyle.SOLID,
                  BorderStrokeStyle.NONE,
                  BorderStrokeStyle.NONE,
                  BorderStrokeStyle.NONE,
                  null,
                  new BorderWidths(0, 0, 0, 4),
                  null)));
    }

    activeView.set(viewId);
  }

  /**
   * Sets the navigation handler callback.
   *
   * @param handler the callback invoked when a button is clicked
   */
  public void setNavigationHandler(Consumer<ViewId> handler) {
    this.navigationHandler = handler;
  }

  /**
   * Sets the toggle handler callback.
   *
   * @param onToggle the callback invoked when the toggle button is clicked
   */
  public void setOnToggle(Runnable onToggle) {
    this.onToggle = onToggle;
  }

  /**
   * Sets the logo click handler callback.
   *
   * @param onLogoClick the callback invoked when the logo is clicked
   */
  public void setOnLogoClick(Runnable onLogoClick) {
    this.onLogoClick = onLogoClick;
  }
}
