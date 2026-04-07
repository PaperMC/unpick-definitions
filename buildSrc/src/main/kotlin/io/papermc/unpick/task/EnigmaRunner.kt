package io.papermc.unpick.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.assign
import javax.inject.Inject

abstract class EnigmaRunner @Inject constructor(
    javaToolchains: JavaToolchainService
) : JavaExec() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputJar: RegularFileProperty

    @get:CompileClasspath
    abstract val libraries: ConfigurableFileCollection

    init {
        // Enigma runs on Java 17. If the Gradle JVM supports Java 17, then we are fine
        // If not, then we set the java launcher via JVM toolchain so Gradle downloads a Java 17 JVM
        if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
            javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }
    }

    override fun exec() {
        args = listOf(
            "--no-edit-all",
            "--single-class-tree",
            *libraries.files.map { "--library=${it.absolutePath}" }.toTypedArray(),
            "--jar=${inputJar.get().asFile.absolutePath}"
        )

        super.exec()
    }
}
