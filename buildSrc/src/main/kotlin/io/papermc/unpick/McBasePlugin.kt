package io.papermc.unpick

import io.papermc.unpick.data.GameManifest
import io.papermc.unpick.data.MainManifest
import io.papermc.unpick.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import java.net.URI
import kotlin.io.path.readText

class McBasePlugin : Plugin<Project> {

    private companion object {
        const val MAIN_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }

    lateinit var gameManifest: GameManifest

    override fun apply(target: Project) {
        val ext = target.extensions.create<McBaseExtension>("mcBase", target.provider { gameManifest })

        target.gradle.sharedServices.registerIfAbsent("download", DownloadService::class) {}

        target.repositories {
            maven("https://libraries.minecraft.net/")
        }

        val minecraft by target.configurations.registering {
            isTransitive = false
        }

        val manifestFile = target.layout.buildDirectory.file("manifest/manifest.json")
        val mainManifestFile = target.layout.buildDirectory.file("manifest/main_manifest.json")
        download(target, MAIN_MANIFEST, mainManifestFile)

        val mainManifest = json.decodeFromString<MainManifest>(mainManifestFile.path.readText())

        target.afterEvaluate {
            val manifest = mainManifest.versions.first { it.id == ext.mcVersion.get() }

            download(target, manifest.url, manifestFile, Hash(manifest.sha1, HashingAlgorithm.SHA1))
            gameManifest = json.decodeFromString<GameManifest>(manifestFile.path.readText())

            target.dependencies {
                for (library in gameManifest.libraries) {
                    minecraft(library.name)
                }
            }
        }
    }

    private fun download(project: Project, url: String, target: Provider<RegularFile>, hash: Hash? = null) {
        project.download.download(URI.create(url), target.path, hash, {
            if (project.gradle.startParameter.logLevel != LogLevel.QUIET) {
                project.logger.lifecycle("Downloading $url...")
            }
        })
    }
}
