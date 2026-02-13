package fr.inria.corese.gui.feature.settings;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.core.theme.AppThemeRegistry;
import fr.inria.corese.gui.core.theme.ThemeManager;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;

/**
 * Controller for the Settings view.
 *
 * <p>
 * Acts as a bridge between the SettingsModel (View state) and ThemeManager (App
 * state). Strictly follows MVC by delegating all business logic to the
 * ThemeManager.
 */
public final class SettingsController {

	private final SettingsModel model;
	private final SettingsView view;
	private final ThemeManager themeManager;

	public SettingsController(SettingsModel model, SettingsView view) {
		this.model = model;
		this.view = view;
		this.themeManager = ThemeManager.getInstance();

		initializeView();
		setupBindings();
		setupViewBindings();
		setupViewListeners();
	}

	private void initializeView() {
		view.getThemeComboBox().getItems().addAll(themeManager.getBaseThemes());
		view.selectUiScale(themeManager.getUiScale());
		updateThemeSelection();
		updateControlsDisabledState();
		updateScaleControlsDisabledState();
	}

	private void setupBindings() {
		// 1. Initial Sync: Manager -> Model
		if (themeManager.getTheme() != null)
			model.setTheme(themeManager.getTheme());
		if (themeManager.getAccentColor() != null)
			model.setAccentColor(themeManager.getAccentColor());
		model.setUseSystemTheme(themeManager.isSystemThemeEnabled());
		model.setUiScale(themeManager.getUiScale());
		model.setAutoUiScale(themeManager.isAutoUiScaleEnabled());

		// Update controls state after initial sync
		updateControlsDisabledState();
		updateScaleControlsDisabledState();

		// 2. Model -> Manager (User inputs)
		model.themeProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && !model.isUseSystemTheme()) {
				themeManager.setTheme(newVal);
			}
		});

		model.accentColorProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				themeManager.setAccentColor(newVal);
			}
		});

		model.useSystemThemeProperty().addListener((obs, oldVal, newVal) -> themeManager.setSystemThemeEnabled(newVal));
		model.uiScaleProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && !model.isAutoUiScale()) {
				themeManager.setUiScale(newVal.doubleValue());
			}
		});
		model.autoUiScaleProperty()
				.addListener((obs, oldVal, newVal) -> themeManager.setAutoUiScaleEnabled(Boolean.TRUE.equals(newVal)));

		// 3. Manager -> Model (System changes or external updates)
		themeManager.themeProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && !newVal.equals(model.getTheme())) {
				model.setTheme(newVal);
			}
		});

		themeManager.accentColorProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && !newVal.equals(model.getAccentColor())) {
				model.setAccentColor(newVal);
			}
		});

		themeManager.systemThemeEnabledProperty().addListener((obs, oldVal, newVal) -> {
			if (!Boolean.valueOf(model.isUseSystemTheme()).equals(newVal)) {
				model.setUseSystemTheme(Boolean.TRUE.equals(newVal));
			}
		});
		themeManager.uiScaleProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && Math.abs(newVal.doubleValue() - model.getUiScale()) > 0.0001) {
				model.setUiScale(newVal.doubleValue());
			}
		});
		themeManager.autoUiScaleEnabledProperty().addListener((obs, oldVal, newVal) -> {
			boolean enabled = Boolean.TRUE.equals(newVal);
			if (model.isAutoUiScale() != enabled) {
				model.setAutoUiScale(enabled);
			}
		});
	}

	private void setupViewBindings() {
		view.getSystemThemeSwitch().selectedProperty().bindBidirectional(model.useSystemThemeProperty());
		view.getAccentColorPicker().valueProperty().bindBidirectional(model.accentColorProperty());
		view.getAutoUiScaleSwitch().selectedProperty().bindBidirectional(model.autoUiScaleProperty());

		model.themeProperty().addListener((obs, oldTheme, newTheme) -> {
			if (newTheme != null)
				updateThemeSelection();
		});
		model.uiScaleProperty().addListener((obs, oldScale, newScale) -> {
			if (newScale != null) {
				view.selectUiScale(newScale.doubleValue());
			}
		});

		model.useSystemThemeProperty().addListener((obs, oldValue, newValue) -> updateControlsDisabledState());
		model.autoUiScaleProperty().addListener((obs, oldValue, newValue) -> updateScaleControlsDisabledState());
	}

	private void setupViewListeners() {
		view.getThemeComboBox().setOnAction(e -> handleThemeChange());
		view.setOnUiScaleSelection(model::setUiScale);

		view.getLightModeButton().setOnAction(e -> {
			if (view.getLightModeButton().isSelected())
				switchThemeVariant(false);
		});

		view.getDarkModeButton().setOnAction(e -> {
			if (view.getDarkModeButton().isSelected())
				switchThemeVariant(true);
		});
	}

	// ===== Logic moved from View =====

	private void switchThemeVariant(boolean isDark) {
		String baseName = view.getThemeComboBox().getValue();
		if (baseName == null)
			return;
		applyTheme(baseName, isDark);
	}

	private void updateThemeSelection() {
		AppThemeRegistry currentTheme = themeManager.getCurrentAppTheme();
		if (currentTheme == null)
			return;

		String baseName = currentTheme.getBaseName();
		boolean isDark = currentTheme.isDark();

		ComboBox<String> combo = view.getThemeComboBox();
		if (combo.getItems().contains(baseName)) {
			combo.setValue(baseName);
		}

		ToggleButton lightBtn = view.getLightModeButton();
		ToggleButton darkBtn = view.getDarkModeButton();

		if (isDark) {
			if (!darkBtn.isSelected())
				darkBtn.setSelected(true);
		} else {
			if (!lightBtn.isSelected())
				lightBtn.setSelected(true);
		}

		boolean shouldDisable = model.isUseSystemTheme();
		lightBtn.setDisable(shouldDisable);
		darkBtn.setDisable(shouldDisable);
	}

	private void updateControlsDisabledState() {
		boolean disable = model.isUseSystemTheme();
		view.getThemeComboBox().setDisable(disable);
		view.getAccentColorPicker().setDisable(disable);
		view.getLightModeButton().setDisable(disable);
		view.getDarkModeButton().setDisable(disable);
	}

	private void updateScaleControlsDisabledState() {
		view.setUiScaleOptionsDisabled(model.isAutoUiScale());
	}

	private void handleThemeChange() {
		String baseName = view.getThemeComboBox().getValue();
		if (baseName == null)
			return;

		// Preserve current darkness state when changing base theme
		boolean isDark = themeManager.isCurrentThemeDark();

		applyTheme(baseName, isDark);
	}

	// ===== Data Access for View (Internal) =====

	public List<String> getBaseThemes() {
		return themeManager.getBaseThemes();
	}

	public String getCurrentThemeName() {
		return themeManager.getCurrentThemeName();
	}

	public String getBaseThemeName(String fullThemeName) {
		return themeManager.getBaseThemeName(fullThemeName);
	}

	public boolean isDarkTheme(String themeName) {
		return themeManager.isDarkTheme(themeName);
	}

	// ===== Actions =====

	public void applyThemeByName(String themeName) {
		Theme theme = themeManager.getThemeByName(themeName);
		if (theme != null) {
			model.setTheme(theme);
		}
	}

	public void applyTheme(String baseName, boolean isDark) {
		Theme theme = themeManager.getThemeVariant(baseName, isDark);
		if (theme != null) {
			model.setTheme(theme);
		}
	}
}
