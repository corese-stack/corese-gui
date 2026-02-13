package fr.inria.corese.gui.feature.settings;

import fr.inria.corese.gui.core.theme.AppThemeRegistry;
import fr.inria.corese.gui.core.theme.ThemeManager;
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
	private static final double SCALE_EPSILON = 0.0001;

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
	}

	private void setupBindings() {
		// 1. Initial Sync: Manager -> Model
		if (themeManager.getTheme() != null)
			model.setTheme(themeManager.getTheme());
		if (themeManager.getAccentColor() != null)
			model.setAccentColor(themeManager.getAccentColor());
		model.setUseSystemTheme(themeManager.isSystemThemeEnabled());
		model.setUiScale(themeManager.getUiScale());

		// Update controls state after initial sync
		updateControlsDisabledState();

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
			if (newVal != null) {
				themeManager.setUiScale(newVal.doubleValue());
			}
		});

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
			if (newVal != null && Math.abs(newVal.doubleValue() - model.getUiScale()) > SCALE_EPSILON) {
				model.setUiScale(newVal.doubleValue());
			}
		});
	}

	private void setupViewBindings() {
		view.getSystemThemeSwitch().selectedProperty().bindBidirectional(model.useSystemThemeProperty());
		view.getAccentColorPicker().valueProperty().bindBidirectional(model.accentColorProperty());

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
		ThemeManager manager = themeManager;
		var theme = manager.getThemeVariant(baseName, isDark);
		if (theme != null) {
			model.setTheme(theme);
		}
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

	private void handleThemeChange() {
		String baseName = view.getThemeComboBox().getValue();
		if (baseName == null)
			return;

		// Preserve current darkness state when changing base theme
		boolean isDark = themeManager.isCurrentThemeDark();
		var theme = themeManager.getThemeVariant(baseName, isDark);
		if (theme != null) {
			model.setTheme(theme);
		}
	}
}
