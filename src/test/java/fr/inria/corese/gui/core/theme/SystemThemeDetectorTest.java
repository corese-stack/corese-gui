package fr.inria.corese.gui.core.theme;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SystemThemeDetectorTest {

	@Test
	void parsePortalAccentColor_parsesReadOneTuplePayload() {
		Color color = SystemThemeDetector.parsePortalAccentColor(
				"(<<(0.2039215686, 0.3960784314, 0.6431372549)>>,)");

		assertNotNull(color);
		assertEquals(0.2039215686, color.getRed(), 0.0001);
		assertEquals(0.3960784314, color.getGreen(), 0.0001);
		assertEquals(0.6431372549, color.getBlue(), 0.0001);
	}

	@Test
	void parsePortalAccentColor_parsesReadFallbackPayload() {
		Color color = SystemThemeDetector.parsePortalAccentColor(
				"(<(0.1333333333, 0.6666666667, 0.2000000000)>,)");

		assertNotNull(color);
		assertEquals(0.1333333333, color.getRed(), 0.0001);
		assertEquals(0.6666666667, color.getGreen(), 0.0001);
		assertEquals(0.2000000000, color.getBlue(), 0.0001);
	}

	@Test
	void parsePortalAccentColor_returnsNullWhenTripletIsMissing() {
		assertNull(SystemThemeDetector.parsePortalAccentColor("(<'blue'>,)"));
	}

	@Test
	void parsePortalAccentColor_returnsNullForOutOfRangeValues() {
		assertNull(SystemThemeDetector.parsePortalAccentColor("(<(1.2, 0.4, 0.3)>,)"));
	}
}
