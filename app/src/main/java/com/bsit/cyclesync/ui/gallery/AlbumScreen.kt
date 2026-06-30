package com.bsit.cyclesync.ui.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bsit.cyclesync.ui.theme.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    albumName: String,
    viewModel: AlbumViewModel = hiltViewModel(),
    onRename: (oldName: String, newName: String) -> Unit = { _, _ -> },
    onDelete: (deletedAlbum: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var zoomedPhoto by remember { mutableStateOf<Bitmap?>(null) }

    // Camera launcher
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri.value != null) {
            val bitmap = try {
                val stream = context.contentResolver.openInputStream(photoUri.value!!)
                android.graphics.BitmapFactory.decodeStream(stream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            bitmap?.let {
                viewModel.uploadPhotoAsBase64(uiState.selectedAlbum, it)
                Toast.makeText(context, "Photo added successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun createImageUri(context: Context): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "CycleSync_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Taken with CycleSync Camera")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Albums",
                        style = MaterialTheme.typography.titleLarge,
                        color = White
                    )
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = GreenStart
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        floatingActionButton = {
            ThemedCaptureFAB {
                val uri = createImageUri(context)
                if (uri != null) {
                    photoUri.value = uri
                    cameraLauncher.launch(uri)
                } else {
                    Toast.makeText(context, "Failed to create image URI", Toast.LENGTH_SHORT).show()
                }
            }
        }

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Album Menu
            DropdownMenuBar(
                albums = uiState.albums,
                selectedAlbum = uiState.selectedAlbum,
                onSelect = { album: String -> viewModel.selectAlbum(album) },
                onCreate = { album: String ->
                    viewModel.addAlbum(album)
                    viewModel.selectAlbum(album)
                },
                onRename = { old: String, new: String -> viewModel.renameAlbum(old, new) },
                onDelete = { name: String -> viewModel.deleteAlbum(name) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Photos grid / Loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(color = GreenStart)
                    }
                    uiState.photos.isEmpty() -> {
                        Text(
                            text = "No photos yet.\nTap 'Capture' to add one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.photos) { photo ->
                                val bitmap = remember(photo.image) {
                                    viewModel.decodeBase64ToBitmap(photo.image)
                                }

                                bitmap?.let {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .combinedClickable(
                                                onClick = { zoomedPhoto = bitmap },
                                                onLongClick = { showDeleteDialog = photo.id }
                                            )
                                    ) {
                                        Card(
                                            shape = MaterialTheme.shapes.medium,
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        ) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Display photo date
                                        Text(
                                            text = photo.date.ifEmpty { "Unknown Date" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Delete confirmation
                if (showDeleteDialog != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = null },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deletePhoto(uiState.selectedAlbum, showDeleteDialog!!)
                                showDeleteDialog = null
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                        },
                        title = { Text("Delete Photo") },
                        text = { Text("Are you sure you want to delete this photo?") }
                    )
                }

                // Zoomed photo view
                if (zoomedPhoto != null) {
                    Dialog(onDismissRequest = { zoomedPhoto = null }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.0f))
                        ) {
                            var scale by remember { mutableFloatStateOf(1f) }
                            var offsetX by remember { mutableFloatStateOf(0f) }
                            var offsetY by remember { mutableFloatStateOf(0f) }

                            Image(
                                bitmap = zoomedPhoto!!.asImageBitmap(),
                                contentDescription = "Zoomed Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        }
                                    }
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            IconButton(
                                onClick = { zoomedPhoto = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.small
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun DropdownMenuBar(
    albums: List<String>,
    selectedAlbum: String,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var renameDialogFor by remember { mutableStateOf<String?>(null) }
    var deleteConfirmFor by remember { mutableStateOf<String?>(null) }
    var newAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Selected Album: $selectedAlbum",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Albums")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            albums.forEach { album ->
                DropdownMenuItem(
                    text = { Text(album) },
                    onClick = {
                        expanded = false
                        onSelect(album)
                    },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                expanded = false
                                renameDialogFor = album
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                            if (album != "Default") {
                                IconButton(onClick = {
                                    expanded = false
                                    deleteConfirmFor = album
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                )
            }

            Divider()

            DropdownMenuItem(
                text = { Text("➕ Create New Album", color = accentColor) },
                onClick = {
                    expanded = false
                    newAlbumDialog = true
                }
            )
        }


        // ✨ Stylish Create Album Dialog
        if (newAlbumDialog) {
            Dialog(onDismissRequest = { newAlbumDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Create New Album",
                            style = MaterialTheme.typography.titleLarge,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newAlbumName,
                            onValueChange = { newAlbumName = it },
                            label = { Text("Album Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    newAlbumDialog = false
                                    newAlbumName = ""
                                }
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newAlbumName.isNotBlank()) {
                                        onCreate(newAlbumName.trim())
                                        newAlbumDialog = false
                                        newAlbumName = ""
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Create")
                            }
                        }
                    }
                }
            }
        }

        //  Rename Dialog (kept consistent style)
        if (renameDialogFor != null) {
            Dialog(onDismissRequest = { renameDialogFor = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Rename Album",
                            style = MaterialTheme.typography.titleLarge,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newAlbumName,
                            onValueChange = { newAlbumName = it },
                            label = { Text("New Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                renameDialogFor = null
                                newAlbumName = ""
                            }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newAlbumName.isNotBlank()) {
                                        onRename(renameDialogFor!!, newAlbumName.trim())
                                        renameDialogFor = null
                                        newAlbumName = ""
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Rename")
                            }
                        }
                    }
                }
            }
        }

        // 🗑 Delete Confirmation Dialog
        if (deleteConfirmFor != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmFor = null },
                title = { Text("Delete Album") },
                text = { Text("Are you sure you want to delete '${deleteConfirmFor}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(deleteConfirmFor!!)
                        deleteConfirmFor = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmFor = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

