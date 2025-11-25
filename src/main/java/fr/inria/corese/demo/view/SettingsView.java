package fr.inria.corese.demo.view;

import atlantafx.base.theme.Styles;
import fr.inria.corese.demo.AppConstants;
import fr.inria.corese.demo.controller.SettingsController;
import fr.inria.corese.demo.model.SettingsModel;
import fr.inria.corese.demo.view.base.AbstractView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings view for configuring application preferences.
 *
 * <p>This view follows the MVC pattern:
 * <ul>
 *   <li><b>Model:</b> {@link SettingsModel} - stores settings data
 *   <li><b>View:</b> This class - displays UI
 *   <li><b>Controller:</b> {@link SettingsController} - handles business logic
 * </ul>
 */
public final class SettingsView extends AbstractView {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);
  private static final String STYLESHEET_PATH = "/styles/settings-view.css";

  // ===== MVC Components =====
  private final SettingsModel model;
  private final SettingsController controller;

  // ===== UI Components =====
  private ComboBox<String> themeComboBox;
  private ToggleButton lightBtn;
  private ToggleButton darkBtn;
  private HBox colorSelectionBox;

  // ===== Constructor =====

  public SettingsView() {
    super(new ScrollPane(), STYLESHEET_PATH);
    
    this.model = new SettingsModel();
    this.controller = new SettingsController(model);

    initializeLayout();
    setupBindings();
    updateControlsDisabledState();
  }

  // ===== Initialization =====

  private void initializeLayout() {
    ScrollPane scrollPane = (ScrollPane) getRoot();
    scrollPane.getStyleClass().add("settings-scroll-pane");
    scrollPane.setFitToWidth(true);

    VBox content = new VBox();
    content.getStyleClass().add("settings-content");

    content.getChildren().addAll(
        createAppearanceSection(),
        createAboutSection()
    );
    
    scrollPane.setContent(content);
  }

  private void setupBindings() {
    model.themeProperty().addListener((obs, oldTheme, newTheme) -> {
      if (newTheme != null) updateThemeSelection();
    });

    model.accentColorProperty().addListener((obs, oldColor, newColor) -> {
      if (newColor != null) updateAccentColorSelection();
    });
    
    model.useSystemThemeProperty().addListener((obs, oldValue, newValue) -> updateControlsDisabledState());
  }

  // ===== Appearance Section =====

  private VBox createAppearanceSection() {
    VBox section = new VBox();
    section.getStyleClass().add("settings-section");

    Label sectionTitle = new Label("Appearance");
    sectionTitle.getStyleClass().add(Styles.TITLE_3);

    // System Theme Checkbox
    CheckBox systemThemeCheckBox = new CheckBox("Use system theme and accent color");
    systemThemeCheckBox.getStyleClass().add("system-theme-checkbox");
    systemThemeCheckBox.selectedProperty().bindBidirectional(model.useSystemThemeProperty());

    // Theme Selection
    VBox themeBox = createThemeControl();
    
    // Accent Color Selection
    VBox accentBox = createAccentColorControl();

    section.getChildren().addAll(sectionTitle, systemThemeCheckBox, themeBox, accentBox);
    return section;
  }

  private VBox createThemeControl() {
    VBox container = new VBox();
    container.getStyleClass().add("theme-control-container");

    Label label = new Label("Theme");
    label.getStyleClass().add("control-label");

    HBox controls = new HBox();
    controls.getStyleClass().add("theme-controls-row");

    themeComboBox = new ComboBox<>();
    themeComboBox.getStyleClass().add("theme-combo-box");
    themeComboBox.getItems().addAll(controller.getBaseThemes());
    themeComboBox.setOnAction(e -> handleThemeChange());

    // Variant Buttons
    lightBtn = createVariantButton(Feather.SUN, "Light mode", false);
    darkBtn = createVariantButton(Feather.MOON, "Dark mode", true);

    ToggleGroup variantGroup = new ToggleGroup();
    lightBtn.setToggleGroup(variantGroup);
    darkBtn.setToggleGroup(variantGroup);

    updateThemeSelection();

    controls.getChildren().addAll(themeComboBox, lightBtn, darkBtn);
    container.getChildren().addAll(label, controls);
    return container;
  }

  private ToggleButton createVariantButton(Feather icon, String tooltip, boolean isDark) {
    ToggleButton btn = new ToggleButton();
    btn.setGraphic(new FontIcon(icon));
    btn.getStyleClass().addAll("theme-variant-button", Styles.BUTTON_ICON);
    btn.setTooltip(new Tooltip(tooltip));
    btn.setOnAction(e -> switchThemeVariant(isDark));
    return btn;
  }

  private void switchThemeVariant(boolean isDark) {
    String baseName = themeComboBox.getValue();
    if (baseName == null) return;

    String newTheme = baseName + (isDark ? " Dark" : " Light");
    controller.applyThemeByName(newTheme);
  }

  private VBox createAccentColorControl() {
    VBox container = new VBox();
    container.getStyleClass().add("accent-color-control");

    Label label = new Label("Accent Color");
    label.getStyleClass().add("control-label");

    HBox colorsRow = new HBox();
    colorsRow.getStyleClass().add("color-selection-box");
    colorSelectionBox = colorsRow;

    for (Color color : AppConstants.getAccentColors()) {
      colorsRow.getChildren().add(createColorButton(color));
    }

    colorsRow.getChildren().add(createCustomColorPickerButton());

    container.getChildren().addAll(label, colorsRow);
    return container;
  }

  private Button createColorButton(Color color) {
    Button btn = new Button();
    btn.getStyleClass().add("color-button");
    
    Circle circle = new Circle(10, color);
    circle.getStyleClass().add("color-circle");
    btn.setGraphic(circle);

    if (color.equals(model.getAccentColor())) {
      btn.getStyleClass().add("selected");
    }

    btn.setOnAction(e -> applyAccentColor(color));
    return btn;
  }

  private void applyAccentColor(Color color) {
    model.setAccentColor(color);
  }

  private void updateAccentColorSelection() {
    Color color = model.getAccentColor();
    if (color == null) return;
    
    boolean isPreset = isPresetColor(color);
    
    for (Node node : colorSelectionBox.getChildren()) {
        updateColorNodeSelection(node, color, isPreset);
    }
  }

  private boolean isPresetColor(Color color) {
    for (Color c : AppConstants.getAccentColors()) {
        if (c.equals(color)) return true;
    }
    return false;
  }

  private void updateColorNodeSelection(Node node, Color targetColor, boolean isPreset) {
    node.getStyleClass().remove("selected");
    
    if (node instanceof Button btn && btn.getGraphic() instanceof Circle circle) {
        if (circle.getFill().equals(targetColor)) {
            btn.getStyleClass().add("selected");
        }
    } else if (node instanceof ColorPicker picker && !isPreset) {
        picker.getStyleClass().add("selected");
        if (!targetColor.equals(picker.getValue())) {
            picker.setValue(targetColor);
        }
    }
  }

  private Node createCustomColorPickerButton() {
    ColorPicker picker = new ColorPicker(model.getAccentColor());
    picker.getStyleClass().addAll(Styles.BUTTON_ICON, "custom-color-button");
    
    picker.setOnAction(e -> {
      Color selectedColor = picker.getValue();
      if (selectedColor != null) {
        applyAccentColor(selectedColor);
      }
    });
    
    return picker;
  }

  private void handleThemeChange() {
    String baseName = themeComboBox.getValue();
    if (baseName == null) return;

    boolean isDark = darkBtn.isSelected();
    controller.applyTheme(baseName, isDark);
  }

  private void updateThemeSelection() {
    String currentTheme = controller.getCurrentThemeName();
    if (currentTheme == null) return;

    String baseName = controller.getBaseThemeName(currentTheme);
    boolean isDark = controller.isDarkTheme(currentTheme);

    if (themeComboBox.getItems().contains(baseName)) {
      themeComboBox.setValue(baseName);
    }

    if (isDark) darkBtn.setSelected(true);
    else lightBtn.setSelected(true);
    
    boolean shouldDisable = model.isUseSystemTheme();
    
    lightBtn.setDisable(shouldDisable);
    darkBtn.setDisable(shouldDisable);
  }

  private void updateControlsDisabledState() {
    boolean disable = model.isUseSystemTheme();
    themeComboBox.setDisable(disable);
    colorSelectionBox.setDisable(disable);
    
    if (disable) {
        colorSelectionBox.getChildren().forEach(node -> node.getStyleClass().remove("selected"));
    } else {
        updateAccentColorSelection();
    }
    
    lightBtn.setDisable(disable);
    darkBtn.setDisable(disable);
  }

  // ===== About Section =====

  private VBox createAboutSection() {
    VBox section = new VBox();
    section.getStyleClass().add("settings-section");

    Label sectionTitle = new Label("About");
    sectionTitle.getStyleClass().add(Styles.TITLE_3);

    HBox appInfoBox = new HBox();
    appInfoBox.getStyleClass().add("app-info-box");

    ImageView logo = createLogo();

    VBox infoBox = new VBox();
    infoBox.getStyleClass().add("app-info-text");
    
    Label appNameLabel = new Label(AppConstants.APP_NAME);
    appNameLabel.getStyleClass().add("app-name");

    Label versionLabel = new Label("Version " + AppConstants.APP_VERSION);
    versionLabel.getStyleClass().add("app-version");

    infoBox.getChildren().addAll(appNameLabel, versionLabel);
    appInfoBox.getChildren().addAll(logo, infoBox);

    VBox linksBox = new VBox();
    linksBox.getStyleClass().add("links-container");

    HBox linksRow = new HBox();
    linksRow.getStyleClass().add("links-row");

    linksRow.getChildren().addAll(
        createLinkButton("Website", AppConstants.WEBSITE_URL, Feather.GLOBE),
        createLinkButton("GitHub", AppConstants.GITHUB_URL, Feather.GITHUB),
        createLinkButton("Issues", AppConstants.ISSUES_URL, Feather.ALERT_CIRCLE),
        createLinkButton("Forum", AppConstants.FORUM_URL, Feather.MESSAGE_CIRCLE)
    );
    
    linksBox.getChildren().add(linksRow);

    section.getChildren().addAll(sectionTitle, appInfoBox, linksBox);
    return section;
  }

  private ImageView createLogo() {
    ImageView logo = new ImageView();
    logo.getStyleClass().add("app-logo");
    logo.setFitWidth(64);
    logo.setFitHeight(64);
    logo.setPreserveRatio(true);
    
    try {
      Image logoImage = new Image(getClass().getResourceAsStream("/images/logo.png"));
      logo.setImage(logoImage);
    } catch (Exception e) {
      LOGGER.warn("Failed to load logo", e);
    }
    
    return logo;
  }

  private Button createLinkButton(String text, String url, Feather icon) {
    Button button = new Button(text);
    button.getStyleClass().addAll("link-button", Styles.BUTTON_OUTLINED);
    button.setGraphic(new FontIcon(icon));
    button.setOnAction(e -> openURL(url));
    return button;
  }

  private void openURL(String url) {
    controller.openURL(url);
  }
}