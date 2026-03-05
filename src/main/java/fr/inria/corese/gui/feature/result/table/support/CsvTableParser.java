package fr.inria.corese.gui.feature.result.table.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight RFC-4180 style CSV parser for table results.
 *
 * <p>
 * Supports quoted fields, escaped quotes ({@code ""}) and embedded newlines
 * within quoted fields.
 */
public final class CsvTableParser {

	private CsvTableParser() {
	}

	/**
	 * Parses CSV content into rows of fields.
	 *
	 * @param csvContent
	 *            CSV payload to parse.
	 * @return parsed rows, empty when input is null/empty.
	 */
	public static List<String[]> parse(String csvContent) {
		if (csvContent == null || csvContent.isEmpty()) {
			return List.of();
		}

		List<String[]> rows = new ArrayList<>();
		List<String> currentRow = new ArrayList<>();
		StringBuilder currentField = new StringBuilder();
		boolean inQuotes = false;
		boolean fieldStarted = false;

		for (int i = 0; i < csvContent.length(); i++) {
			char current = csvContent.charAt(i);

			// Ignore an optional UTF-8 BOM at start of content.
			if (i == 0 && current == '\ufeff') {
				continue;
			}

			if (current == '"') {
				if (inQuotes) {
					if (i + 1 < csvContent.length() && csvContent.charAt(i + 1) == '"') {
						currentField.append('"');
						fieldStarted = true;
						i++;
					} else {
						inQuotes = false;
					}
				} else if (currentField.length() == 0) {
					inQuotes = true;
					fieldStarted = true;
				} else {
					currentField.append(current);
					fieldStarted = true;
				}
				continue;
			}

			if (!inQuotes && current == ',') {
				addField(currentRow, currentField);
				fieldStarted = false;
				continue;
			}

			if (!inQuotes && (current == '\n' || current == '\r')) {
				addField(currentRow, currentField);
				addRow(rows, currentRow);
				fieldStarted = false;
				if (current == '\r' && i + 1 < csvContent.length() && csvContent.charAt(i + 1) == '\n') {
					i++;
				}
				continue;
			}

			currentField.append(current);
			fieldStarted = true;
		}

		boolean endsWithComma = !csvContent.isEmpty() && csvContent.charAt(csvContent.length() - 1) == ',';
		if (fieldStarted || !currentRow.isEmpty() || inQuotes || endsWithComma) {
			addField(currentRow, currentField);
			addRow(rows, currentRow);
		}

		return rows;
	}

	private static void addField(List<String> currentRow, StringBuilder currentField) {
		currentRow.add(currentField.toString());
		currentField.setLength(0);
	}

	private static void addRow(List<String[]> rows, List<String> currentRow) {
		rows.add(currentRow.toArray(String[]::new));
		currentRow.clear();
	}
}
