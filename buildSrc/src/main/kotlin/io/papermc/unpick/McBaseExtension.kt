package io.papermc.unpick

import io.papermc.unpick.data.GameManifest
import io.papermc.unpick.util.changesDisallowed
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class McBaseExtension @Inject constructor(
    manifest: Provider<GameManifest>,
    objects: ObjectFactory
) {
    val mcVersion: Property<String> = objects.property()

    val manifest: Provider<GameManifest> = objects.property<GameManifest>().value(manifest).changesDisallowed()
}
