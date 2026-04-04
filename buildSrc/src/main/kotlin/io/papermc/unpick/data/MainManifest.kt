package io.papermc.unpick.data

import kotlinx.serialization.Serializable

@Serializable
data class MainManifest(
    val versions: Set<VersionManifest>
)
