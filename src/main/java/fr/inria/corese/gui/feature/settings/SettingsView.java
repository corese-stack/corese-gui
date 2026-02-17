package fr.inria.corese.gui.feature.settings;

import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.shortcut.KeyboardShortcutRegistry;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.BrowserUtils;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.utils.fx.LogoShadowEffects;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import fr.inria.corese.gui.utils.fx.SvgImageLoader;

import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	private static final String SHORTCUT_KEY_TOKENS_SEPARATOR = "\u0001";

	private record ShortcutDisplayEntry(String description, String availability,
			List<KeyboardShortcutRegistry.Shortcut> variants) {
	}

	// ===== UI Components =====
	private ToggleSwitch systemThemeSwitch;
	private ComboBox<String> themeComboBox;
	private ToggleButton lightModeButton;
	private ToggleButton darkModeButton;
	private ColorPicker accentColorPicker;
	private Button uiScaleDecreaseButton;
	private Button uiScaleIncreaseButton;
	private Label uiScaleValueLabel;

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

		content.getChildren().addAll(createAppearanceSection(), createKeyboardShortcutsSection(), createAboutSection());

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

		uiScaleDecreaseButton = createUiScaleStepperButton("-");
		uiScaleDecreaseButton.getStyleClass().add("settings-scale-stepper-button-left");

		uiScaleIncreaseButton = createUiScaleStepperButton("+");
		uiScaleIncreaseButton.getStyleClass().add("settings-scale-stepper-button-right");

		uiScaleValueLabel = new Label("100%");
		uiScaleValueLabel.getStyleClass().add("settings-scale-stepper-value");

		HBox stepper = new HBox(0, uiScaleDecreaseButton, uiScaleValueLabel, uiScaleIncreaseButton);
		stepper.getStyleClass().add("settings-scale-stepper");
		stepper.setAlignment(Pos.CENTER_RIGHT);

		uiScaleTile.setAction(stepper);
		return uiScaleTile;
	}

	private static Button createUiScaleStepperButton(String label) {
		Button button = new Button(label);
		button.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "settings-scale-stepper-button");
		button.setFocusTraversable(false);
		return button;
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

	public void setOnUiScaleDecrease(Runnable handler) {
		setButtonAction(uiScaleDecreaseButton, handler);
	}

	public void setOnUiScaleIncrease(Runnable handler) {
		setButtonAction(uiScaleIncreaseButton, handler);
	}

	public void updateUiScaleDisplay(double scale) {
		if (uiScaleValueLabel == null) {
			return;
		}
		int percent = (int) Math.round(scale * 100);
		uiScaleValueLabel.setText(percent + "%");
	}

	public void setUiScaleDecreaseDisabled(boolean disabled) {
		if (uiScaleDecreaseButton != null) {
			uiScaleDecreaseButton.setDisable(disabled);
		}
	}

	public void setUiScaleIncreaseDisabled(boolean disabled) {
		if (uiScaleIncreaseButton != null) {
			uiScaleIncreaseButton.setDisable(disabled);
		}
	}

	private VBox createKeyboardShortcutsSection() {
		VBox section = new VBox(12);
		section.getStyleClass().addAll("settings-section", "settings-shortcuts-section", "app-card", "app-card-subtle");
		RoundedClipSupport.applyRoundedClip(section, SECTION_RADIUS);

		Label sectionTitle = new Label("Keyboard Shortcuts");
		sectionTitle.getStyleClass().add(Styles.TITLE_3);

		Label sectionSubtitle = new Label("Global shortcuts and contextual shortcuts by page.");
		sectionSubtitle.getStyleClass().add("settings-shortcuts-subtitle");

		VBox groupsBox = new VBox(10);
		groupsBox.getStyleClass().add("settings-shortcuts-groups");

		Map<String, List<ShortcutDisplayEntry>> groupedDisplayEntries = buildShortcutDisplayEntriesByCategory();
		for (Map.Entry<String, List<ShortcutDisplayEntry>> entry : groupedDisplayEntries.entrySet()) {
			groupsBox.getChildren().add(createShortcutGroup(entry.getKey(), entry.getValue()));
		}

		section.getChildren().addAll(sectionTitle, sectionSubtitle, groupsBox);
		return section;
	}

	private VBox createShortcutGroup(String title, List<ShortcutDisplayEntry> entries) {
		VBox group = new VBox(6);
		group.getStyleClass().add("settings-shortcuts-group");
		Label groupTitle = new Label(title);
		groupTitle.getStyleClass().add("settings-shortcuts-group-title");
		group.getChildren().add(groupTitle);
		for (ShortcutDisplayEntry entry : entries) {
			group.getChildren().add(createShortcutTile(entry));
		}
		return group;
	}

	private Tile createShortcutTile(ShortcutDisplayEntry entry) {
		Tile tile = new Tile(entry.description(), entry.availability());
		tile.getStyleClass().add("settings-shortcut-tile");
		tile.setAction(createShortcutKeysBox(entry.variants()));
		return tile;
	}

	private HBox createShortcutKeysBox(List<KeyboardShortcutRegistry.Shortcut> shortcuts) {
		HBox keysBox = new HBox(6);
		keysBox.getStyleClass().add("settings-shortcut-keys-box");
		keysBox.setAlignment(Pos.CENTER_RIGHT);

		List<List<String>> tokenGroups = deduplicateShortcutTokenGroups(shortcuts);
		for (int shortcutIndex = 0; shortcutIndex < tokenGroups.size(); shortcutIndex++) {
			keysBox.getChildren().add(createShortcutTokenGroupBox(tokenGroups.get(shortcutIndex)));
			if (shortcutIndex < tokenGroups.size() - 1) {
				Label optionSeparator = new Label("or");
				optionSeparator.getStyleClass().add("settings-shortcut-option-separator");
				keysBox.getChildren().add(optionSeparator);
			}
		}
		return keysBox;
	}

	private static void setButtonAction(Button button, Runnable handler) {
		if (button == null) {
			return;
		}
		button.setOnAction(event -> {
			if (handler != null) {
				handler.run();
			}
		});
	}

	private Map<String, List<ShortcutDisplayEntry>> buildShortcutDisplayEntriesByCategory() {
		Map<String, Map<String, List<KeyboardShortcutRegistry.Shortcut>>> groupedShortcuts = new LinkedHashMap<>();
		for (KeyboardShortcutRegistry.Shortcut shortcut : KeyboardShortcutRegistry.shortcuts()) {
			Map<String, List<KeyboardShortcutRegistry.Shortcut>> categoryGroup = groupedShortcuts
					.computeIfAbsent(shortcut.category(), unusedCategory -> new LinkedHashMap<>());
			categoryGroup.computeIfAbsent(buildShortcutActionKey(shortcut), unusedAction -> new ArrayList<>())
					.add(shortcut);
		}

		Map<String, List<ShortcutDisplayEntry>> displayEntriesByCategory = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, List<KeyboardShortcutRegistry.Shortcut>>> entry : groupedShortcuts
				.entrySet()) {
			List<ShortcutDisplayEntry> displayEntries = entry.getValue().values().stream().map(variants -> {
				KeyboardShortcutRegistry.Shortcut first = variants.get(0);
				return new ShortcutDisplayEntry(first.description(), first.availability(), List.copyOf(variants));
			}).toList();
			displayEntriesByCategory.put(entry.getKey(), displayEntries);
		}
		return displayEntriesByCategory;
	}

	private static String buildShortcutActionKey(KeyboardShortcutRegistry.Shortcut shortcut) {
		return shortcut.scope() + "|" + shortcut.action() + "|" + shortcut.description() + "|"
				+ shortcut.availability();
	}

	private static List<List<String>> deduplicateShortcutTokenGroups(
			List<KeyboardShortcutRegistry.Shortcut> shortcuts) {
		List<KeyboardShortcutRegistry.Shortcut> safeShortcuts = shortcuts == null ? List.of() : shortcuts;
		Map<String, List<String>> uniqueTokenGroups = new LinkedHashMap<>();
		for (KeyboardShortcutRegistry.Shortcut shortcut : safeShortcuts) {
			List<String> tokens = KeyboardShortcutRegistry.displayKeyTokens(shortcut);
			if (tokens == null || tokens.isEmpty()) {
				continue;
			}
			String key = String.join(SHORTCUT_KEY_TOKENS_SEPARATOR, tokens);
			uniqueTokenGroups.putIfAbsent(key, tokens);
		}
		return List.copyOf(uniqueTokenGroups.values());
	}

	private static HBox createShortcutTokenGroupBox(List<String> tokens) {
		HBox comboBox = new HBox(4);
		comboBox.getStyleClass().add("settings-shortcut-combo");
		comboBox.setAlignment(Pos.CENTER_RIGHT);

		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			Label tokenLabel = new Label(tokens.get(tokenIndex));
			tokenLabel.getStyleClass().add("settings-shortcut-key-chip");
			comboBox.getChildren().add(tokenLabel);
			if (tokenIndex < tokens.size() - 1) {
				Label tokenSeparator = new Label("+");
				tokenSeparator.getStyleClass().add("settings-shortcut-key-separator");
				comboBox.getChildren().add(tokenSeparator);
			}
		}
		return comboBox;
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
