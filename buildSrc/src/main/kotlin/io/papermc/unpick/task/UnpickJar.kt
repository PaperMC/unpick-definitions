package io.papermc.unpick.task

import daomephsta.unpick.api.ConstantUninliner
import daomephsta.unpick.api.classresolvers.ClassResolvers
import daomephsta.unpick.api.constantgroupers.ConstantGroupers
import io.papermc.unpick.util.path
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*

@CacheableTask
abstract class UnpickJar : DefaultTask() {

    private companion object {
        val FS_CREATE_ARGS = mapOf("create" to "true")
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val input: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val definitions: RegularFileProperty

    @get:CompileClasspath
    abstract val classpath: ConfigurableFileCollection

    @TaskAction
    fun run() {
        output.path.deleteIfExists()

        unpick(definitions.path)
    }

    private fun unpick(definitions: Path) {
        Files.newBufferedReader(definitions).use { mappingsReader ->
            FileSystems.newFileSystem(input.path).use { inputFs ->
                val zips = mutableListOf<ZipFile>()
                try {
                    var classResolver = ClassResolvers.fromDirectory(inputFs.getPath("/"))

                    for (file in classpath.files) {
                        val zip = ZipFile(file)
                        zips.add(zip)
                        classResolver = classResolver.chain(ClassResolvers.jar(zip))
                    }

                    classResolver = classResolver.chain(ClassResolvers.classpath(null))

                    val uninliner = ConstantUninliner.builder()
                        .classResolver(classResolver)
                        .grouper(
                            ConstantGroupers.dataDriven()
                                .classResolver(classResolver)
                                .mappingSource(mappingsReader)
                                .build()
                        )
                        .build()

                    FileSystems.newFileSystem(output.path, FS_CREATE_ARGS).use { outputFs ->
                        copyContents(inputFs.getPath("/"), outputFs.getPath("/")) { input ->
                            val node = ClassNode()
                            ClassReader(input).accept(node, 0)
                            uninliner.transform(node)
                            val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                            node.accept(writer)
                            return@copyContents writer.toByteArray()
                        }
                    }
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                } finally {
                    for (zip in zips) {
                        try {
                            zip.close()
                        } catch (_: IOException) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    private fun copyContents(
        inputRoot: Path, outputRoot: Path, classTransformer: (input: InputStream) -> ByteArray
    ) {
        inputRoot.walk(PathWalkOption.INCLUDE_DIRECTORIES, PathWalkOption.BREADTH_FIRST).forEach { sourcePath ->
            if (sourcePath == inputRoot) {
                return@forEach
            }

            val relativePath = sourcePath.relativeTo(inputRoot)
            val outputPath = outputRoot.resolve(relativePath.invariantSeparatorsPathString)
            if (sourcePath.isDirectory()) {
                outputPath.createDirectory()
            } else {
                if (sourcePath.name.endsWith(".class")) {
                    outputPath.writeBytes(classTransformer(sourcePath.inputStream()))
                } else {
                    sourcePath.copyTo(outputPath)
                }
            }
        }
    }
}
