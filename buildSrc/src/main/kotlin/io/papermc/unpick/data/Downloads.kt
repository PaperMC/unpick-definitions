package io.papermc.unpick.data

import kotlinx.serialization.Serializable

@Serializable
data class Downloads(
    val client: ManifestFile
)
