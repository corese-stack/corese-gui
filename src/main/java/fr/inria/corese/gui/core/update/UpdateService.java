package fr.inria.corese.gui.core.update;

import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.utils.AppExecutors;
import fr.inria.corese.gui.utils.BrowserUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for checking GitHub releases and exposing update actions.
 */
@SuppressWarnings("java:S6548")
public final class UpdateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateService.class);

	private static final String ACCEPT_HEADER = "application/vnd.github+json";
	private static final String USER_AGENT = "corese-gui-update-checker";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	private static final String UPDATE_API_URL_SYSTEM_PROPERTY = "corese.update.apiLatestUrl";
	private static final String CURRENT_VERSION_SYSTEM_PROPERTY = "corese.update.currentVersion";
	private static final String FLATPAK_OVERRIDE_SYSTEM_PROPERTY = "corese.update.forceFlatpak";
	private static final String RUNTIME_FLAVOR_SYSTEM_PROPERTY = "corese.update.runtimeFlavor";
	private static final String JPACKAGE_APP_PATH_SYSTEM_PROPERTY = "jpackage.app-path";
	private static final String MIN_RELEASE_AGE_MINUTES_SYSTEM_PROPERTY = "corese.update.minReleaseAgeMinutes";
	private static final long DEFAULT_MIN_RELEASE_AGE_MINUTES = 20L;
	private static final String PREF_STARTUP_UPDATE_NOTIFICATION = "update.startupNotificationEnabled";
	private static final String PORTABLE_RUNTIME_MARKER_FILE_NAME = ".corese-portable";

	private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\\d+");
	private static final UpdateService INSTANCE = new UpdateService();

	private final HttpClient httpClient;
	private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
	private final AtomicReference<CheckResult> lastResult = new AtomicReference<>(CheckResult.notChecked());
	private final Preferences preferences = Preferences.userNodeForPackage(UpdateService.class);
	private final AtomicBoolean startupUpdateNotificationEnabled = new AtomicBoolean(
			loadStartupUpdateNotificationPreference());

	public enum Status {
		NOT_CHECKED, CHECKING, UPDATE_AVAILABLE, UP_TO_DATE, FLATPAK_MANAGED, UNAVAILABLE
	}

	public record UpdateAsset(String name, String downloadUrl) {

		public UpdateAsset {
			name = normalizeText(name);
			downloadUrl = normalizeText(downloadUrl);
		}
	}

	public record UpdateInfo(String currentVersion, String latestVersion, String releasePageUrl, String releaseName,
			UpdateAsset preferredAsset, List<UpdateAsset> assets) {

		public UpdateInfo {
			currentVersion = normalizeText(currentVersion);
			latestVersion = normalizeText(latestVersion);
			releasePageUrl = normalizeText(releasePageUrl);
			releaseName = normalizeText(releaseName);
			assets = assets == null ? List.of() : List.copyOf(assets);
		}
	}

	public record CheckResult(Status status, String message, UpdateInfo info) {

		public CheckResult {
			status = status == null ? Status.UNAVAILABLE : status;
			message = normalizeText(message);
		}

		public static CheckResult notChecked() {
			return new CheckResult(Status.NOT_CHECKED, "Not checked yet.", null);
		}

		public static CheckResult checking() {
			return new CheckResult(Status.CHECKING, "Checking for updates...", null);
		}

		public static CheckResult updateAvailable(UpdateInfo info) {
			if (info == null) {
				return new CheckResult(Status.UPDATE_AVAILABLE, "Update available.", null);
			}
			return new CheckResult(Status.UPDATE_AVAILABLE,
					"Update available: " + info.latestVersion() + " (installed: " + info.currentVersion() + ").", info);
		}

		public static CheckResult upToDate(UpdateInfo info) {
			if (info == null) {
				return new CheckResult(Status.UP_TO_DATE, "You are up to date.", null);
			}
			return new CheckResult(Status.UP_TO_DATE, "You are up to date (" + info.currentVersion() + ").", info);
		}

		public static CheckResult flatpakManaged() {
			return new CheckResult(Status.FLATPAK_MANAGED, "Updates are managed by Flatpak.", null);
		}

		public static CheckResult unavailable(String message) {
			String safeMessage = normalizeText(message);
			if (safeMessage.isBlank()) {
				safeMessage = "Unable to check for updates.";
			}
			return new CheckResult(Status.UNAVAILABLE, safeMessage, null);
		}
	}

	static record ParsedRelease(String tagName, String name, String htmlUrl, Instant publishedAt, boolean draft,
			boolean prerelease, List<UpdateAsset> assets) {

		ParsedRelease {
			tagName = normalizeText(tagName);
			name = normalizeText(name);
			htmlUrl = normalizeText(htmlUrl);
			assets = assets == null ? List.of() : List.copyOf(assets);
		}
	}

	private record RuntimeTarget(String osToken, String archToken, boolean preferPortablePackage) {
	}

	private record VersionParts(List<Integer> numbers, String qualifier) {
	}

	private record AssetCandidate(UpdateAsset asset, int targetRank, int formatRank, int standalonePenalty,
			int nameLength) {
	}

	private record JsonSlice(String raw, int nextIndex) {
	}

	private record JsonStringToken(String value, int nextIndex) {
	}

	private record ObjectFieldParseResult(String key, String rawValue, int nextIndex) {
	}

	private record EscapedJsonChar(String value, int nextIndex) {
	}

	private UpdateService() {
		this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL)
				.build());
	}

	UpdateService(HttpClient httpClient) {
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
	}

	public static UpdateService getInstance() {
		return INSTANCE;
	}

	public CheckResult getLastResult() {
		return lastResult.get();
	}

	public boolean isStartupUpdateNotificationEnabled() {
		return startupUpdateNotificationEnabled.get();
	}

	public void setStartupUpdateNotificationEnabled(boolean enabled) {
		boolean previous = startupUpdateNotificationEnabled.getAndSet(enabled);
		if (previous == enabled) {
			return;
		}
		saveStartupUpdateNotificationPreference(enabled);
	}

	public void checkForUpdatesAsync(boolean userInitiated, Consumer<CheckResult> callback) {
		if (userInitiated) {
			LOGGER.debug("Manual update check requested.");
		}
		if (isFlatpakManagedRuntime()) {
			CheckResult result = CheckResult.flatpakManaged();
			lastResult.set(result);
			dispatch(callback, result);
			return;
		}

		if (!checkInProgress.compareAndSet(false, true)) {
			dispatch(callback, lastResult.get());
			return;
		}

		CheckResult checking = CheckResult.checking();
		lastResult.set(checking);
		dispatch(callback, checking);

		AppExecutors.execute(() -> {
			CheckResult result;
			try {
				result = checkForUpdatesInternal();
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				LOGGER.warn("Update check interrupted", interruptedException);
				result = CheckResult.unavailable("Unable to check for updates.");
			} catch (IOException ioException) {
				LOGGER.warn("I/O failure during update check", ioException);
				result = CheckResult.unavailable("Unable to check for updates.");
			} catch (RuntimeException runtimeException) {
				LOGGER.warn("Unexpected update check failure", runtimeException);
				result = CheckResult.unavailable("Unable to check for updates.");
			}

			lastResult.set(result);
			checkInProgress.set(false);
			dispatch(callback, result);
		});
	}

	public boolean isFlatpakManagedRuntime() {
		String override = normalizeText(System.getProperty(FLATPAK_OVERRIDE_SYSTEM_PROPERTY));
		if ("true".equalsIgnoreCase(override)) {
			return true;
		}
		if ("false".equalsIgnoreCase(override)) {
			return false;
		}

		String flatpakId = normalizeText(System.getenv("FLATPAK_ID"));
		if (!flatpakId.isBlank()) {
			return true;
		}

		try {
			return Files.exists(Path.of("/.flatpak-info"));
		} catch (Exception e) {
			LOGGER.debug("Flatpak runtime probe failed", e);
			return false;
		}
	}

	public void openLatestReleasePage() {
		CheckResult result = lastResult.get();
		UpdateInfo info = result == null ? null : result.info();
		if (result != null && result.status() == Status.UPDATE_AVAILABLE && info != null) {
			openReleasePage(info);
			return;
		}
		if (isSnapshotLikeVersion(resolveCurrentVersion())) {
			BrowserUtils.openUrl(AppConstants.RELEASES_URL);
			return;
		}
		openReleasePage(info);
	}

	public void openLatestDownload() {
		openDownload(lastResult.get() == null ? null : lastResult.get().info());
	}

	public void openReleasePage(UpdateInfo info) {
		if (info != null && !info.releasePageUrl().isBlank()) {
			BrowserUtils.openUrl(info.releasePageUrl());
			return;
		}
		BrowserUtils.openUrl(AppConstants.RELEASES_URL);
	}

	public void openDownload(UpdateInfo info) {
		if (info == null) {
			openLatestReleasePage();
			return;
		}
		UpdateAsset preferredAsset = info.preferredAsset();
		if (preferredAsset != null && !preferredAsset.downloadUrl().isBlank()) {
			BrowserUtils.openUrl(preferredAsset.downloadUrl());
			return;
		}
		openReleasePage(info);
	}

	private CheckResult checkForUpdatesInternal() throws IOException, InterruptedException {
		String currentVersion = resolveCurrentVersion();
		URI endpoint = resolveLatestReleaseEndpoint();

		HttpRequest request = HttpRequest.newBuilder(endpoint).header("Accept", ACCEPT_HEADER)
				.header("User-Agent", USER_AGENT).timeout(REQUEST_TIMEOUT).GET().build();

		HttpResponse<String> response = httpClient.send(request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200) {
			LOGGER.warn("Update check failed with HTTP status {}", response.statusCode());
			return CheckResult.unavailable("Unable to check updates (HTTP " + response.statusCode() + ").");
		}

		ParsedRelease parsedRelease = parseReleasePayload(response.body());
		if (parsedRelease == null) {
			return CheckResult.unavailable("Unable to parse update metadata.");
		}
		if (parsedRelease.draft() || parsedRelease.prerelease()) {
			return CheckResult.unavailable("Latest release metadata is not publish-ready.");
		}

		Duration minReleaseAge = resolveMinReleaseAge();
		if (minReleaseAge.toMinutes() > 0 && isReleaseTooRecent(parsedRelease, minReleaseAge)) {
			logReleaseAgeGate(parsedRelease, minReleaseAge);
			return CheckResult.upToDate(null);
		}

		String latestVersion = normalizeVersionForDisplay(parsedRelease.tagName());
		String releaseName = parsedRelease.name().isBlank() ? latestVersion : parsedRelease.name();
		RuntimeTarget target = resolveRuntimeTarget();
		UpdateAsset preferredAsset = selectPreferredAsset(parsedRelease.assets(), target.osToken(), target.archToken(),
				target.preferPortablePackage()).orElse(null);

		UpdateInfo info = new UpdateInfo(currentVersion, latestVersion, parsedRelease.htmlUrl(), releaseName,
				preferredAsset, parsedRelease.assets());

		if (isNewerVersion(parsedRelease.tagName(), currentVersion)) {
			return CheckResult.updateAvailable(info);
		}
		return CheckResult.upToDate(info);
	}

	private static String resolveCurrentVersion() {
		String overrideVersion = normalizeText(System.getProperty(CURRENT_VERSION_SYSTEM_PROPERTY));
		if (!overrideVersion.isBlank()) {
			return overrideVersion;
		}
		return AppConstants.APP_VERSION;
	}

	private static URI resolveLatestReleaseEndpoint() {
		String overrideEndpoint = normalizeText(System.getProperty(UPDATE_API_URL_SYSTEM_PROPERTY));
		String endpoint = overrideEndpoint.isBlank() ? AppConstants.RELEASES_API_LATEST_URL : overrideEndpoint;
		try {
			return URI.create(endpoint);
		} catch (IllegalArgumentException _) {
			LOGGER.warn("Invalid update endpoint override '{}', using default endpoint.", endpoint);
			return URI.create(AppConstants.RELEASES_API_LATEST_URL);
		}
	}

	private static Duration resolveMinReleaseAge() {
		String rawMinutes = normalizeText(System.getProperty(MIN_RELEASE_AGE_MINUTES_SYSTEM_PROPERTY));
		if (rawMinutes.isBlank()) {
			return Duration.ofMinutes(DEFAULT_MIN_RELEASE_AGE_MINUTES);
		}
		try {
			long value = Long.parseLong(rawMinutes);
			if (value <= 0) {
				return Duration.ZERO;
			}
			return Duration.ofMinutes(value);
		} catch (NumberFormatException _) {
			LOGGER.warn("Invalid '{}' value '{}', falling back to {} minutes.", MIN_RELEASE_AGE_MINUTES_SYSTEM_PROPERTY,
					rawMinutes, DEFAULT_MIN_RELEASE_AGE_MINUTES);
			return Duration.ofMinutes(DEFAULT_MIN_RELEASE_AGE_MINUTES);
		}
	}

	private boolean loadStartupUpdateNotificationPreference() {
		try {
			return preferences.getBoolean(PREF_STARTUP_UPDATE_NOTIFICATION, true);
		} catch (Exception e) {
			LOGGER.warn("Failed to load startup update notification preference, defaulting to enabled.", e);
			return true;
		}
	}

	private void saveStartupUpdateNotificationPreference(boolean enabled) {
		try {
			preferences.putBoolean(PREF_STARTUP_UPDATE_NOTIFICATION, enabled);
		} catch (Exception e) {
			LOGGER.warn("Failed to persist startup update notification preference.", e);
		}
	}

	private static boolean isReleaseTooRecent(ParsedRelease release, Duration minReleaseAge) {
		if (release == null || minReleaseAge == null || minReleaseAge.isZero() || minReleaseAge.isNegative()) {
			return false;
		}
		Instant publishedAt = release.publishedAt();
		if (publishedAt == null) {
			return true;
		}
		Duration age = Duration.between(publishedAt, Instant.now());
		if (age.isNegative()) {
			age = Duration.ZERO;
		}
		return age.compareTo(minReleaseAge) < 0;
	}

	private static void logReleaseAgeGate(ParsedRelease release, Duration minReleaseAge) {
		if (release == null || minReleaseAge == null || minReleaseAge.isZero() || minReleaseAge.isNegative()) {
			return;
		}
		Instant publishedAt = release.publishedAt();
		if (publishedAt == null) {
			LOGGER.debug("Latest release ignored by hidden age gate: published_at missing.");
			return;
		}
		Duration age = Duration.between(publishedAt, Instant.now());
		if (age.isNegative()) {
			age = Duration.ZERO;
		}
		Duration remaining = minReleaseAge.minus(age).isNegative() ? Duration.ZERO : minReleaseAge.minus(age);
		LOGGER.debug("Latest release ignored by hidden age gate. age={}s remaining={}s", age.getSeconds(),
				remaining.getSeconds());
	}

	private static RuntimeTarget resolveRuntimeTarget() {
		String os = normalizeOsToken(System.getProperty("os.name"));
		String arch = normalizeArchToken(System.getProperty("os.arch"));
		boolean preferPortablePackage = resolvePortableRuntimePreference(os);
		return new RuntimeTarget(os, arch, preferPortablePackage);
	}

	private static boolean resolvePortableRuntimePreference(String osToken) {
		String normalizedFlavor = normalizeText(System.getProperty(RUNTIME_FLAVOR_SYSTEM_PROPERTY))
				.toLowerCase(Locale.ROOT);
		if ("portable".equals(normalizedFlavor)) {
			return true;
		}
		if ("installer".equals(normalizedFlavor)) {
			return false;
		}
		if (!"windows".equals(osToken)) {
			return false;
		}
		return hasPortableRuntimeMarker();
	}

	private static boolean hasPortableRuntimeMarker() {
		String launcherPathValue = normalizeText(System.getProperty(JPACKAGE_APP_PATH_SYSTEM_PROPERTY));
		if (launcherPathValue.isBlank()) {
			return false;
		}
		try {
			Path launcherPath = Path.of(launcherPathValue);
			Path launcherDirectory = launcherPath.getParent();
			if (launcherDirectory == null) {
				return false;
			}
			Path markerPath = launcherDirectory.resolve(PORTABLE_RUNTIME_MARKER_FILE_NAME);
			return Files.exists(markerPath);
		} catch (Exception e) {
			LOGGER.debug("Portable runtime marker probe failed", e);
			return false;
		}
	}

	static boolean isNewerVersion(String candidateVersion, String currentVersion) {
		return compareVersions(candidateVersion, currentVersion) > 0;
	}

	static int compareVersions(String leftVersion, String rightVersion) {
		VersionParts left = parseVersion(leftVersion);
		VersionParts right = parseVersion(rightVersion);

		int maxSize = Math.max(left.numbers().size(), right.numbers().size());
		for (int i = 0; i < maxSize; i++) {
			int leftPart = i < left.numbers().size() ? left.numbers().get(i) : 0;
			int rightPart = i < right.numbers().size() ? right.numbers().get(i) : 0;
			if (leftPart != rightPart) {
				return Integer.compare(leftPart, rightPart);
			}
		}

		String leftQualifier = left.qualifier();
		String rightQualifier = right.qualifier();
		if (leftQualifier.equalsIgnoreCase(rightQualifier)) {
			return 0;
		}

		int leftRank = qualifierRank(leftQualifier);
		int rightRank = qualifierRank(rightQualifier);
		if (leftRank != rightRank) {
			return Integer.compare(leftRank, rightRank);
		}

		return leftQualifier.compareToIgnoreCase(rightQualifier);
	}

	private static VersionParts parseVersion(String version) {
		String normalized = normalizeVersionToken(version);
		if (normalized.isBlank()) {
			return new VersionParts(List.of(0), "snapshot");
		}

		String[] split = normalized.split("-", 2);
		String numericPart = split.length > 0 ? split[0] : normalized;
		String qualifier = split.length > 1 ? split[1] : "";

		List<Integer> numericSegments = extractNumericSegments(numericPart);
		if (numericSegments.isEmpty()) {
			numericSegments = extractNumericSegments(normalized);
		}
		if (numericSegments.isEmpty()) {
			numericSegments = List.of(0);
		}
		return new VersionParts(List.copyOf(numericSegments), qualifier.toLowerCase(Locale.ROOT));
	}

	private static List<Integer> extractNumericSegments(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<Integer> segments = new ArrayList<>();
		Matcher matcher = VERSION_NUMBER_PATTERN.matcher(value);
		while (matcher.find()) {
			segments.add(parseIntegerWithSaturation(matcher.group()));
		}
		return segments;
	}

	private static int parseIntegerWithSaturation(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException _) {
			return Integer.MAX_VALUE;
		}
	}

	private static int qualifierRank(String qualifier) {
		if (qualifier == null || qualifier.isBlank()) {
			return 50;
		}
		String normalized = qualifier.toLowerCase(Locale.ROOT);
		if (normalized.contains("snapshot") || normalized.contains("dev") || normalized.contains("nightly")) {
			return 10;
		}
		if (normalized.contains("alpha")) {
			return 20;
		}
		if (normalized.contains("beta")) {
			return 30;
		}
		if (normalized.contains("rc")) {
			return 40;
		}
		return 25;
	}

	static Optional<UpdateAsset> selectPreferredAsset(List<UpdateAsset> assets, String osToken, String archToken) {
		return selectPreferredAsset(assets, osToken, archToken, false);
	}

	static Optional<UpdateAsset> selectPreferredAsset(List<UpdateAsset> assets, String osToken, String archToken,
			boolean preferPortablePackage) {
		List<UpdateAsset> safeAssets = assets == null ? List.of() : assets;
		if (safeAssets.isEmpty()) {
			return Optional.empty();
		}

		String normalizedOs = normalizeOsToken(osToken);
		String normalizedArch = normalizeArchToken(archToken);
		String exactTargetToken = "-" + normalizedOs + "-" + normalizedArch;

		List<AssetCandidate> candidates = new ArrayList<>();
		for (UpdateAsset asset : safeAssets) {
			if (asset != null && !asset.name().isBlank() && !asset.downloadUrl().isBlank()) {
				String normalizedName = asset.name().toLowerCase(Locale.ROOT);
				if (isSupportedPackageAsset(normalizedName)) {
					int targetRank = resolveTargetRank(normalizedName, normalizedOs, normalizedArch, exactTargetToken);
					int formatRank = resolveFormatRank(normalizedName, normalizedOs, preferPortablePackage);
					int standalonePenalty = normalizedName.contains("standalone") ? 1 : 0;
					candidates.add(new AssetCandidate(asset, targetRank, formatRank, standalonePenalty,
							normalizedName.length()));
				}
			}
		}

		Comparator<AssetCandidate> comparator = Comparator.comparingInt(AssetCandidate::targetRank)
				.thenComparingInt(AssetCandidate::formatRank).thenComparingInt(AssetCandidate::standalonePenalty)
				.thenComparingInt(AssetCandidate::nameLength);

		return candidates.stream().sorted(comparator).filter(candidate -> candidate.targetRank() < 4)
				.map(AssetCandidate::asset).findFirst();
	}

	private static boolean isSupportedPackageAsset(String normalizedName) {
		if (normalizedName == null || normalizedName.isBlank()) {
			return false;
		}
		return normalizedName.endsWith(".msi") || normalizedName.endsWith(".exe") || normalizedName.endsWith(".zip")
				|| normalizedName.endsWith(".deb") || normalizedName.endsWith(".rpm")
				|| normalizedName.endsWith(".appimage") || normalizedName.endsWith(".tar.gz")
				|| normalizedName.endsWith(".dmg") || normalizedName.endsWith(".pkg")
				|| normalizedName.endsWith(".jar");
	}

	private static int resolveTargetRank(String normalizedName, String osToken, String archToken,
			String exactTargetToken) {
		String assetOsToken = detectAssetOsToken(normalizedName);
		String assetArchToken = detectAssetArchToken(normalizedName);
		boolean hasAssetOs = !assetOsToken.isBlank();
		boolean hasAssetArch = !assetArchToken.isBlank();
		boolean osMatches = !hasAssetOs || assetOsToken.equals(osToken);
		boolean archMatches = !hasAssetArch || assetArchToken.equals(archToken);

		if (normalizedName.contains(exactTargetToken)) {
			return 0;
		}
		if (osMatches && hasAssetArch && archMatches) {
			return 1;
		}
		if (osMatches && !hasAssetArch) {
			return 2;
		}
		if (normalizedName.endsWith(".jar") && osMatches && archMatches) {
			return 3;
		}
		if (osMatches && hasAssetArch && !archMatches) {
			return 6;
		}
		if (normalizedName.endsWith(".jar")) {
			return 7;
		}
		return 4;
	}

	private static String detectAssetOsToken(String normalizedName) {
		if (normalizedName == null || normalizedName.isBlank()) {
			return "";
		}
		if (containsTokenWithBoundary(normalizedName, "windows")) {
			return "windows";
		}
		if (containsTokenWithBoundary(normalizedName, "macos") || containsTokenWithBoundary(normalizedName, "darwin")) {
			return "macos";
		}
		if (containsTokenWithBoundary(normalizedName, "linux")) {
			return "linux";
		}
		return "";
	}

	private static String detectAssetArchToken(String normalizedName) {
		if (normalizedName == null || normalizedName.isBlank()) {
			return "";
		}
		if (containsTokenWithBoundary(normalizedName, "arm64")
				|| containsTokenWithBoundary(normalizedName, "aarch64")) {
			return "arm64";
		}
		if (containsTokenWithBoundary(normalizedName, "x64") || containsTokenWithBoundary(normalizedName, "x86_64")
				|| containsTokenWithBoundary(normalizedName, "amd64")) {
			return "x64";
		}
		return "";
	}

	private static boolean containsTokenWithBoundary(String text, String token) {
		if (text == null || text.isBlank() || token == null || token.isBlank()) {
			return false;
		}
		int fromIndex = 0;
		while (fromIndex < text.length()) {
			int index = text.indexOf(token, fromIndex);
			if (index < 0) {
				return false;
			}
			int leftIndex = index - 1;
			int rightIndex = index + token.length();
			boolean leftBoundary = leftIndex < 0 || !Character.isLetterOrDigit(text.charAt(leftIndex));
			boolean rightBoundary = rightIndex >= text.length() || !Character.isLetterOrDigit(text.charAt(rightIndex));
			if (leftBoundary && rightBoundary) {
				return true;
			}
			fromIndex = index + token.length();
		}
		return false;
	}

	private static int resolveFormatRank(String normalizedName, String osToken, boolean preferPortablePackage) {
		return switch (osToken) {
			case "windows" -> resolveWindowsFormatRank(normalizedName, preferPortablePackage);
			case "macos" -> resolveMacOsFormatRank(normalizedName);
			case "linux" -> resolveLinuxFormatRank(normalizedName);
			default -> normalizedName.endsWith(".jar") ? 1 : 2;
		};
	}

	private static int resolveWindowsFormatRank(String normalizedName, boolean preferPortablePackage) {
		if (preferPortablePackage) {
			if (normalizedName.endsWith(".zip") && isPortableAssetName(normalizedName)) {
				return 0;
			}
			if (normalizedName.endsWith(".zip")) {
				return 1;
			}
			if (normalizedName.endsWith(".msi")) {
				return 2;
			}
			if (normalizedName.endsWith(".exe")) {
				return 3;
			}
			if (normalizedName.endsWith(".jar")) {
				return 5;
			}
			return 4;
		}
		if (normalizedName.endsWith(".msi")) {
			return 0;
		}
		if (normalizedName.endsWith(".exe")) {
			return 1;
		}
		if (normalizedName.endsWith(".zip") && isPortableAssetName(normalizedName)) {
			return 2;
		}
		if (normalizedName.endsWith(".zip")) {
			return 3;
		}
		if (normalizedName.endsWith(".jar")) {
			return 6;
		}
		return 4;
	}

	private static boolean isPortableAssetName(String normalizedName) {
		return containsTokenWithBoundary(normalizedName, "portable");
	}

	private static int resolveMacOsFormatRank(String normalizedName) {
		if (normalizedName.endsWith(".dmg")) {
			return 0;
		}
		if (normalizedName.endsWith(".pkg")) {
			return 1;
		}
		if (normalizedName.endsWith(".zip")) {
			return 2;
		}
		if (normalizedName.endsWith(".tar.gz")) {
			return 3;
		}
		if (normalizedName.endsWith(".jar")) {
			return 5;
		}
		return 4;
	}

	private static int resolveLinuxFormatRank(String normalizedName) {
		if (normalizedName.endsWith(".deb")) {
			return 0;
		}
		if (normalizedName.endsWith(".rpm")) {
			return 1;
		}
		if (normalizedName.endsWith(".appimage")) {
			return 2;
		}
		if (normalizedName.endsWith(".tar.gz")) {
			return 3;
		}
		if (normalizedName.endsWith(".jar")) {
			return 5;
		}
		return 4;
	}

	static ParsedRelease parseReleasePayload(String responseBody) {
		String safeBody = normalizeText(responseBody);
		if (safeBody.isBlank()) {
			return null;
		}

		Map<String, String> rootFields = parseObjectFields(safeBody);
		if (rootFields.isEmpty()) {
			return null;
		}

		String tagName = parseStringLiteral(rootFields.get("tag_name"));
		String name = parseStringLiteral(rootFields.get("name"));
		String htmlUrl = parseStringLiteral(rootFields.get("html_url"));
		Instant publishedAt = parseInstantLiteral(rootFields.get("published_at"));
		boolean draft = parseBooleanLiteral(rootFields.get("draft"), false);
		boolean prerelease = parseBooleanLiteral(rootFields.get("prerelease"), false);
		List<UpdateAsset> assets = parseAssetArray(rootFields.get("assets"));

		if (tagName.isBlank() || htmlUrl.isBlank()) {
			return null;
		}
		return new ParsedRelease(tagName, name, htmlUrl, publishedAt, draft, prerelease, assets);
	}

	private static List<UpdateAsset> parseAssetArray(String assetsRaw) {
		List<String> values = parseArrayValues(assetsRaw);
		if (values.isEmpty()) {
			return List.of();
		}
		List<UpdateAsset> assets = new ArrayList<>();
		for (String value : values) {
			Map<String, String> fields = parseObjectFields(value);
			if (!fields.isEmpty()) {
				String assetName = parseStringLiteral(fields.get("name"));
				String downloadUrl = parseStringLiteral(fields.get("browser_download_url"));
				if (!assetName.isBlank() && !downloadUrl.isBlank()) {
					assets.add(new UpdateAsset(assetName, downloadUrl));
				}
			}
		}
		return List.copyOf(assets);
	}

	private static List<String> parseArrayValues(String rawArray) {
		String safeArray = normalizeText(rawArray);
		if (safeArray.isBlank() || safeArray.charAt(0) != '[') {
			return List.of();
		}

		List<String> values = new ArrayList<>();
		int index = 1;
		while (index < safeArray.length()) {
			index = skipWhitespaces(safeArray, index);
			if (index < safeArray.length() && safeArray.charAt(index) != ']') {
				JsonSlice valueSlice = readJsonValueSlice(safeArray, index);
				if (valueSlice == null) {
					return List.of();
				}
				values.add(valueSlice.raw());
				index = skipWhitespaces(safeArray, valueSlice.nextIndex());
				if (index < safeArray.length() && safeArray.charAt(index) == ',') {
					index++;
				}
			} else {
				index = safeArray.length();
			}
		}
		return List.copyOf(values);
	}

	private static Map<String, String> parseObjectFields(String rawObject) {
		String safeObject = normalizeText(rawObject);
		if (safeObject.isBlank() || safeObject.charAt(0) != '{') {
			return Map.of();
		}

		Map<String, String> fields = new LinkedHashMap<>();
		int index = 1;
		while (index < safeObject.length()) {
			index = skipWhitespaces(safeObject, index);
			if (index < safeObject.length() && safeObject.charAt(index) != '}') {
				ObjectFieldParseResult fieldResult = parseObjectField(safeObject, index);
				if (fieldResult == null) {
					return Map.of();
				}
				fields.put(fieldResult.key(), fieldResult.rawValue());
				index = skipWhitespaces(safeObject, fieldResult.nextIndex());
				if (index < safeObject.length() && safeObject.charAt(index) == ',') {
					index++;
				}
			} else {
				index = safeObject.length();
			}
		}
		return fields;
	}

	private static ObjectFieldParseResult parseObjectField(String safeObject, int index) {
		JsonStringToken keyToken = parseJsonString(safeObject, index);
		if (keyToken == null) {
			return null;
		}
		int separatorIndex = skipWhitespaces(safeObject, keyToken.nextIndex());
		if (separatorIndex >= safeObject.length() || safeObject.charAt(separatorIndex) != ':') {
			return null;
		}
		int valueIndex = skipWhitespaces(safeObject, separatorIndex + 1);
		JsonSlice valueSlice = readJsonValueSlice(safeObject, valueIndex);
		if (valueSlice == null) {
			return null;
		}
		return new ObjectFieldParseResult(keyToken.value(), valueSlice.raw(), valueSlice.nextIndex());
	}

	private static JsonSlice readJsonValueSlice(String text, int index) {
		if (index >= text.length()) {
			return null;
		}
		char current = text.charAt(index);
		if (current == '"') {
			JsonStringToken token = parseJsonString(text, index);
			if (token == null) {
				return null;
			}
			return new JsonSlice(text.substring(index, token.nextIndex()), token.nextIndex());
		}
		if (current == '{') {
			return readNestedSlice(text, index, '{', '}');
		}
		if (current == '[') {
			return readNestedSlice(text, index, '[', ']');
		}

		int cursor = index;
		while (cursor < text.length()) {
			char valueChar = text.charAt(cursor);
			if (valueChar == ',' || valueChar == '}' || valueChar == ']') {
				break;
			}
			cursor++;
		}
		if (cursor <= index) {
			return null;
		}
		return new JsonSlice(text.substring(index, cursor).trim(), cursor);
	}

	private static JsonSlice readNestedSlice(String text, int index, char openChar, char closeChar) {
		int depth = 0;
		boolean inString = false;
		boolean escaping = false;

		for (int cursor = index; cursor < text.length(); cursor++) {
			char current = text.charAt(cursor);
			if (inString) {
				if (escaping) {
					escaping = false;
				} else if (current == '\\') {
					escaping = true;
				} else if (current == '"') {
					inString = false;
				}
			} else if (current == '"') {
				inString = true;
			} else if (current == openChar) {
				depth++;
			} else if (current == closeChar) {
				depth--;
				if (depth == 0) {
					return new JsonSlice(text.substring(index, cursor + 1), cursor + 1);
				}
			}
		}
		return null;
	}

	private static JsonStringToken parseJsonString(String text, int startIndex) {
		if (startIndex >= text.length() || text.charAt(startIndex) != '"') {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		int cursor = startIndex + 1;
		while (cursor < text.length()) {
			char current = text.charAt(cursor);
			if (current == '"') {
				return new JsonStringToken(builder.toString(), cursor + 1);
			}
			if (current == '\\') {
				EscapedJsonChar escapedChar = parseEscapedJsonChar(text, cursor + 1);
				if (escapedChar == null) {
					return null;
				}
				builder.append(escapedChar.value());
				cursor = escapedChar.nextIndex();
			} else {
				builder.append(current);
				cursor++;
			}
		}
		return null;
	}

	private static EscapedJsonChar parseEscapedJsonChar(String text, int index) {
		if (index >= text.length()) {
			return null;
		}
		char escaped = text.charAt(index);
		return switch (escaped) {
			case '"', '\\', '/' -> new EscapedJsonChar(String.valueOf(escaped), index + 1);
			case 'b' -> new EscapedJsonChar(String.valueOf('\b'), index + 1);
			case 'f' -> new EscapedJsonChar(String.valueOf('\f'), index + 1);
			case 'n' -> new EscapedJsonChar(String.valueOf('\n'), index + 1);
			case 'r' -> new EscapedJsonChar(String.valueOf('\r'), index + 1);
			case 't' -> new EscapedJsonChar(String.valueOf('\t'), index + 1);
			case 'u' -> parseUnicodeEscapedChar(text, index);
			default -> new EscapedJsonChar(String.valueOf(escaped), index + 1);
		};
	}

	private static EscapedJsonChar parseUnicodeEscapedChar(String text, int index) {
		if (index + 4 >= text.length()) {
			return null;
		}
		String hex = text.substring(index + 1, index + 5);
		try {
			return new EscapedJsonChar(String.valueOf((char) Integer.parseInt(hex, 16)), index + 5);
		} catch (NumberFormatException _) {
			return null;
		}
	}

	private static boolean parseBooleanLiteral(String rawValue, boolean defaultValue) {
		String safeRaw = normalizeText(rawValue);
		if ("true".equalsIgnoreCase(safeRaw)) {
			return true;
		}
		if ("false".equalsIgnoreCase(safeRaw)) {
			return false;
		}
		return defaultValue;
	}

	private static Instant parseInstantLiteral(String rawValue) {
		String timestamp = parseStringLiteral(rawValue);
		if (timestamp.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(timestamp);
		} catch (Exception e) {
			LOGGER.debug("Invalid release published_at value '{}'", timestamp, e);
			return null;
		}
	}

	private static String parseStringLiteral(String rawValue) {
		String safeRaw = normalizeText(rawValue);
		if (safeRaw.isBlank() || safeRaw.charAt(0) != '"') {
			return "";
		}
		JsonStringToken token = parseJsonString(safeRaw, 0);
		if (token == null) {
			return "";
		}
		return normalizeText(token.value());
	}

	private static int skipWhitespaces(String value, int start) {
		int index = Math.max(0, start);
		while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
			index++;
		}
		return index;
	}

	private static void dispatch(Consumer<CheckResult> callback, CheckResult result) {
		if (callback == null) {
			return;
		}
		if (Platform.isFxApplicationThread()) {
			callback.accept(result);
			return;
		}
		Platform.runLater(() -> callback.accept(result));
	}

	private static String normalizeOsToken(String osName) {
		String safeOsName = normalizeText(osName).toLowerCase(Locale.ROOT);
		if (safeOsName.contains("win")) {
			return "windows";
		}
		if (safeOsName.contains("mac")) {
			return "macos";
		}
		return "linux";
	}

	private static String normalizeArchToken(String archName) {
		String safeArch = normalizeText(archName).toLowerCase(Locale.ROOT);
		if ("amd64".equals(safeArch) || "x86_64".equals(safeArch)) {
			return "x64";
		}
		if ("aarch64".equals(safeArch) || "arm64".equals(safeArch)) {
			return "arm64";
		}
		return safeArch.replaceAll("[^a-z0-9]+", "");
	}

	private static String normalizeVersionToken(String version) {
		String normalized = normalizeText(version);
		if (normalized.startsWith("v") || normalized.startsWith("V")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}

	private static String normalizeVersionForDisplay(String version) {
		String normalized = normalizeVersionToken(version);
		return normalized.isBlank() ? version : normalized;
	}

	private static boolean isSnapshotLikeVersion(String version) {
		String normalized = normalizeVersionToken(version).toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return false;
		}
		return normalized.contains("snapshot") || normalized.contains("dev") || normalized.contains("alpha")
				|| normalized.contains("beta") || normalized.contains("rc");
	}

	private static String normalizeText(String value) {
		if (value == null) {
			return "";
		}
		return value.trim();
	}
}
