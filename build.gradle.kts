plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "fr.inria.corese.demo"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // JavaFX
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
    implementation("org.openjfx:javafx-web:21")
    implementation("org.openjfx:javafx-base:22.0.1")

    // AtlantaFX - Theme moderne pour JavaFX
    implementation("io.github.mkpaz:atlantafx-base:2.0.1")

    // Material FX
    implementation("io.github.palexdev:materialfx:11.17.0")

    // RichTextFX pour l'éditeur de code
    implementation("org.fxmisc.richtext:richtextfx:0.11.2")

    // Apache Jena pour le traitement RDF
    implementation("org.apache.jena:jena-core:4.10.0")

    // Ikonli
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-materialdesign-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:12.3.1")

    // Corese
    implementation("fr.inria.corese:corese-core:4.6.2")

    // JAXB
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.1")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Logging - SLF4J with Logback
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("ch.qos.logback:logback-core:1.4.11")
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml", "javafx.web")
}

application {
    mainClass.set("fr.inria.corese.demo.App")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
