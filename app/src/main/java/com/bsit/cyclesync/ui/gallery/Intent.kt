package com.bsit.cyclesync.ui.gallery

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap

sealed class AlbumIntent {
    data class OnFinishPickingImagesWith(val bitmaps: List<ImageBitmap>) : AlbumIntent()
    data class OnImageSavedWith(val bitmap: ImageBitmap) : AlbumIntent()
    object OnImageSavingCanceled : AlbumIntent()
    data class OnImageClicked(val index: Int) : AlbumIntent()
    object OnCloseImagePreview : AlbumIntent()
    data class OnDeleteImage(val index: Int) : AlbumIntent()
    object OnConfirmDelete : AlbumIntent()
    object OnCancelDelete : AlbumIntent()
}