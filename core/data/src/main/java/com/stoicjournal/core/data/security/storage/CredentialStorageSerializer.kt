package com.stoicjournal.core.data.security.storage

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object CredentialStorageSerializer : Serializer<CredentialPreferences> {
    override val defaultValue: CredentialPreferences = CredentialPreferences()

    override suspend fun readFrom(input: InputStream): CredentialPreferences {
        return try {
            Json.decodeFromString(
                CredentialPreferences.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: CredentialPreferences, output: OutputStream) {
        output.write(Json.encodeToString(CredentialPreferences.serializer(), t).toByteArray())
    }
}
