package fr.inria.corese.gui.feature.settings;

import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.BrowserUtils;
import fr.inria.corese.gui.utils.SvgImageLoader;






import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings view for configuring application preferences.
 *
 * <p>This view follows the MVC pattern:
 *
 * <ul>
 *   <li><b>Model:</b> {@link SettingsModel} - stores settings data
 *   <li><b>View:</b> This class - displays UI
 *   <li><b>Controller:</b> {@link SettingsController} - handles business logic
 * </ul>
 */
public final class SettingsView extends AbstractView {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);
  private static final String STYLESHEET_PATH = "/styles/settings-view.css";

  // ===== UI Components =====
  private ToggleSwitch systemThemeSwitch;
  private ComboBox<String> themeComboBox;
  private ToggleButton lightModeButton;
  private ToggleButton darkModeButton;
  private ColorPicker accentColorPicker;

  // ===== Constructor =====

  public SettingsView() {
    super(new ScrollPane(), STYLESHEET_PATH);
    initializeLayout();
  }

  // ===== Initialization =====

  private void initializeLayout() {
    ScrollPane scrollPane = (ScrollPane) getRoot();
    scrollPane.setFitToWidth(true);

    VBox content = new VBox();
    content.getStyleClass().add("settings-content");

    content.getChildren().addAll(createAppearanceSection(), createAboutSection());

    scrollPane.setContent(content);
  }

  // ===== Appearance Section =====

  private VBox createAppearanceSection() {
    VBox section = new VBox();
    section.getStyleClass().add("settings-section");

    Label sectionTitle = new Label("Appearance");
    sectionTitle.getStyleClass().add(Styles.TITLE_3);

    // System Theme Tile
    Tile systemThemeTile = new Tile("System Theme", "Use system theme and accent color");
    systemThemeSwitch = new ToggleSwitch();
    systemThemeTile.setAction(systemThemeSwitch);

    // Theme Tile
    Tile themeTile = new Tile("Theme", "Select application interface theme");

    themeComboBox = new ComboBox<>();

    // Mode Toggle Group
    ToggleGroup modeGroup = new ToggleGroup();

    lightModeButton = new ToggleButton(null, new FontIcon(Feather.SUN));
    lightModeButton.getStyleClass().addAll(Styles.LEFT_PILL);
    lightModeButton.setToggleGroup(modeGroup);

    darkModeButton = new ToggleButton(null, new FontIcon(Feather.MOON));
    darkModeButton.getStyleClass().addAll(Styles.RIGHT_PILL);
    darkModeButton.setToggleGroup(modeGroup);

    // Enforce selection
    modeGroup
        .selectedToggleProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal == null) oldVal.setSelected(true);
            });

    HBox modeBox = new HBox(lightModeButton, darkModeButton);
    modeBox.setAlignment(Pos.CENTER_LEFT);

    HBox themeActions = new HBox(15, themeComboBox, modeBox);
    themeActions.setAlignment(Pos.CENTER_RIGHT);
    themeTile.setAction(themeActions);

    // Accent Color Tile
    Tile accentColorTile = new Tile("Accent Color", "Choose your preferred accent color");

    accentColorPicker = new ColorPicker();
    accentColorTile.setAction(accentColorPicker);

    section.getChildren().addAll(sectionTitle, systemThemeTile, themeTile, accentColorTile);
    return section;
  }

  // ===== Getters for Controller =====

  public ToggleSwitch getSystemThemeSwitch() {
    return systemThemeSwitch;
  }

  public ComboBox<String> getThemeComboBox() {
    return themeComboBox;
  }

  public ToggleButton getLightModeButton() {
    return lightModeButton;
  }

  public ToggleButton getDarkModeButton() {
    return darkModeButton;
  }

  public ColorPicker getAccentColorPicker() {
    return accentColorPicker;
  }


  // ===== About Section =====

  private VBox createAboutSection() {
    VBox section = new VBox();
    section.getStyleClass().add("settings-section");

    Label sectionTitle = new Label("About");
    sectionTitle.getStyleClass().add(Styles.TITLE_3);

    Tile aboutTile = new Tile(AppConstants.APP_NAME, "Version " + AppConstants.APP_VERSION);
    aboutTile.setGraphic(createLogo());

    HBox linksBox = new HBox(10);
    linksBox.setAlignment(Pos.CENTER_RIGHT);
    linksBox
        .getChildren()
        .addAll(
            createLinkButton("Website", AppConstants.WEBSITE_URL, Feather.GLOBE),
            createLinkButton("GitHub", AppConstants.GITHUB_URL, Feather.GITHUB),
            createLinkButton("Issues", AppConstants.ISSUES_URL, Feather.ALERT_CIRCLE),
            createLinkButton("Forum", AppConstants.FORUM_URL, Feather.MESSAGE_CIRCLE));

    aboutTile.setAction(linksBox);

    section.getChildren().addAll(sectionTitle, aboutTile);
    return section;
  }

  private ImageView createLogo() {
    ImageView coreseLogo = new ImageView();
    coreseLogo.setFitWidth(64);
    coreseLogo.setFitHeight(64);
    coreseLogo.setPreserveRatio(true);
    coreseLogo.setSmooth(true);
    coreseLogo.getStyleClass().add("app-logo");

    try {
      // Load SVG with 2x scaling
      Image logoImage = SvgImageLoader.loadSvgImage("/images/corese-gui-logo.svg", 64, 64, 2.0);
      if (logoImage != null) {
        coreseLogo.setImage(logoImage);
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to load logo", e);
    }

    return coreseLogo;
  }

  private Button createLinkButton(String text, String url, Feather icon) {
    Button button = new Button(text, new FontIcon(icon));
    button.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
    button.setOnAction(e -> openURL(url));
    return button;
  }

  private void openURL(String url) {
    BrowserUtils.openUrl(url);
  }
}
