package fr.inria.corese.gui.core.service;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLHandshakeException;

/**
 * Shared helpers for HTTPS->HTTP fallback on known demo URLs.
 */
final class DemoHttpFallbackSupport {

	private static final String HTTPS_SCHEME = "https";
	private static final String HTTP_SCHEME = "http";
	private static final String DEMO_HTTP_FALLBACK_HOST = normalizeNonBlank(
			System.getProperty("corese.demo.httpFallback.host"), "ns.inria.fr");
	private static final String DEMO_HTTP_FALLBACK_PATH_PREFIX = normalizePathPrefix(
			System.getProperty("corese.demo.httpFallback.pathPrefix"), "/humans/");
	private static final Pattern LOAD_URI_PATTERN = Pattern.compile("(?i)(\\bload\\s+(?:silent\\s+)?<)([^>]+)(>)");

	private DemoHttpFallbackSupport() {
		throw new AssertionError("Utility class");
	}

	static String demoHost() {
		return DEMO_HTTP_FALLBACK_HOST;
	}

	static String demoPathPrefix() {
		return DEMO_HTTP_FALLBACK_PATH_PREFIX;
	}

	static boolean isSslHandshakeFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SSLHandshakeException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	static URI resolveUriAfterSslFailure(URI uri, Throwable failure) {
		if (!isSslHandshakeFailure(failure)) {
			return null;
		}
		return toHttpUriIfCandidate(uri);
	}

	static String rewriteLoadUrisToHttp(String query) {
		if (query == null || query.isBlank()) {
			return query;
		}

		Matcher matcher = LOAD_URI_PATTERN.matcher(query);
		StringBuffer rewritten = new StringBuffer();
		boolean changed = false;

		while (matcher.find()) {
			URI uri = parseUriSafely(matcher.group(2));
			URI fallbackUri = toHttpUriIfCandidate(uri);
			String replacement = matcher.group(0);
			if (fallbackUri != null) {
				replacement = matcher.group(1) + fallbackUri + matcher.group(3);
				changed = true;
			}
			matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(rewritten);
		return changed ? rewritten.toString() : query;
	}

	private static URI parseUriSafely(String uriString) {
		if (uriString == null || uriString.isBlank()) {
			return null;
		}
		try {
			return URI.create(uriString.trim());
		} catch (IllegalArgumentException _) {
			return null;
		}
	}

	private static URI toHttpUriIfCandidate(URI uri) {
		if (!isDemoHttpFallbackCandidate(uri)) {
			return null;
		}
		try {
			return new URI(HTTP_SCHEME, uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(),
					uri.getFragment());
		} catch (Exception _) {
			return null;
		}
	}

	private static boolean isDemoHttpFallbackCandidate(URI uri) {
		if (uri == null || !HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
			return false;
		}
		String host = uri.getHost();
		String path = uri.getPath();
		if (host == null || path == null) {
			return false;
		}
		return DEMO_HTTP_FALLBACK_HOST.equalsIgnoreCase(host) && path.startsWith(DEMO_HTTP_FALLBACK_PATH_PREFIX);
	}

	private static String normalizeNonBlank(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}

	private static String normalizePathPrefix(String value, String fallback) {
		String normalized = normalizeNonBlank(value, fallback);
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		return normalized.endsWith("/") ? normalized : normalized + "/";
	}
}
