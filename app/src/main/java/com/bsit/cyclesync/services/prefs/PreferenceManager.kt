package com.bsit.cyclesync.services.prefs

import kotlinx.coroutines.flow.Flow



interface PreferencesManager {
    suspend fun setActiveSession(sessionId: String)

    val activeSessionId: Flow<String?>

    suspend fun setBitmapString(bitmapString: List<String>)

    val getImageList: Flow<List<String>?>
}