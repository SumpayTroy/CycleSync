package com.bsit.cyclesync.di

import android.content.Context
import com.bsit.cyclesync.data.FirstLaunchPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Provides FirebaseAuth as a singleton
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirstLaunchPreferences(
        @ApplicationContext context: Context
    ): FirstLaunchPreferences = FirstLaunchPreferences(context)
}