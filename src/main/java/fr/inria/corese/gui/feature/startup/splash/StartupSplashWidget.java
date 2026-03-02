package fr.inria.corese.gui.feature.startup.splash;

import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.utils.fx.SvgImageLoader;
import java.net.URL;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup splash widget rendering and theming.
 */
public final class StartupSplashWidget {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartupSplashWidget.class);
	private static final String APP_LOGO_SVG_RESOURCE = "/images/corese-gui-logo.svg";
	private static final String APP_LOGO_PNG_RESOURCE = "/images/corese-gui-logo.png";
	private static final String SUBTITLE_TEXT = "Query, visualize and validate RDF datasets.";
	private static final String STATUS_TEXT = "Loading workspace...";
	private static final double CARD_RADIUS = 34.0;
	private static final double LOGO_SURFACE_RADIUS = 30.0;

	private final StackPane root;
	private final StackPane card;
	private final Circle topGlow;
	private final Circle bottomGlow;
	private final StackPane logoSurface;
	private final ImageView logoView;
	private final Label titleLabel;
	private final Label subtitleLabel;
	private final Label versionLabel;
	private final Label statusLabel;
	private final Circle statusDot;

	public StartupSplashWidget() {
		root = new StackPane();
		root.setPadding(new Insets(18));

		card = new StackPane();
		card.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		topGlow = new Circle(205);
		topGlow.setMouseTransparent(true);
		StackPane.setAlignment(topGlow, Pos.TOP_LEFT);
		topGlow.setTranslateX(-78);
		topGlow.setTranslateY(-90);

		bottomGlow = new Circle(245);
		bottomGlow.setMouseTransparent(true);
		StackPane.setAlignment(bottomGlow, Pos.BOTTOM_RIGHT);
		bottomGlow.setTranslateX(114);
		bottomGlow.setTranslateY(124);

		logoView = new ImageView();
		logoView.setPreserveRatio(true);
		logoView.setSmooth(true);
		Image splashLogo = loadSplashLogo();
		if (splashLogo != null) {
			logoView.setImage(splashLogo);
		}

		logoSurface = new StackPane(logoView);
		logoSurface.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		logoSurface.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		titleLabel = new Label(AppConstants.APP_NAME);
		titleLabel.setFont(Font.font("System", FontWeight.BOLD, 46));

		subtitleLabel = new Label(SUBTITLE_TEXT);
		subtitleLabel.setWrapText(true);
		subtitleLabel.setMaxWidth(560);
		subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 20));

		versionLabel = new Label("Version " + AppConstants.APP_VERSION);
		versionLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));

		statusLabel = new Label(STATUS_TEXT);
		statusLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));

		statusDot = new Circle(4.0);

		VBox textColumn = new VBox(10, titleLabel, subtitleLabel, versionLabel);
		textColumn.setAlignment(Pos.CENTER_LEFT);
		textColumn.setFillWidth(true);

		HBox heroRow = new HBox(34, logoSurface, textColumn);
		heroRow.setAlignment(Pos.CENTER);
		heroRow.setFillHeight(false);

		StackPane heroContainer = new StackPane(heroRow);
		heroContainer.setAlignment(Pos.CENTER);
		heroContainer.setMaxWidth(Double.MAX_VALUE);

		HBox statusRow = new HBox(10, statusDot, statusLabel);
		statusRow.setAlignment(Pos.CENTER);

		BorderPane content = new BorderPane();
		content.setPadding(new Insets(44, 56, 42, 56));
		content.setCenter(heroContainer);
		content.setBottom(statusRow);
		BorderPane.setAlignment(statusRow, Pos.CENTER);
		BorderPane.setMargin(statusRow, new Insets(22, 0, 0, 0));

		card.getChildren().addAll(topGlow, bottomGlow, content);
		root.getChildren().add(card);
	}

	public StackPane getRoot() {
		return root;
	}

	public void setSize(double width, double height) {
		double safeWidth = Math.max(720, width);
		double safeHeight = Math.max(430, height);

		card.setPrefSize(safeWidth, safeHeight);

		double logoSurfaceSize = Math.clamp(safeWidth * 0.17, 126, 178);
		logoSurface.setPrefSize(Math.rint(logoSurfaceSize), Math.rint(logoSurfaceSize));

		double logoIconSize = logoSurfaceSize * 0.66;
		logoView.setFitWidth(Math.rint(logoIconSize));
		logoView.setFitHeight(Math.rint(logoIconSize));

		double titleFontSize = Math.clamp(safeWidth * 0.048, 36, 52);
		titleLabel.setFont(Font.font("System", FontWeight.BOLD, titleFontSize));
		subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, Math.clamp(safeWidth * 0.020, 16, 22)));
		subtitleLabel.setMaxWidth(Math.clamp(safeWidth * 0.54, 360, 620));
		versionLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, Math.clamp(safeWidth * 0.015, 13, 16)));

		topGlow.setRadius(Math.clamp(safeWidth * 0.22, 160, 240));
		topGlow.setTranslateX(-Math.clamp(safeWidth * 0.095, 70, 96));
		topGlow.setTranslateY(-Math.clamp(safeHeight * 0.18, 78, 104));

		bottomGlow.setRadius(Math.clamp(safeWidth * 0.24, 180, 255));
		bottomGlow.setTranslateX(Math.clamp(safeWidth * 0.13, 88, 126));
		bottomGlow.setTranslateY(Math.clamp(safeHeight * 0.20, 102, 142));
	}

	public void applyTheme(boolean darkMode, Color accentColor) {
		Color accent = accentColor == null ? ThemeManager.getDefaultAccentColor() : accentColor;

		Color windowBackground = darkMode ? Color.web("#0F1724") : Color.web("#EFF4FC");
		Color cardStart = darkMode ? Color.web("#1B273A") : Color.web("#F8FBFF");
		Color cardEnd = darkMode ? Color.web("#121E31") : Color.web("#ECF2FB");
		Color cardBorder = darkMode ? Color.color(1, 1, 1, 0.20) : Color.color(0.10, 0.18, 0.30, 0.15);
		Color cardShadow = darkMode ? Color.color(0, 0, 0, 0.44) : Color.color(0.12, 0.19, 0.30, 0.24);
		Color logoSurfaceFill = darkMode ? Color.color(1, 1, 1, 0.08) : Color.color(1, 1, 1, 0.72);
		Color logoSurfaceBorder = darkMode ? Color.color(1, 1, 1, 0.14) : Color.color(0.10, 0.18, 0.30, 0.12);

		Color titleColor = darkMode ? Color.web("#F4F7FF") : Color.web("#172136");
		Color subtitleColor = darkMode ? Color.web("#B6C4DB") : Color.web("#3F5576");
		Color versionColor = darkMode ? Color.web("#95A4BC") : Color.web("#5A6C8B");
		Color statusColor = darkMode ? Color.web("#C8D3E6") : Color.web("#425A7A");

		root.setStyle(String.format(Locale.ROOT, "-fx-background-color: %s;", toHex(windowBackground)));
		card.setStyle(String.format(Locale.ROOT,
				"-fx-background-color: linear-gradient(from 0%% 0%% to 100%% 100%%, %s 0%%, %s 100%%); "
						+ "-fx-background-radius: %.1f; "
						+ "-fx-border-color: %s; "
						+ "-fx-border-width: 1; "
						+ "-fx-border-radius: %.1f;",
				toHex(cardStart), toHex(cardEnd), CARD_RADIUS, toRgba(cardBorder), CARD_RADIUS));
		card.setEffect(new DropShadow(46, cardShadow));

		topGlow.setFill(accent.deriveColor(0, 1.0, 1.0, darkMode ? 0.20 : 0.14));
		bottomGlow.setFill(accent.deriveColor(36, 0.75, darkMode ? 1.05 : 0.98, darkMode ? 0.18 : 0.11));

		logoSurface.setStyle(String.format(Locale.ROOT,
				"-fx-background-color: %s; "
						+ "-fx-background-radius: %.1f; "
						+ "-fx-border-color: %s; "
						+ "-fx-border-width: 1; "
						+ "-fx-border-radius: %.1f;",
				toRgba(logoSurfaceFill), LOGO_SURFACE_RADIUS, toRgba(logoSurfaceBorder), LOGO_SURFACE_RADIUS));

		titleLabel.setTextFill(titleColor);
		subtitleLabel.setTextFill(subtitleColor);
		versionLabel.setTextFill(versionColor);
		statusLabel.setTextFill(statusColor);
		statusDot.setFill(accent.deriveColor(0, 1.0, 1.0, darkMode ? 0.92 : 0.76));
	}

	private static Image loadSplashLogo() {
		try {
			Image logo = SvgImageLoader.loadSvgImage(APP_LOGO_SVG_RESOURCE, 160, 160);
			if (logo != null && !logo.isError()) {
				return logo;
			}
		} catch (Exception e) {
			LOGGER.debug("Unable to load splash SVG logo", e);
		}

		try {
			URL fallbackUrl = StartupSplashWidget.class.getResource(APP_LOGO_PNG_RESOURCE);
			if (fallbackUrl == null) {
				return null;
			}
			Image fallback = new Image(fallbackUrl.toExternalForm(), false);
			if (!fallback.isError()) {
				return fallback;
			}
		} catch (Exception e) {
			LOGGER.debug("Unable to load splash PNG logo", e);
		}
		return null;
	}

	private static String toHex(Color color) {
		int red = (int) Math.round(color.getRed() * 255);
		int green = (int) Math.round(color.getGreen() * 255);
		int blue = (int) Math.round(color.getBlue() * 255);
		return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
	}

	private static String toRgba(Color color) {
		int red = (int) Math.round(color.getRed() * 255);
		int green = (int) Math.round(color.getGreen() * 255);
		int blue = (int) Math.round(color.getBlue() * 255);
		double opacity = Math.clamp(color.getOpacity(), 0.0, 1.0);
		return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.3f)", red, green, blue, opacity);
	}
}
