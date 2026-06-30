package com.bsit.cyclesync.services.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject



private const val CYCLE_SYNC: String = "cycle_sync"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = CYCLE_SYNC)

class DataStorePreferencesManager @Inject constructor(private val appContext: Context) :
    PreferencesManager {

    companion object {
        private val ACTIVE_SESSION = stringPreferencesKey("ACTIVE_SESSION")
        private val GALLERY_IMAGES = stringPreferencesKey("GALLERY_IMAGES")
    }

    override suspend fun setActiveSession(sessionId: String) {
        appContext.dataStore.edit { preference ->
            preference[ACTIVE_SESSION] = sessionId
        }
    }

    override val activeSessionId: Flow<String?>
        get() = appContext.dataStore.data.map { preference ->
            preference[ACTIVE_SESSION]
        }

    override suspend fun setBitmapString(bitmapString: List<String>) {
        appContext.dataStore.edit { preference ->
            preference[GALLERY_IMAGES] = bitmapString.joinToString { "," }
        }
    }

    override val getImageList: Flow<List<String>?>
        get() = appContext.dataStore.data.distinctUntilChanged().map { preference ->
            preference[GALLERY_IMAGES]?.split(",")?.toList()
        }

}