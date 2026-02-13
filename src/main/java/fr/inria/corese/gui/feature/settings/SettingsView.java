package fr.inria.corese.gui.feature.settings;

import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.BrowserUtils;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.utils.fx.LogoShadowEffects;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import fr.inria.corese.gui.utils.fx.SvgImageLoader;

import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings view for configuring application preferences.
 *
 * <p>
 * This view follows the MVC pattern:
 *
 * <ul>
 * <li><b>Model:</b> {@link SettingsModel} - stores settings data
 * <li><b>View:</b> This class - displays UI
 * <li><b>Controller:</b> {@link SettingsController} - handles business logic
 * </ul>
 */
public final class SettingsView extends AbstractView {

	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);
	@SuppressWarnings("java:S1075")
	private static final String STYLESHEET_PATH = "/css/features/settings-view.css";
	@SuppressWarnings("java:S1075")
	private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";
	private static final double SECTION_RADIUS = 8.0;
	// ===== UI Components =====
	private ToggleSwitch systemThemeSwitch;
	private ComboBox<String> themeComboBox;
	private ToggleButton lightModeButton;
	private ToggleButton darkModeButton;
	private ColorPicker accentColorPicker;
	private final Map<Double, ToggleButton> uiScaleButtons = new LinkedHashMap<>();

	// ===== Constructor =====

	public SettingsView() {
		super(new ScrollPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
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
		section.getStyleClass().addAll("settings-section", "app-card", "app-card-subtle");
		RoundedClipSupport.applyRoundedClip(section, SECTION_RADIUS);

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

		lightModeButton = new ToggleButton(null, new FontIcon(ButtonIcon.THEME_LIGHT.getIkon()));
		lightModeButton.getStyleClass().addAll(Styles.LEFT_PILL);
		lightModeButton.setToggleGroup(modeGroup);

		darkModeButton = new ToggleButton(null, new FontIcon(ButtonIcon.THEME_DARK.getIkon()));
		darkModeButton.getStyleClass().addAll(Styles.RIGHT_PILL);
		darkModeButton.setToggleGroup(modeGroup);

		// Enforce selection
		modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == null)
				oldVal.setSelected(true);
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

		Tile uiScaleTile = createUiScaleTile();

		section.getChildren().addAll(sectionTitle, systemThemeTile, themeTile, accentColorTile, uiScaleTile);
		return section;
	}

	private Tile createUiScaleTile() {
		Tile uiScaleTile = new Tile("Interface Scale", "Adjust global application zoom");

		ToggleGroup scaleGroup = new ToggleGroup();
		HBox presets = new HBox(0);
		presets.getStyleClass().add("settings-scale-segmented");
		presets.setAlignment(Pos.CENTER_RIGHT);
		uiScaleButtons.clear();
		double[] presetValues = {0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0};
		int segmentCount = presetValues.length;
		int segmentIndex = 0;

		for (double preset : presetValues) {
			ToggleButton button = createUiScaleButton(preset);
			button.setToggleGroup(scaleGroup);
			applySegmentPositionStyle(button, segmentIndex++, segmentCount);
			uiScaleButtons.put(preset, button);
			presets.getChildren().add(button);
		}

		uiScaleTile.setAction(presets);
		return uiScaleTile;
	}

	private static ToggleButton createUiScaleButton(double preset) {
		int percent = (int) Math.round(preset * 100);
		ToggleButton button = new ToggleButton(percent + "%");
		button.setUserData(preset);
		button.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "settings-scale-segment-button");
		button.setFocusTraversable(false);
		return button;
	}

	private static void applySegmentPositionStyle(ToggleButton button, int index, int total) {
		button.getStyleClass().removeAll("settings-segment-first", "settings-segment-middle", "settings-segment-last");
		if (index == 0) {
			button.getStyleClass().add("settings-segment-first");
		} else if (index == total - 1) {
			button.getStyleClass().add("settings-segment-last");
		} else {
			button.getStyleClass().add("settings-segment-middle");
		}
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

	public void setOnUiScaleSelection(Consumer<Double> handler) {
		if (handler == null) {
			return;
		}
		uiScaleButtons.forEach((scale, button) -> button.setOnAction(event -> {
			if (button.isSelected()) {
				handler.accept(scale);
			}
		}));
	}

	public void selectUiScale(double scale) {
		if (uiScaleButtons.isEmpty()) {
			return;
		}
		ToggleButton nearestButton = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Map.Entry<Double, ToggleButton> entry : uiScaleButtons.entrySet()) {
			double distance = Math.abs(entry.getKey() - scale);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestButton = entry.getValue();
			}
		}
		if (nearestButton != null && !nearestButton.isSelected()) {
			nearestButton.setSelected(true);
		}
	}

	// ===== About Section =====

	private VBox createAboutSection() {
		VBox section = new VBox();
		section.getStyleClass().addAll("settings-section", "app-card", "app-card-subtle");
		RoundedClipSupport.applyRoundedClip(section, SECTION_RADIUS);

		Label sectionTitle = new Label("About");
		sectionTitle.getStyleClass().add(Styles.TITLE_3);

		HBox aboutRow = createAboutRow();
		section.getChildren().addAll(sectionTitle, aboutRow);
		return section;
	}

	private HBox createAboutRow() {
		HBox header = new HBox(14);
		header.getStyleClass().add("settings-about-header");
		header.setAlignment(Pos.CENTER_LEFT);

		StackPane logo = createLogo();

		VBox identity = new VBox(2);
		identity.getStyleClass().add("settings-about-identity");
		Label appName = new Label(AppConstants.APP_NAME);
		appName.getStyleClass().addAll(Styles.TITLE_4, "settings-about-name");

		Label version = new Label("Version " + AppConstants.APP_VERSION);
		version.getStyleClass().add("settings-about-version");
		version.setWrapText(false);

		identity.getChildren().addAll(appName, version);
		header.getChildren().addAll(logo, identity);

		HBox links = new HBox(10);
		links.getStyleClass().add("settings-about-links");
		links.setAlignment(Pos.CENTER_RIGHT);
		links.getChildren().addAll(createLinkButton("Website", AppConstants.WEBSITE_URL, ButtonIcon.LINK_WEBSITE),
				createLinkButton("Repository", AppConstants.REPOSITORY_URL, ButtonIcon.LINK_REPOSITORY),
				createLinkButton("Issues", AppConstants.ISSUES_URL, ButtonIcon.LINK_ISSUES),
				createLinkButton("Forum", AppConstants.FORUM_URL, ButtonIcon.LINK_FORUM));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox aboutRow = new HBox(16, header, spacer, links);
		aboutRow.getStyleClass().add("settings-about-row");
		return aboutRow;
	}

	private StackPane createLogo() {
		ImageView coreseLogo = new ImageView();
		coreseLogo.setFitWidth(64);
		coreseLogo.setFitHeight(64);
		coreseLogo.setPreserveRatio(true);
		coreseLogo.getStyleClass().add("settings-logo-image");
		coreseLogo.setEffect(LogoShadowEffects.createThemeAwareShadow());

		StackPane logoWrapper = new StackPane(coreseLogo);
		logoWrapper.getStyleClass().add("settings-logo-wrapper");

		try {
			// Load SVG with 2x scaling
			Image logoImage = SvgImageLoader.loadSvgImage("/images/corese-gui-logo.svg", 64, 64, 2.0);
			if (logoImage != null) {
				coreseLogo.setImage(logoImage);
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to load logo", e);
		}

		Tooltip.install(logoWrapper, new Tooltip("Open Corese website"));
		logoWrapper.setOnMouseClicked(e -> openURL(AppConstants.WEBSITE_URL));
		LogoShadowEffects.installDefaultAnimation(logoWrapper, (DropShadow) coreseLogo.getEffect());

		return logoWrapper;
	}

	private Button createLinkButton(String text, String url, ButtonIcon icon) {
		Button button = new Button(text, new FontIcon(icon.getIkon()));
		button.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
		button.setOnAction(e -> openURL(url));
		return button;
	}

	private void openURL(String url) {
		BrowserUtils.openUrl(url);
	}
}
