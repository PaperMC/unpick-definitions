package io.papermc.unpick.data

import kotlinx.serialization.Serializable

@Serializable
data class ManifestFile(
    val url: String,
    val sha1: String
)
