package fr.inria.corese.demo.view;

import fr.inria.corese.demo.view.base.AbstractView;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the sidebar layout of the Corese-GUI application.
 *
 * <p>This view contains the navigation buttons and the application logo. Its visual style (colors,
 * typography, hover states) is fully provided by the active Atlantafx theme and {@code
 * navigation-bar.css}.
 *
 * <p>Layout structure:
 *
 * <pre>
 * +------------------------------+
 * | VBox (root)                 |
 * |  +------------------------+ |
 * |  |  ImageView (logo)      | |
 * |  +------------------------+ |
 * |  |  Button (Data)         | |
 * |  |  Button (RDF Editor)   | |
 * |  |  Button (Validation)   | |
 * |  |  Button (Query)        | |
 * |  |  [spacer]              | |
 * |  |  Button (Settings)     | |
 * +------------------------------+
 * </pre>
 */
public final class NavigationBarView extends AbstractView {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationBarView.class);

  /** Path to the stylesheet for this view. */
  private static final String STYLESHEET_PATH = "/styles/navigation-bar.css";

  // ===== Buttons =====
  private final Button dataButton = createButton("Data");
  private final Button rdfEditorButton = createButton("RDF Editor");
  private final Button validationButton = createButton("Validation");
  private final Button queryButton = createButton("Query");
  private final Button settingsButton = createButton("Settings");

  // ===== Constructor =====
  public NavigationBarView() {
    super(new VBox(8), STYLESHEET_PATH);
    initializeLayout();
  }

  // ===== Initialization =====

  /** Configures layout hierarchy and spacing for the navigation sidebar. */
  private void initializeLayout() {
    VBox rootBox = (VBox) getRoot();
    rootBox.getStyleClass().addAll("sidebar", "surface");
    rootBox.setAlignment(Pos.TOP_CENTER);

    ImageView logo = createLogo();
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    rootBox
        .getChildren()
        .setAll(
            logo,
            dataButton,
            rdfEditorButton,
            validationButton,
            queryButton,
            spacer,
            settingsButton);
  }

  /** Loads and configures the application logo. */
  private ImageView createLogo() {
    ImageView logoView = new ImageView();
    try {
      Image logoImg =
          new Image(
              Objects.requireNonNull(
                  getClass().getResourceAsStream("/images/logo.png"), "Logo resource not found"));
      logoView.setImage(logoImg);
      logoView.setFitWidth(48);
      logoView.setFitHeight(48);
      VBox.setMargin(logoView, new Insets(0, 0, 16, 0));
    } catch (Exception e) {
      LOGGER.error("Failed to load logo image.", e);
    }
    return logoView;
  }

  /** Creates a navigation button with default Atlantafx styling. */
  private Button createButton(String text) {
    Button button = new Button(text);
    button.getStyleClass().add("flat"); // neutral Atlantafx style
    button.setMaxWidth(Double.MAX_VALUE); // take full width
    button.setAlignment(Pos.CENTER_LEFT);
    return button;
  }

  // ===== Selection Management =====

  /** Highlights the active navigation button using the "accent" style. */
  public void setButtonSelected(Button selectedButton) {
    getRoot()
        .lookupAll(".button")
        .forEach(
            node -> {
              if (node instanceof Button b) {
                b.getStyleClass().remove("accent");
              }
            });

    if (selectedButton != null) {
      selectedButton.getStyleClass().add("accent");
    }
  }

  /** Returns the button associated with a specific {@link ViewId}. */
  public Button getButtonForView(ViewId view) {
    return switch (view) {
      case DATA -> dataButton;
      case RDF_EDITOR -> rdfEditorButton;
      case VALIDATION -> validationButton;
      case QUERY -> queryButton;
      case SETTINGS -> settingsButton;
    };
  }

  // ===== Accessors =====
  public Button getDataButton() {
    return dataButton;
  }

  public Button getRdfEditorButton() {
    return rdfEditorButton;
  }

  public Button getValidationButton() {
    return validationButton;
  }

  public Button getQueryButton() {
    return queryButton;
  }

  public Button getSettingsButton() {
    return settingsButton;
  }
}
