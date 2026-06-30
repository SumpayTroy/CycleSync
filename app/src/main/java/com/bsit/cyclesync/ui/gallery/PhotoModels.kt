package com.bsit.cyclesync.ui.gallery

import android.net.Uri

data class PhotoItem(
    val id: String = "",
    val image: String = "",
    val date: String = ""
)

data class AlbumViewState(
    val selectedAlbum: String = "Default",
    val albums: List<String> = listOf("Default"),
    val photos: List<PhotoItem> = emptyList(),
    val tempPhotoUri: Uri? = null,
    val isLoading: Boolean = false
)