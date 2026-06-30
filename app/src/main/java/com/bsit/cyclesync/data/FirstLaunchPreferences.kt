package com.bsit.cyclesync.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private val Context.firstLaunchDataStore by preferencesDataStore(name = "first_launch_prefs")

class FirstLaunchPreferences(private val context: Context) {
    companion object {
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    val isFirstLaunch: Flow<Boolean> = context.firstLaunchDataStore.data
        .map { prefs -> prefs[KEY_FIRST_LAUNCH] ?: true }

    // suspend version (used by coroutines)
    suspend fun setFirstLaunchDone() {
        withContext(Dispatchers.IO) {
            context.firstLaunchDataStore.edit { prefs ->
                prefs[KEY_FIRST_LAUNCH] = false
            }
        }
    }

    // ✅ blocking version (safe to call from Java)
    fun markFirstLaunchDoneBlocking() {
        runBlocking {
            setFirstLaunchDone()
        }
    }
}
