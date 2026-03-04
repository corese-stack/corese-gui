package fr.inria.corese.gui;

import fr.inria.corese.gui.core.bootstrap.RuntimeStorageBootstrap;

/**
 * Plain Java entrypoint used by fat JAR execution (java -jar).
 *
 * <p>
 * A JavaFX Application subclass as Main-Class can trigger the JDK FX launcher,
 * which expects JavaFX modules on the module path. Delegating through a plain
 * class keeps startup compatible with shaded classpath packaging.
 * </p>
 */
public final class Launcher {

	private Launcher() {
		// Utility class
	}

	public static void main(String[] args) {
		RuntimeStorageBootstrap.initialize();
		App.main(args);
	}
}
