package io.papermc.unpick.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

val json = Json {
    ignoreUnknownKeys = true
}

val xml = XML {
    recommended()
    defaultPolicy {
        ignoreUnknownChildren()
    }
}

val FileSystemLocation.path: Path
    get() = asFile.toPath()
val Provider<out FileSystemLocation>.path: Path
    get() = get().path
val Provider<out FileSystemLocation>.pathOrNull: Path?
    get() = orNull?.path

inline fun <reified T> HttpClient.getXml(url: String): T {
    return xml.decodeFromString(getText(url))
}

fun <P : Property<*>> P.changesDisallowed(): P = apply { disallowChanges() }

@Suppress("UNCHECKED_CAST")
val Project.download: DownloadService
    get() = (gradle.sharedServices.registrations.getByName("download").service as Provider<DownloadService>).get()

fun HttpClient.getText(url: String): String {
    val request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(url))
        .header("Cache-Control", "no-cache, max-age=0")
        .build()

    val response = send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw NotFoundException()
        }
        throw Exception("Failed to download file: $url")
    }

    return response.body()
}

class NotFoundException : Exception()
