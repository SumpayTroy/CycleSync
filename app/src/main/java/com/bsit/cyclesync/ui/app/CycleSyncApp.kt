package com.bsit.cyclesync.ui.app

import android.app.Application
import android.content.Context
import com.bsit.cyclesync.ui.utils.AppProvider
import dagger.hilt.android.HiltAndroidApp



@HiltAndroidApp
class CycleSyncApp : Application() {
    init {
        AppProvider.registerContextProvider {
            this
        }

        AppProvider.registerApplicationProvider {
            this
        }
    }

    override fun getApplicationContext(): Context {
        return this
    }
}