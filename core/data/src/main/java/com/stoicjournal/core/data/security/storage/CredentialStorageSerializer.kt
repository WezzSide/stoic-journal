package com.stoicjournal.core.data.security.storage

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

import androidx.datastore.core.CorruptionException

object CredentialStorageSerializer : Serializer<CredentialPreferences> {
    override val defaultValue: CredentialPreferences = CredentialPreferences()

    override suspend fun readFrom(input: InputStream): CredentialPreferences {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return defaultValue
        return try {
            Json.decodeFromString(
                CredentialPreferences.serializer(),
                text
            )
        } catch (e: Exception) {
            throw CorruptionException("Cannot read credentials.", e)
        }
    }

    override suspend fun writeTo(t: CredentialPreferences, output: OutputStream) {
        output.write(Json.encodeToString(CredentialPreferences.serializer(), t).toByteArray())
    }
}
