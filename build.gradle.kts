plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
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
    implementation("org.openjfx:javafx-fxml:25.0.1")
    implementation("org.openjfx:javafx-web:25.0.1")
    implementation("org.openjfx:javafx-swing:25.0.1")
    implementation("org.openjfx:javafx-base:25.0.1")
    // AtlantaFX - Theme moderne pour JavaFX
    implementation("io.github.mkpaz:atlantafx-base:2.1.0")

    // Ikonli
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-materialdesign-pack:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:12.4.0")

    // Corese
    implementation("fr.inria.corese:corese-core:4.6.5")

    // Logging - SLF4J with Logback
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("ch.qos.logback:logback-core:1.5.21")

    // JSVG for SVG rendering
    // TODO : bump to 2.0.0
    implementation("com.github.weisj:jsvg:1.7.2")
}

javafx {
    version = "25.0.1"
    modules("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

application {
    mainClass.set("fr.inria.corese.demo.App")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
