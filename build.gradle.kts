import io.papermc.unpick.task.*
import io.papermc.unpick.util.ArtifactVersionProvider

plugins {
    java
    `maven-publish`
    id("mc-base")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

val enigma by configurations.registering

mcBase {
    mcVersion = providers.gradleProperty("minecraft_version")
}

dependencies {
    enigma("cuchaz:enigma-swing:4.0.2")
    enigma("org.vineflower:vineflower:1.11.1") // sync with mache
    enigma(project(":enigma-plugin", "runtimeElements"))
}

val downloadJar by tasks.registering(DownloadFile::class) {
    group = "unpick"

    url = mcBase.manifest.map { it.downloads.client.url }
    expectedSha1 = mcBase.manifest.map { it.downloads.client.sha1 }

    output = project.layout.buildDirectory.file("client.jar")
    verbose = project.gradle.startParameter.logLevel != LogLevel.QUIET
}

val generateUnpickData by tasks.registering(GenerateUnpickData::class) {
    group = "unpick"
    definitions = project.layout.projectDirectory.dir("definitions")
    output = temporaryDir.resolve("unpick_combined.unpick")
}

val unpickJar by tasks.registering(UnpickJar::class) {
    group = "unpick"
    input = downloadJar.flatMap { it.output }
    output = project.layout.buildDirectory.file("client-unpicked.jar")
    definitions = generateUnpickData.flatMap { it.output }
    classpath.setFrom(configurations.minecraft)
}

tasks.register<EnigmaRunner>("enigma") {
    group = "unpick"
    description = "Runs the Enigma mapping tool"
    classpath(enigma)
    mainClass = "cuchaz.enigma.gui.Main"
    val selectedJar = if (project.findProperty("unpick") != null) {
        unpickJar.flatMap { it.output }
    } else {
        downloadJar.flatMap { it.output }
    }
    inputJar = selectedJar
    libraries.setFrom(configurations.minecraft)
}

tasks.register<CheckUnpickDefinitions>("checkUnpickDefinitions") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    input = project.layout.projectDirectory.dir("definitions")
    classpath.setFrom(
        downloadJar.flatMap { it.output },
        configurations.minecraft
    )
}

val artifactVersionProvider = providers.of(ArtifactVersionProvider::class) {
    parameters {
        repoUrl = "https://artifactory.papermc.io/artifactory/releases/"
        version = mcBase.mcVersion
        ci = providers.environmentVariable("CI").map { it.toBooleanStrict() }.orElse(false)
    }
}

publishing {
    publications.withType(MavenPublication::class).configureEach {
        pom {
            name = "Unpick definitions"
            description = "Unpick definitions for Minecraft: Java Edition."
            organization {
                name = "PaperMC"
                url = "https://github.com/PaperMC"
            }
        }
        artifactId = "unpick-definitions"
    }

    publications.register<MavenPublication>("export") {
        artifact(generateUnpickData.flatMap { it.output })
        version = artifactVersionProvider.get()
    }
}

val printVersion = tasks.register("printVersion") {
    val version = artifactVersionProvider.get()
    inputs.property("version", version)
    doFirst {
        println(version)
    }
}
