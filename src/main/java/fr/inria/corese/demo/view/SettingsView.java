package fr.inria.corese.demo.view;

import atlantafx.base.theme.*;
import fr.inria.corese.demo.view.base.AbstractView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings view for configuring application preferences.
 *
 * <p>This view allows users to customize:
 *
 * <ul>
 *   <li>Application theme (Nord Light, Nord Dark, Primer Light, Primer Dark, etc.)
 *   <li>Accent color
 * </ul>
 *
 * <p>Layout structure:
 *
 * <pre>
 * +--------------------------------+
 * | VBox (root)                   |
 * |  +---------------------------+ |
 * |  | Label (Settings)          | |
 * |  +---------------------------+ |
 * |  | VBox (Theme Section)      | |
 * |  |  - ComboBox (themes)      | |
 * |  +---------------------------+ |
 * |  | VBox (Accent Section)     | |
 * |  |  - ColorPicker            | |
 * |  +---------------------------+ |
 * +--------------------------------+
 * </pre>
 */
public final class SettingsView extends AbstractView {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);
  private static final String STYLESHEET_PATH = "/styles/settings-view.css";

  // ===== UI Components =====
  private final ComboBox<ThemeOption> themeComboBox;
  private final ColorPicker accentColorPicker;

  // ===== Constructor =====

  public SettingsView() {
    super(new VBox(), STYLESHEET_PATH);

    this.themeComboBox = createThemeComboBox();
    this.accentColorPicker = createAccentColorPicker();

    initializeLayout();
  }

  // ===== Initialization =====

  /** Configures the layout hierarchy and spacing for the settings view. */
  private void initializeLayout() {
    VBox root = (VBox) getRoot();
    root.getStyleClass().add("settings-view");
    root.setPadding(new Insets(30));
    root.setSpacing(30);
    root.setAlignment(Pos.TOP_LEFT);
    root.setFillWidth(true);

    // Title
    Label titleLabel = new Label("Settings");
    titleLabel.getStyleClass().add("settings-title");

    // Theme section
    VBox themeSection = createSection("Theme", "Choose the application theme", themeComboBox);

    // Accent color section
    VBox accentSection =
        createSection("Accent Color", "Customize the accent color", accentColorPicker);

    root.getChildren().addAll(titleLabel, themeSection, accentSection);
  }

  /**
   * Creates a settings section with a title, description, and control.
   *
   * @param title Section title
   * @param description Section description
   * @param control The control to display (ComboBox, ColorPicker, etc.)
   * @return VBox containing the section
   */
  private VBox createSection(String title, String description, Control control) {
    VBox section = new VBox(10);
    section.getStyleClass().add("settings-section");

    // Section header
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("section-title");

    Label descLabel = new Label(description);
    descLabel.getStyleClass().add("section-description");

    // Control container
    HBox controlBox = new HBox(10);
    controlBox.setAlignment(Pos.CENTER_LEFT);
    controlBox.getChildren().add(control);

    section.getChildren().addAll(titleLabel, descLabel, controlBox);
    return section;
  }

  // ===== Theme Management =====

  /** Creates and configures the theme selection ComboBox. */
  private ComboBox<ThemeOption> createThemeComboBox() {
    ComboBox<ThemeOption> comboBox = new ComboBox<>();
    comboBox.getStyleClass().add("theme-combo-box");
    comboBox.setPrefWidth(250);

    // Add available themes
    comboBox
        .getItems()
        .addAll(
            new ThemeOption("Nord Light", new NordLight()),
            new ThemeOption("Nord Dark", new NordDark()),
            new ThemeOption("Primer Light", new PrimerLight()),
            new ThemeOption("Primer Dark", new PrimerDark()),
            new ThemeOption("Cupertino Light", new CupertinoLight()),
            new ThemeOption("Cupertino Dark", new CupertinoDark()),
            new ThemeOption("Dracula", new Dracula()));

    // Set cell factory for custom rendering
    comboBox.setCellFactory(
        param ->
            new ListCell<>() {
              @Override
              protected void updateItem(ThemeOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                  setGraphic(null);
                } else {
                  setText(item.name());
                  FontIcon icon = new FontIcon(Feather.CIRCLE);
                  icon.getStyleClass().add("theme-icon");
                  setGraphic(icon);
                }
              }
            });

    comboBox.setButtonCell(
        new ListCell<>() {
          @Override
          protected void updateItem(ThemeOption item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
              setText(null);
              setGraphic(null);
            } else {
              setText(item.name());
              FontIcon icon = new FontIcon(Feather.CIRCLE);
              icon.getStyleClass().add("theme-icon");
              setGraphic(icon);
            }
          }
        });

    // Select current theme
    String currentThemeName = getCurrentThemeName();
    comboBox
        .getItems()
        .stream()
        .filter(option -> option.theme().getClass().getSimpleName().equals(currentThemeName))
        .findFirst()
        .ifPresent(comboBox::setValue);

    // Handle theme changes
    comboBox.setOnAction(
        e -> {
          ThemeOption selected = comboBox.getValue();
          if (selected != null) {
            applyTheme(selected.theme());
          }
        });

    return comboBox;
  }

  /** Gets the current theme class name. */
  private String getCurrentThemeName() {
    try {
      var currentTheme = javafx.application.Application.getUserAgentStylesheet();
      if (currentTheme.contains("nord-light")) return "NordLight";
      if (currentTheme.contains("nord-dark")) return "NordDark";
      if (currentTheme.contains("primer-light")) return "PrimerLight";
      if (currentTheme.contains("primer-dark")) return "PrimerDark";
      if (currentTheme.contains("cupertino-light")) return "CupertinoLight";
      if (currentTheme.contains("cupertino-dark")) return "CupertinoDark";
      if (currentTheme.contains("dracula")) return "Dracula";
    } catch (Exception e) {
      LOGGER.warn("Could not determine current theme", e);
    }
    return "NordLight"; // default
  }

  /** Applies the selected theme to the application. */
  private void applyTheme(Theme theme) {
    try {
      javafx.application.Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
      LOGGER.info("Applied theme: {}", theme.getClass().getSimpleName());
    } catch (Exception e) {
      LOGGER.error("Failed to apply theme", e);
      showError("Failed to apply theme: " + e.getMessage());
    }
  }

  // ===== Accent Color Management =====

  /** Creates and configures the accent color picker. */
  private ColorPicker createAccentColorPicker() {
    ColorPicker picker = new ColorPicker();
    picker.getStyleClass().add("accent-color-picker");
    picker.setPrefWidth(250);

    // Set current accent color (default to AtlantaFX blue)
    picker.setValue(javafx.scene.paint.Color.web("#0078D4"));

    // Handle color changes
    picker.setOnAction(
        e -> {
          javafx.scene.paint.Color color = picker.getValue();
          if (color != null) {
            applyAccentColor(color);
          }
        });

    return picker;
  }

  /** Applies the selected accent color to the application. */
  private void applyAccentColor(javafx.scene.paint.Color color) {
    try {
      // Convert color to CSS format
      String cssColor =
          String.format(
              "#%02X%02X%02X",
              (int) (color.getRed() * 255),
              (int) (color.getGreen() * 255),
              (int) (color.getBlue() * 255));

      // Apply to root style
      String style = "-color-accent-emphasis: " + cssColor + ";";
      getRoot().getScene().getRoot().setStyle(style);

      LOGGER.info("Applied accent color: {}", cssColor);
    } catch (Exception e) {
      LOGGER.error("Failed to apply accent color", e);
      showError("Failed to apply accent color: " + e.getMessage());
    }
  }

  // ===== Error Handling =====

  /** Shows an error alert to the user. */
  private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Settings Error");
    alert.setHeaderText("An error occurred");
    alert.setContentText(message);
    alert.showAndWait();
  }

  // ===== Helper Classes =====

  /** Record to hold theme information. */
  private record ThemeOption(String name, Theme theme) {
    @Override
    public String toString() {
      return name;
    }
  }
}
