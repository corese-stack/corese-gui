import org.gradle.api.tasks.bundling.Zip

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "fr.inria.corese"
version = "5.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // JavaFX
    implementation("org.openjfx:javafx-controls:25.0.1")
    implementation("org.openjfx:javafx-web:25.0.1")
    implementation("org.openjfx:javafx-swing:25.0.1")
    implementation("org.openjfx:javafx-base:25.0.1")
    // AtlantaFX - Theme moderne pour JavaFX
    implementation("io.github.mkpaz:atlantafx-base:2.1.0")

    // Ikonli
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-materialdesign-pack:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:12.4.0")

    // Corese
    implementation("fr.inria.corese:corese-core:4.6.5")

    // Logging - SLF4J with Logback
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("ch.qos.logback:logback-core:1.5.21")

    // JSVG for SVG rendering
    implementation("com.github.weisj:jsvg:2.0.0")

    // SVG -> PNG/PDF export
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17")
    implementation("org.apache.xmlgraphics:batik-codec:1.17")
    implementation("org.apache.xmlgraphics:fop-core:2.9")

    // Tests
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "25.0.1"
    modules("javafx.controls", "javafx.web", "javafx.swing")
}

application {
    mainClass.set("fr.inria.corese.gui.App")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=javafx.graphics,javafx.web"
    )
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val osClassifier = when {
    System.getProperty("os.name").lowercase().contains("win") -> "windows"
    System.getProperty("os.name").lowercase().contains("mac") -> "macos"
    else -> "linux"
}

tasks.jar {
    archiveBaseName.set("corese-gui")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

tasks.register<Zip>("releasePackage") {
    group = "distribution"
    description = "Creates an OS-specific distributable zip with launcher and runtime dependencies."
    dependsOn(tasks.installDist)

    val installDir = layout.buildDirectory.dir("install/${project.name}")
    from(installDir)

    archiveBaseName.set("corese-gui")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set(osClassifier)
    destinationDirectory.set(layout.buildDirectory.dir("release"))
}

spotless {
    val maxFormattedFileSize = 200 * 1024L // 200 KB

    java {
        val javaTargets = fileTree("src/main/java") {
            include("**/*.java")
        }.filter { it.length() <= maxFormattedFileSize }

        target(javaTargets)
        eclipse()
        trimTrailingWhitespace()
        endWithNewline()
        ratchetFrom("HEAD")
    }

    format("misc") {
        target(
            ".editorconfig",
            "**/*.md",
            "**/*.yml",
            "**/*.yaml",
            "**/*.gradle.kts",
            "**/*.css",
            "**/*.xml"
        )
        trimTrailingWhitespace()
        endWithNewline()
        indentWithSpaces(4)
        ratchetFrom("HEAD")
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
