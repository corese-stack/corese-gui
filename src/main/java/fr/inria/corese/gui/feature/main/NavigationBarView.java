package fr.inria.corese.gui.feature.main;

import fr.inria.corese.gui.core.enums.ViewId;

import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.SvgImageLoader;







import atlantafx.base.theme.Styles;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View component for the sidebar (navigation bar) of the Corese-GUI application.
 *
 * <p><strong>Responsibilities:</strong>
 *
 * <ul>
 *   <li>Defines the UI layout and structure
 *   <li>Manages collapse/expand animations
 *   <li>Handles visual state (active button highlighting)
 * </ul>
 *
 * <p><strong>Does NOT handle:</strong> Business logic and event coordination are delegated to
 * {@link fr.inria.corese.gui.feature.main.NavigationBarController}.
 *
 * <p>This class follows the MVC pattern as a pure View component.
 */
public final class NavigationBarView extends AbstractView {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationBarView.class);

  private static final String STYLESHEET_PATH = "/styles/navigation-bar.css";

  // ==== State ====
  private boolean collapsed = false;
  private ParallelTransition currentTransition;
  private Consumer<Boolean> onToggle;
  private Runnable onLogoClick;

  // ==== UI elements ====
  private final Button logo;
  private final Button dataButton;
  private final Button queryButton;
  private final Button validationButton;
  private final Button toggleButton;
  private final Button settingsButton;

  // ==== Constructor ====

  /** Creates a new NavigationBarView instance and initializes the layout. */
  public NavigationBarView() {
    super(new VBox(), STYLESHEET_PATH);

    this.logo = createLogoButton();
    this.dataButton =
        createNavigationButton("Data", MaterialDesignD.DATABASE, "Load and manage RDF data");
    this.queryButton =
        createNavigationButton(
            "Query", MaterialDesignM.MAGNIFY, "Execute SPARQL queries on loaded RDF datasets");
    this.validationButton =
        createNavigationButton(
            "Validation",
            MaterialDesignS.SHIELD_CHECK,
            "Validate RDF data against SHACL shapes and constraints");
    this.toggleButton = createToggleButton();
    this.settingsButton =
        createNavigationButton(
            "Settings", MaterialDesignC.COG, "Configure application preferences and appearance");

    initializeLayout();
  }

  // ===== Layout & creation =====

  /** Configures the sidebar layout structure. */
  private void initializeLayout() {
    VBox root = (VBox) getRoot();
    root.getStyleClass().add("sidebar");
    root.setFillWidth(true);

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    root.getChildren()
        .setAll(
            logo, dataButton, queryButton, validationButton, spacer, toggleButton, settingsButton);
  }

  /** Creates the top logo button (non-interactive except logging). */
  private Button createLogoButton() {
    Button button = new Button();
    button.getStyleClass().addAll("sidebar-logo", Styles.FLAT);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setAlignment(Pos.CENTER);

    try {
      double logoSize = NavigationBarAnimations.getLogoExpandedSize();
      // Load SVG with 2x scaling for high DPI
      Image image = SvgImageLoader.loadSvgImage("/images/corese-logo.svg", logoSize, logoSize, 2.0);

      if (image != null) {
        ImageView view = new ImageView(image);
        view.getStyleClass().add("app-logo");
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(logoSize);
        view.setFitHeight(logoSize);
        button.setGraphic(view);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to load logo image", e);
    }

    button.setOnAction(
        e -> {
          if (onLogoClick != null) {
            onLogoClick.run();
          }
        });
    return button;
  }

  /** Creates a sidebar button with icon and text. */
  private Button createNavigationButton(String text, Ikon iconCode, String tooltipText) {
    Button button = new Button();
    button.getStyleClass().addAll("sidebar-button", Styles.FLAT, Styles.LEFT_PILL);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setAlignment(Pos.CENTER_LEFT);

    FontIcon icon = new FontIcon(iconCode);
    icon.getStyleClass().add("sidebar-icon");

    Label label = new Label(text);
    label.getStyleClass().add("sidebar-text");

    HBox content = new HBox(icon, label);
    content.getStyleClass().add("sidebar-button-content");
    content.setAlignment(Pos.CENTER_LEFT);

    button.setGraphic(content);
    button.setUserData(label);
    button.setTooltip(new Tooltip(tooltipText));

    return button;
  }

  /** Creates the toggle button for collapse/expand. */
  private Button createToggleButton() {
    Button button = new Button();
    button.getStyleClass().addAll("sidebar-toggle", Styles.FLAT, Styles.ROUNDED);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setAlignment(Pos.CENTER);

    FontIcon icon = new FontIcon(Feather.CHEVRONS_LEFT);
    icon.getStyleClass().add("sidebar-icon");
    button.setGraphic(icon);

    button.setTooltip(new Tooltip("Collapse sidebar"));
    button.setOnAction(
        e -> {
          if (onToggle != null) {
            onToggle.accept(!collapsed);
          }
        });

    return button;
  }

  // ===== Collapse / expand =====

  /** Allows the controller/model to set collapse state. */
  public void setCollapsed(boolean value) {
    if (this.collapsed != value) {
      toggleSidebar();
    }
  }

  /** Performs the collapse/expand animation. */
  private void toggleSidebar() {
    if (currentTransition != null && currentTransition.getStatus() == Animation.Status.RUNNING) {
      currentTransition.stop();
    }

    collapsed = !collapsed;

    VBox root = (VBox) getRoot();
    FontIcon toggleIcon = (FontIcon) toggleButton.getGraphic();
    ImageView logoView = (ImageView) logo.getGraphic();

    Button[] navigationButtons =
        new Button[] {dataButton, validationButton, queryButton, settingsButton};

    currentTransition =
        NavigationBarAnimations.createToggleAnimation(
            root, toggleIcon, logoView, navigationButtons, collapsed);

    currentTransition.setOnFinished(
        e -> {
          if (collapsed) {
            root.getStyleClass().add("collapsed");
            toggleButton.getTooltip().setText("Expand sidebar");
          } else {
            root.getStyleClass().remove("collapsed");
            toggleButton.getTooltip().setText("Collapse sidebar");
          }
          currentTransition = null;
        });

    currentTransition.play();
  }

  // ===== Selection & handlers =====

  /**
   * Sets the active view and highlights the corresponding button.
   *
   * @param viewId the view to highlight
   */
  public void setActiveView(ViewId viewId) {
    Button targetButton = getButtonForViewId(viewId);
    if (targetButton == null) {
      return;
    }

    // Remove accent from all buttons
    getRoot()
        .lookupAll(".sidebar-button")
        .forEach(
            node -> {
              if (node instanceof Button button) {
                button.getStyleClass().remove(Styles.ACCENT);
              }
            });

    // Highlight the target button
    targetButton.getStyleClass().add(Styles.ACCENT);
  }

  /**
   * Sets the navigation handler to be called when a navigation button is clicked.
   *
   * @param handler the navigation handler
   */
  public void setNavigationHandler(Consumer<ViewId> handler) {
    for (ViewId id : ViewId.values()) {
      Button button = getButtonForViewId(id);
      if (button != null) {
        button.setOnAction(
            e -> {
              if (handler != null) {
                handler.accept(id);
              }
            });
      }
    }
  }

  /**
   * Returns the button corresponding to the given view ID.
   *
   * @param viewId the view ID
   * @return the corresponding button
   */
  private Button getButtonForViewId(ViewId viewId) {
    return switch (viewId) {
      case DATA -> dataButton;
      case VALIDATION -> validationButton;
      case QUERY -> queryButton;
      case SETTINGS -> settingsButton;
    };
  }

  /**
   * Returns whether the sidebar is currently collapsed.
   *
   * @return true if collapsed, false otherwise
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Sets the toggle handler to be called when the sidebar is collapsed or expanded.
   *
   * @param handler the toggle handler
   */
  public void setOnToggle(Consumer<Boolean> handler) {
    this.onToggle = handler;
  }

  /**
   * Sets the handler to be called when the logo is clicked.
   *
   * @param handler the logo click handler
   */
  public void setOnLogoClick(Runnable handler) {
    this.onLogoClick = handler;
  }
}
