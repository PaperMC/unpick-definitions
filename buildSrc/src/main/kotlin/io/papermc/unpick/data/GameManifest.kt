package io.papermc.unpick.data

import kotlinx.serialization.Serializable

@Serializable
data class GameManifest(
    val downloads: Downloads,
    val libraries: Set<GameLibrary>,
    val id: String
)
