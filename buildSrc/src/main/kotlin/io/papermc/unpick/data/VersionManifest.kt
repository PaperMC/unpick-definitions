package io.papermc.unpick.data

import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
    val id: String,
    val url: String,
    val sha1: String
)
