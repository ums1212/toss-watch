package dev.comon.toss_watch.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.datastore.DataStoreTokenStore
import dev.comon.toss_watch.core.datastore.TokenStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: DataStoreTokenStore): TokenStore

    companion object {

        private const val SESSION_DATASTORE_FILE = "toss_watch_session"
        private const val KEYSET_NAME = "toss_watch_token_keyset"
        private const val KEYSET_PREFS_FILE = "toss_watch_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://toss_watch_master_key"

        @Provides
        @Singleton
        fun provideSessionDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile(SESSION_DATASTORE_FILE) },
            )

        /**
         * Android Keystore의 마스터키로 감싼(wrapped) AES256-GCM 키셋을 준비하고
         * AEAD 프리미티브를 제공한다.
         *
         * - 실제 데이터 암호화 키(DEK)는 Tink가 생성하여 [KEYSET_PREFS_FILE]에
         *   Keystore 마스터키로 암호화된 상태로만 저장된다.
         * - 마스터키는 Keystore 하드웨어 경계 밖으로 나오지 않는다.
         */
        @Provides
        @Singleton
        fun provideTokenAead(
            @ApplicationContext context: Context,
        ): Aead {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_FILE)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
            return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        }
    }
}
