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
	private static final double UI_SCALE_STEP = 0.1;
	private static final int GRAPH_TRIPLE_LIMIT_MIN_STEP = 100;

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
		view.updateUiScaleDisplay(themeManager.getUiScale());
		updateUiScaleStepperState(themeManager.getUiScale());
		view.updateGraphPreviewLimitDisplay(themeManager.getGraphAutoRenderTriplesLimit());
		updateGraphPreviewLimitStepperState(themeManager.getGraphAutoRenderTriplesLimit());
		updateThemeSelection();
		updateControlsDisabledState();
	}

	private void setupBindings() {
		syncThemeManagerIntoModel();
		updateControlsDisabledState();
		bindModelToThemeManager();
		bindThemeManagerToModel();
	}

	private void syncThemeManagerIntoModel() {
		if (themeManager.getTheme() != null) {
			model.setTheme(themeManager.getTheme());
		}
		if (themeManager.getAccentColor() != null) {
			model.setAccentColor(themeManager.getAccentColor());
		}
		model.setUseSystemTheme(themeManager.isSystemThemeEnabled());
		model.setUiScale(themeManager.getUiScale());
		model.setGraphAutoRenderTriplesLimit(themeManager.getGraphAutoRenderTriplesLimit());
	}

	private void bindModelToThemeManager() {
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
		model.graphAutoRenderTriplesLimitProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				themeManager.setGraphAutoRenderTriplesLimit(newVal.intValue());
			}
		});
	}

	private void bindThemeManagerToModel() {
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
		themeManager.graphAutoRenderTriplesLimitProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && newVal.intValue() != model.getGraphAutoRenderTriplesLimit()) {
				model.setGraphAutoRenderTriplesLimit(newVal.intValue());
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
				double scaleValue = newScale.doubleValue();
				view.updateUiScaleDisplay(scaleValue);
				updateUiScaleStepperState(scaleValue);
			}
		});
		model.graphAutoRenderTriplesLimitProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue != null) {
				int value = newValue.intValue();
				view.updateGraphPreviewLimitDisplay(value);
				updateGraphPreviewLimitStepperState(value);
			}
		});

		model.useSystemThemeProperty().addListener((obs, oldValue, newValue) -> updateControlsDisabledState());
	}

	private void setupViewListeners() {
		view.getThemeComboBox().setOnAction(e -> handleThemeChange());
		view.setOnUiScaleDecrease(() -> adjustUiScale(-UI_SCALE_STEP));
		view.setOnUiScaleIncrease(() -> adjustUiScale(+UI_SCALE_STEP));
		view.setOnGraphPreviewLimitDecrease(() -> adjustGraphPreviewLimit(false));
		view.setOnGraphPreviewLimitIncrease(() -> adjustGraphPreviewLimit(true));

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

	private void adjustUiScale(double delta) {
		model.setUiScale(model.getUiScale() + delta);
	}

	private void updateUiScaleStepperState(double scale) {
		double minScale = ThemeManager.getMinUiScale();
		double maxScale = ThemeManager.getMaxUiScale();
		view.setUiScaleDecreaseDisabled(scale <= minScale + SCALE_EPSILON);
		view.setUiScaleIncreaseDisabled(scale >= maxScale - SCALE_EPSILON);
	}

	private void adjustGraphPreviewLimit(boolean increase) {
		int currentValue = model.getGraphAutoRenderTriplesLimit();
		int step = computeGraphPreviewLimitStep(currentValue);
		int min = ThemeManager.getMinGraphAutoRenderTriplesLimit();
		int delta = increase ? step : -step;
		int nextValue = addWithSaturation(currentValue, delta);
		model.setGraphAutoRenderTriplesLimit(Math.max(nextValue, min));
	}

	private static int computeGraphPreviewLimitStep(int currentValue) {
		if (currentValue <= 0) {
			return GRAPH_TRIPLE_LIMIT_MIN_STEP;
		}
		int rawAdaptiveStep = Math.max(GRAPH_TRIPLE_LIMIT_MIN_STEP, currentValue / 10);
		return roundToNiceStep(rawAdaptiveStep);
	}

	private static int roundToNiceStep(int rawStep) {
		if (rawStep <= GRAPH_TRIPLE_LIMIT_MIN_STEP) {
			return GRAPH_TRIPLE_LIMIT_MIN_STEP;
		}
		int magnitude = 1;
		while (magnitude <= Integer.MAX_VALUE / 10 && magnitude * 10 <= rawStep) {
			magnitude *= 10;
		}
		double normalized = rawStep / (double) magnitude;
		int normalizedStep;
		if (normalized < 1.5d) {
			normalizedStep = 1;
		} else if (normalized < 3d) {
			normalizedStep = 2;
		} else if (normalized < 7d) {
			normalizedStep = 5;
		} else {
			normalizedStep = 10;
		}
		return Math.max(GRAPH_TRIPLE_LIMIT_MIN_STEP, normalizedStep * magnitude);
	}

	private static int addWithSaturation(int value, int delta) {
		if (delta > 0 && value > Integer.MAX_VALUE - delta) {
			return Integer.MAX_VALUE;
		}
		if (delta < 0 && value < Integer.MIN_VALUE - delta) {
			return Integer.MIN_VALUE;
		}
		return value + delta;
	}

	private void updateGraphPreviewLimitStepperState(int value) {
		int min = ThemeManager.getMinGraphAutoRenderTriplesLimit();
		view.setGraphPreviewLimitDecreaseDisabled(value <= min);
		view.setGraphPreviewLimitIncreaseDisabled(false);
	}
}
