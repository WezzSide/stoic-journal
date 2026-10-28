package com.stoicjournal.core.data.security.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.stoicjournal.core.data.security.crypto.AesGcmCredentialEncryption
import com.stoicjournal.core.data.security.crypto.CredentialEncryption
import com.stoicjournal.core.data.security.storage.CredentialPreferences
import com.stoicjournal.core.data.security.storage.CredentialStorageSerializer
import com.stoicjournal.core.data.security.storage.SecureCredentialRepositoryImpl
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityProvidesModule {

    @Provides
    @Singleton
    fun provideCredentialDataStore(
        @ApplicationContext context: Context
    ): DataStore<CredentialPreferences> {
        return DataStoreFactory.create(
            serializer = CredentialStorageSerializer,
            produceFile = { File(context.noBackupFilesDir, "sj_credentials.pb") }
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityBindsModule {

    @Binds
    @Singleton
    abstract fun bindCredentialEncryption(
        impl: AesGcmCredentialEncryption
    ): CredentialEncryption

    @Binds
    @Singleton
    abstract fun bindSecureCredentialRepository(
        impl: SecureCredentialRepositoryImpl
    ): SecureCredentialRepository
}
