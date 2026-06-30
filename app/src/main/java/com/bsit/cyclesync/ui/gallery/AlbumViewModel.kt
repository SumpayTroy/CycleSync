package com.bsit.cyclesync.ui.gallery

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class AlbumViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("album_prefs", Application.MODE_PRIVATE)

    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance()
        .getReference("albums")
        .child(currentUser?.uid ?: "guest") // private per user

    private val _uiState = MutableStateFlow(AlbumViewState())
    val uiState: StateFlow<AlbumViewState> = _uiState

    init {
        ensureDefaultAlbum()
        loadAlbums()
        val lastAlbum = prefs.getString("last_album", "Default") ?: "Default"
        selectAlbum(lastAlbum) // restore last viewed album
    }

    private fun ensureDefaultAlbum() {
        database.child("Default").get().addOnSuccessListener {
            if (!it.exists()) {
                database.child("Default").setValue(mapOf("name" to "Default"))
            }
        }
    }

    fun selectAlbum(albumName: String) {
        _uiState.value = _uiState.value.copy(selectedAlbum = albumName)
        saveLastAlbum(albumName)
        loadPhotos(albumName)
    }

    private fun saveLastAlbum(albumName: String) {
        // concise KTX extension, no need for apply()
        prefs.edit { putString("last_album", albumName) }
    }

    fun loadAlbums() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val albums = snapshot.children.mapNotNull { it.key }
                _uiState.value = _uiState.value.copy(albums = albums)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AlbumViewModel", "loadAlbums cancelled: ${error.message}")
            }
        })
    }

    fun loadPhotos(albumName: String) {
        val dbRef = database.child(albumName).child("photos")
        _uiState.value = _uiState.value.copy(isLoading = true)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photos = snapshot.children.mapNotNull { it.getValue(PhotoItem::class.java) }
                _uiState.value = _uiState.value.copy(photos = photos, isLoading = false)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AlbumViewModel", "loadPhotos cancelled: ${error.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        })
    }

    fun addAlbum(albumName: String) {
        if (albumName.isBlank()) return
        database.child(albumName).setValue(mapOf("name" to albumName))
    }

    fun renameAlbum(oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return
        database.child(oldName).get().addOnSuccessListener { snapshot ->
            val albumData = snapshot.value
            if (albumData != null) {
                database.child(newName).setValue(albumData).addOnSuccessListener {
                    database.child(oldName).removeValue()
                    selectAlbum(newName)
                }
            }
        }
    }

    fun deleteAlbum(albumName: String) {
        if (albumName == "Default") return
        database.child(albumName).removeValue()
        selectAlbum("Default")
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()
        return if (ratio > 1) {
            bitmap.scale(maxSize, (maxSize / ratio).toInt())
        } else {
            bitmap.scale((ratio * maxSize).toInt(), maxSize)
        }
    }

    fun uploadPhotoAsBase64(albumName: String, bitmap: Bitmap) {
        val photoId = database.push().key ?: UUID.randomUUID().toString()
        val resized = resizeBitmap(bitmap)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val photoData = mapOf(
            "id" to photoId,
            "image" to base64String,
            "date" to currentDate
        )

        database.child(albumName).child("photos").child(photoId).setValue(photoData)
            .addOnSuccessListener { Log.d("AlbumUpload", "Photo saved successfully") }
            .addOnFailureListener { e -> Log.e("AlbumUpload", "Failed to save photo", e) }
    }

    fun deletePhoto(albumName: String, photoId: String) {
        database.child(albumName).child("photos").child(photoId).removeValue()
    }

    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("DecodeError", "Failed to decode image", e)
            null
        }
    }
}
