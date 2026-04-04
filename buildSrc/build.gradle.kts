plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.3.0" // stay in sync with gradle built-in version of the DSL
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.10.0")

    implementation("io.github.pdvrieze.xmlutil:core:0.91.3")
    implementation("io.github.pdvrieze.xmlutil:serialization:0.91.3")

    // bump ASM for Java 25 support in unpick
    implementation("org.ow2.asm:asm:9.9.1")
    implementation("org.ow2.asm:asm-tree:9.9.1")

    implementation("cuchaz:enigma:4.0.2")

    implementation("net.fabricmc.unpick:unpick:3.0.0-beta.13")
    implementation("net.fabricmc.unpick:unpick-format-utils:3.0.0-beta.13")
}
