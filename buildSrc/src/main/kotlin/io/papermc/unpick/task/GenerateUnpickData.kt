package io.papermc.unpick.task

import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Writer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

abstract class GenerateUnpickData : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val definitions: DirectoryProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun run() {
        val files = mutableListOf<File>()
        files.addAll(definitions.asFileTree.files)
        files.sortBy { file -> file.absoluteFile.relativeTo(definitions.asFile.get()).path }

        val writer = UnpickV3Writer()
        for (file in files) {
            if (!file.name.endsWith(".unpick")) {
                continue
            }

            UnpickV3Reader(file.bufferedReader()).use { reader ->
                reader.accept(writer)
            }
        }

        output.get().asFile.writeText(writer.output.replace(System.lineSeparator(), "\n"))
    }
}
