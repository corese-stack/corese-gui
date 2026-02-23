package fr.inria.corese.gui.core.service.data;

import java.util.Objects;

/**
 * Immutable tracked source descriptor.
 *
 * @param type
 *            source type
 * @param location
 *            absolute file path or URI
 */
public record DataSource(SourceType type, String location) {
	public DataSource {
		Objects.requireNonNull(type, "type must not be null");
		if (location == null || location.isBlank()) {
			throw new IllegalArgumentException("location must not be blank");
		}
	}
}
