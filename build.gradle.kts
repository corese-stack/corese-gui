import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Locale
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.jvm.tasks.Jar

/*
 * =============================================================================
 * Corese GUI - Gradle build contract
 * =============================================================================
 *
 * Why this build is structured this way:
 * - Java bytecode is portable, JavaFX native runtime parts are OS/arch specific.
 * - This project therefore produces one desktop artifact per host target.
 *
 * Distribution model:
 * - Standalone JAR (fat jar): `corese-gui-<version>-standalone-<os>-<arch>.jar`
 * - Published native artifacts: `corese-gui-<version>-<os>-<arch>.<ext>`
 * - Windows portable archive: `corese-gui-<version>-windows-x64-portable.zip`
 * - Native bundle (jpackage): per host target in `build/jpackage/output/<os>-<arch>/`
 * - No `installDist` / `distZip` / `distTar` tasks on purpose.
 *
 * Common local commands:
 * - `./gradlew clean check`
 * - `./gradlew run`
 * - `./gradlew shadowJar`
 * - `./gradlew jpackageCurrentPlatform`
 * - `./gradlew packageCurrentPlatform`
 * - `./gradlew jpackageCurrentPlatform -PjpackageType=dmg`
 * =============================================================================
 */

plugins {
    // Java project (compilation, testing, standard jar).
    java
    application

    // JavaFX dependency wiring with OS-specific native artifacts.
    id("org.openjfx.javafxplugin") version "0.1.0"

    // Standalone/fat JAR generation.
    id("com.gradleup.shadow") version "9.3.1"

    // Maven Central publication (POM + optional signing).
    id("com.vanniktech.maven.publish") version "0.36.0"

    // Formatting/linting.
    id("com.diffplug.spotless") version "8.2.1"
}

fun normalizeProjectVersion(rawValue: String?): String {
    val candidate = rawValue?.trim().orEmpty()
    if (candidate.isBlank()) {
        return "5.0.0"
    }
    return candidate.removePrefix("refs/tags/").removePrefix("v").trim()
}

val resolvedProjectVersion = normalizeProjectVersion(findProperty("projectVersion") as String?)

/*
 * Centralized metadata to avoid hard-coded string duplication.
 * Keep coordinates and app identity stable across artifacts and CI.
 */
data class MetaInfo(
    val groupId: String,
    val artifactId: String,
    val appName: String,
    val appTechnicalName: String,
    val appVendor: String,
    val appId: String,
    val version: String,
    val javaVersion: Int,
    val description: String,
    val githubRepo: String,
    val licenseName: String,
    val licenseUrl: String,
    val mainClass: String,
    val nativeAccessOption: String,
)

val Meta = MetaInfo(
    groupId = "fr.inria.corese",
    artifactId = "corese-gui",
    appName = "Corese GUI",
    appTechnicalName = "corese-gui",
    appVendor = "Inria",
    appId = "fr.inria.corese.gui",
    version = resolvedProjectVersion,
    javaVersion = 25,
    description = "A graphical desktop application for exploring, querying, and visualizing RDF data using SPARQL and SHACL with the Corese engine.",
    githubRepo = "corese-stack/corese-gui",
    licenseName = "CeCILL-C License",
    licenseUrl = "https://cecill.info/licences/Licence_CeCILL-C_V1-en.html",
    mainClass = "fr.inria.corese.gui.Launcher",
    nativeAccessOption = "--enable-native-access=ALL-UNNAMED,javafx.graphics,javafx.web",
)

/*
 * Dependency versions grouped by concern.
 * Keep all upgrades centralized in this block.
 */
object Versions {
    const val javafx = "25.0.2"
    const val atlantafx = "2.1.0"
    const val ikonli = "12.4.0"
    const val corese = "4.6.5"
    const val slf4j = "2.0.17"
    const val logback = "1.5.32"
    const val jsvg = "2.0.0"
    const val batik = "1.19"
    const val fop = "2.11"
    const val junitBom = "6.0.3"
    const val orgJson = "20250517"
}

group = Meta.groupId
version = Meta.version

/*
 * Host target detection (used for artifact naming and jpackage output paths).
 * This build intentionally packages for the current machine target only.
 */
val hostOs = run {
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    when {
        osName.contains("win") -> "windows"
        osName.contains("mac") -> "macos"
        else -> "linux"
    }
}

val hostArch = run {
    when (System.getProperty("os.arch").lowercase(Locale.ROOT)) {
        "amd64", "x86_64" -> "x64"
        "aarch64", "arm64" -> "arm64"
        else -> System.getProperty("os.arch").lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
    }
}

val hostTarget = "$hostOs-$hostArch"

/*
 * jpackage type defaults to app-image for maximum local portability.
 * CI/release can override with: -PjpackageType=<type>
 */
val jpackageDefaultType = "app-image"
val jpackageType = (findProperty("jpackageType") as String?)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: jpackageDefaultType

val supportedJpackageTypes = when (hostOs) {
    "windows" -> setOf("app-image", "exe", "msi")
    "macos" -> setOf("app-image", "dmg", "pkg")
    else -> setOf("app-image", "deb", "rpm")
}

require(jpackageType in supportedJpackageTypes) {
    "Unsupported jpackage type '$jpackageType' for host OS '$hostOs'. Allowed: $supportedJpackageTypes"
}

/*
 * Use a user-friendly app name on launchers/menus.
 * For Linux installer formats, keep a technical lowercase name for compatibility.
 */
val jpackageAppName = if (hostOs == "linux" && jpackageType in setOf("deb", "rpm")) {
    Meta.appTechnicalName
} else {
    Meta.appName
}

/*
 * jpackage app-version cannot contain qualifiers like "-SNAPSHOT".
 * We strip qualifiers for packaging while keeping the full project version for jars.
 */
val jpackageAppVersion = Meta.version.substringBefore("-")
require(Regex("""\d+(\.\d+){0,3}""").matches(jpackageAppVersion)) {
    "Meta.version must start with a jpackage-compatible numeric version (e.g. 5.0.0). Current: ${Meta.version}"
}

/*
 * Use the Java toolchain runtime for jpackage to keep packaging deterministic
 * and independent from whatever happens to be installed in PATH.
 */
val packagingJavaHome = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(Meta.javaVersion))
}.map { it.metadata.installationPath.asFile.absolutePath }

val jpackageExecutable = packagingJavaHome.map { javaHome ->
    val executable = if (hostOs == "windows") "jpackage.exe" else "jpackage"
    file("$javaHome/bin/$executable").absolutePath
}

val startupSplashImage = file("src/main/resources/images/startup-splash-primer-dark.png")
require(startupSplashImage.exists()) {
    "Missing startup splash image at '${startupSplashImage.path}'"
}
val startupSplashJvmOption = "-splash:${startupSplashImage.absolutePath}"
val startupSplashPackagedJvmOption = "-splash:\$APPDIR/${startupSplashImage.name}"

/*
 * Per-platform icon resolution for jpackage.
 * Keep jpackage icons under packaging/.
 * Note: the app runtime icon still lives in src/main/resources/images/ for JavaFX loading.
 * Override in CI or local builds with: -PjpackageIcon=<path>
 * Note: macOS uses platform default icon if packaging/jpackage/corese-gui.icns is absent.
 */
val linuxJpackageIcon = file("packaging/jpackage/corese-gui.png")
val windowsJpackageIcon = file("packaging/jpackage/corese-gui.ico")
val macosJpackageIcon = file("packaging/jpackage/corese-gui.icns")

val overriddenJpackageIcon = (findProperty("jpackageIcon") as String?)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { file(it) }

val defaultJpackageIcon = when (hostOs) {
    "windows" -> windowsJpackageIcon
    "macos" -> macosJpackageIcon
    else -> linuxJpackageIcon
}

val jpackageIcon = overriddenJpackageIcon ?: defaultJpackageIcon

java {
    // Required for Maven publication completeness.
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Meta.javaVersion))
    }
}

repositories {
    mavenCentral()
}

/*
 * JavaFX modules are resolved by the OpenJFX plugin from `javafx {}` below.
 */
dependencies {
    // UI/UX stack
    implementation("io.github.mkpaz:atlantafx-base:${Versions.atlantafx}")
    implementation("org.kordamp.ikonli:ikonli-javafx:${Versions.ikonli}")
    implementation("org.kordamp.ikonli:ikonli-materialdesign-pack:${Versions.ikonli}")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:${Versions.ikonli}")

    // Corese engine
    implementation("fr.inria.corese:corese-core:${Versions.corese}")

    // Logging
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")

    // SVG rendering and export (PNG/PDF)
    implementation("com.github.weisj:jsvg:${Versions.jsvg}")
    implementation("org.apache.xmlgraphics:batik-transcoder:${Versions.batik}")
    implementation("org.apache.xmlgraphics:batik-codec:${Versions.batik}")
    implementation("org.apache.xmlgraphics:fop-core:${Versions.fop}")

    // Testing
    testImplementation(platform("org.junit:junit-bom:${Versions.junitBom}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.json:json:${Versions.orgJson}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = Versions.javafx
    modules("javafx.controls", "javafx.web")
}

application {
    mainClass.set(Meta.mainClass)
    applicationDefaultJvmArgs = listOf(Meta.nativeAccessOption, startupSplashJvmOption)
}

/*
 * Publication block kept active for Maven Central flow.
 * Signing is enabled only when signing keys are provided.
 */
mavenPublishing {
    coordinates(Meta.groupId, Meta.artifactId, Meta.version)

    pom {
        name.set(Meta.artifactId)
        description.set(Meta.description)
        url.set("https://github.com/${Meta.githubRepo}")

        licenses {
            license {
                name.set(Meta.licenseName)
                url.set(Meta.licenseUrl)
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("remiceres")
                name.set("Rémi Cérès")
                email.set("remi.ceres@inria.fr")
                organization.set("Inria")
                organizationUrl.set("https://www.inria.fr/")
            }
        }

        scm {
            url.set("https://github.com/${Meta.githubRepo}/")
            connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
            developerConnection.set("scm:git:ssh://git@github.com/${Meta.githubRepo}.git")
        }

        issueManagement {
            url.set("https://github.com/${Meta.githubRepo}/issues")
        }
    }

    publishToMavenCentral()

    if (project.hasProperty("signingInMemoryKey") || project.hasProperty("signing.keyId")) {
        signAllPublications()
    }
}

tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
    dependsOn("plainJavadocJar")
}

/*
 * Common task defaults.
 */
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:none")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    isFailOnError = false
    (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
}

// Keep only developer run task from application plugin; distribution archives are disabled on purpose.
listOf("startScripts", "installDist", "distZip", "distTar").forEach { taskName ->
    tasks.named(taskName) {
        enabled = false
    }
}

/*
 * Standard and standalone jar contract.
 */
tasks.named<Jar>("jar") {
    archiveBaseName.set(Meta.artifactId)
    manifest {
        attributes["Main-Class"] = Meta.mainClass
        attributes["Implementation-Version"] = project.version.toString()
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set(Meta.artifactId)
    archiveClassifier.set("standalone-$hostTarget")

    // Preserve and merge service loader files required by SPI-based libs.
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = Meta.mainClass
        attributes["Implementation-Version"] = project.version.toString()
    }
}

val jpackageInputDir = layout.buildDirectory.dir("jpackage/input/$hostTarget")
val jpackageOutputDir = layout.buildDirectory.dir("jpackage/output/$hostTarget")
val windowsPortableImageDir = layout.buildDirectory.dir("jpackage/portable/$hostTarget")
val windowsPortableOutputDir = layout.buildDirectory.dir("portable/$hostTarget")
val windowsPortableMarkerFileName = ".corese-portable"
val windowsPortableArchiveName = "${Meta.artifactId}-${project.version}-$hostTarget-portable.zip"
val mainJarFileName = tasks.named<Jar>("jar").flatMap { it.archiveFileName }

/*
 * Prepares jpackage classpath input:
 * - app main jar
 * - full runtime classpath dependencies
 */
tasks.register<Sync>("prepareJpackageInput") {
    group = "distribution"
    description = "Prepares classpath files consumed by jpackage for $hostTarget."
    dependsOn(tasks.named("jar"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    from(configurations.runtimeClasspath)
    from(startupSplashImage)
    into(jpackageInputDir)
}

/*
 * Builds an unsigned native bundle for the current host target.
 * Optional override: -PjpackageType=<app-image|exe|msi|dmg|pkg|deb|rpm>
 */
tasks.register<Exec>("jpackageCurrentPlatform") {
    group = "distribution"
    description = "Builds an unsigned jpackage bundle for $hostTarget."
    dependsOn(tasks.named("prepareJpackageInput"))

    inputs.property("jpackageType", jpackageType)
    inputs.property("jpackageAppName", jpackageAppName)
    inputs.property("jpackageAppVersion", jpackageAppVersion)
    inputs.property("jpackageIconPath", jpackageIcon.absolutePath)
    inputs.file(startupSplashImage)
    inputs.property("startupSplashPackagedJvmOption", startupSplashPackagedJvmOption)
    inputs.dir(jpackageInputDir)
    outputs.dir(jpackageOutputDir)

    doFirst {
        val inputDir = jpackageInputDir.get().asFile
        val outputDir = jpackageOutputDir.get().asFile
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        commandLine(
            jpackageExecutable.get(),
            "--type", jpackageType,
            "--name", jpackageAppName,
            "--dest", outputDir.absolutePath,
            "--input", inputDir.absolutePath,
            "--main-jar", mainJarFileName.get(),
            "--main-class", Meta.mainClass,
            "--app-version", jpackageAppVersion,
            "--vendor", Meta.appVendor,
            "--runtime-image", packagingJavaHome.get(),
            "--java-options", Meta.nativeAccessOption
        )

        args("--java-options", startupSplashPackagedJvmOption)

        if (jpackageIcon.exists()) {
            args("--icon", jpackageIcon.absolutePath)
        } else {
            logger.lifecycle("No jpackage icon found at '{}', using platform default icon.", jpackageIcon.path)
        }

        // Installer-specific options are intentionally skipped for app-image.
        if (jpackageType != "app-image") {
            if (hostOs == "linux" && jpackageType in setOf("deb", "rpm")) {
                args("--linux-app-category", "Utility")
            }
            if (hostOs == "windows" && jpackageType in setOf("exe", "msi")) {
                args("--win-dir-chooser")
                args("--win-shortcut")
                args("--win-menu")
                args("--win-menu-group", "Corese")
                args("--install-dir", Meta.appName)
            }
            if (hostOs == "macos" && jpackageType in setOf("dmg", "pkg")) {
                args("--mac-package-identifier", Meta.appId)
            }
        }
    }
}

if (hostOs == "windows") {
    tasks.register<Exec>("jpackagePortableCurrentPlatform") {
        group = "distribution"
        description = "Builds a portable app-image for $hostTarget."
        dependsOn(tasks.named("prepareJpackageInput"))

        inputs.property("jpackageAppName", jpackageAppName)
        inputs.property("jpackageAppVersion", jpackageAppVersion)
        inputs.property("jpackageIconPath", jpackageIcon.absolutePath)
        inputs.file(startupSplashImage)
        inputs.property("startupSplashPackagedJvmOption", startupSplashPackagedJvmOption)
        inputs.dir(jpackageInputDir)
        outputs.dir(windowsPortableImageDir)

        doFirst {
            val inputDir = jpackageInputDir.get().asFile
            val outputDir = windowsPortableImageDir.get().asFile
            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
            outputDir.mkdirs()

            commandLine(
                jpackageExecutable.get(),
                "--type", "app-image",
                "--name", jpackageAppName,
                "--dest", outputDir.absolutePath,
                "--input", inputDir.absolutePath,
                "--main-jar", mainJarFileName.get(),
                "--main-class", Meta.mainClass,
                "--app-version", jpackageAppVersion,
                "--vendor", Meta.appVendor,
                "--runtime-image", packagingJavaHome.get(),
                "--java-options", Meta.nativeAccessOption
            )

            args("--java-options", startupSplashPackagedJvmOption)

            if (jpackageIcon.exists()) {
                args("--icon", jpackageIcon.absolutePath)
            } else {
                logger.lifecycle("No jpackage icon found at '{}', using platform default icon.", jpackageIcon.path)
            }
        }
    }

    tasks.register("preparePortableRuntimeMarkerCurrentPlatform") {
        group = "distribution"
        description = "Marks the Windows portable app-image for runtime update targeting."
        dependsOn("jpackagePortableCurrentPlatform")

        val markerPath = windowsPortableImageDir.map { it.file("$jpackageAppName/$windowsPortableMarkerFileName") }
        inputs.dir(windowsPortableImageDir)
        outputs.file(markerPath)

        doLast {
            val portableAppDir = windowsPortableImageDir.get().dir(jpackageAppName).asFile
            if (!portableAppDir.isDirectory) {
                error("Portable app-image directory not found: ${portableAppDir.path}")
            }
            val markerFile = portableAppDir.resolve(windowsPortableMarkerFileName)
            markerFile.parentFile.mkdirs()
            markerFile.writeText("portable-distribution\n")
        }
    }

    tasks.register<Zip>("windowsPortableZipCurrentPlatform") {
        group = "distribution"
        description = "Builds a Windows portable ZIP for $hostTarget."
        dependsOn("preparePortableRuntimeMarkerCurrentPlatform")

        from(windowsPortableImageDir.map { it.dir(jpackageAppName) })
        destinationDirectory.set(windowsPortableOutputDir)
        archiveFileName.set(windowsPortableArchiveName)
        includeEmptyDirs = false
    }
}

tasks.register("packageCurrentPlatform") {
    group = "distribution"
    description = "Builds both standalone JAR and unsigned jpackage bundle for $hostTarget."
    dependsOn("shadowJar", "jpackageCurrentPlatform")
}

if (hostOs == "windows") {
    tasks.named("packageCurrentPlatform") {
        dependsOn("windowsPortableZipCurrentPlatform")
    }
}

// Keep build output aligned with CI/release expectations.
tasks.named("build") {
    dependsOn("shadowJar")
}

/*
 * Spotless policy:
 * - only files changed since HEAD are checked (ratchetFrom)
 * - very large files are skipped for formatting performance
 */
spotless {
    val maxFormattedFileSizeBytes = 200 * 1024L

    java {
        val javaTargets = fileTree("src") {
            include("**/*.java")
        }.filter { it.length() <= maxFormattedFileSizeBytes }

        target(javaTargets)
        eclipse()
        trimTrailingWhitespace()
        endWithNewline()
        ratchetFrom("HEAD")
    }

    format("misc") {
        val miscTargets = fileTree(".") {
            include(".editorconfig")
            include("**/*.md")
            include("**/*.yml")
            include("**/*.yaml")
            include("**/*.gradle.kts")
            include("**/*.css")
            include("**/*.xml")
            exclude(".gradle/**")
            exclude("build/**")
            exclude("packaging/flatpak/.flatpak-builder/**")
        }
        target(miscTargets)
        trimTrailingWhitespace()
        endWithNewline()
        ratchetFrom("HEAD")
    }
}
