package io.papermc.unpick.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GameLibrary(
    val name: String,
    val downloads: JsonObject? = null,
) {
    val isValid: Boolean
        get() = downloads != null && "artifact" in downloads
}
