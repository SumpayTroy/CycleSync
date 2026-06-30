package com.bsit.cyclesync.di

import android.content.Context
import com.bsit.cyclesync.services.prefs.DataStorePreferencesManager
import com.bsit.cyclesync.services.prefs.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton



@InstallIn(SingletonComponent::class)
@Module
object SharedPreferenceModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext appContext: Context): PreferencesManager =
        DataStorePreferencesManager(appContext)
}