@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class)

package com.bsit.cyclesync.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsit.cyclesync.R
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.component.CircularSpeedDisplay
import com.bsit.cyclesync.ui.component.PlaceSearchField
import com.bsit.cyclesync.ui.theme.GreenStart
import com.bsit.cyclesync.ui.utils.BitmapUtils
import com.bsit.cyclesync.ui.utils.LocationUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    var currentLocation by remember { mutableStateOf(viewModel.currentLocation) }


    var isGpsEnabled by remember { mutableStateOf(false) }
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 16f)
    }
    val context = LocalContext.current
    val bikers = viewModel.bikers
    val tempBikers = viewModel.tempBikers
    val currentUser = viewModel.currentFirebaseUser
    val direction = viewModel.directionPolyLine
    val selectedBikers = viewModel.selectedBikers
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheet = viewModel.showBottomSheet
    val isActiveSession = viewModel.isActiveSession
    val speed = viewModel.speed
    val distance = viewModel.distance
    val session = viewModel.sessionId
    val canStartSession = viewModel.canStartSession
    val sessionCheckMessage = viewModel.sessionCheckMessage

    if (!isGpsEnabled) {
        LocationUtils.GpsEnableRequest(
            onGpsEnabled = {
                isGpsEnabled = true
            }
        )
    }

    LaunchedEffect(currentLocation) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        } else {
            //cameraPositionState.animate(
            //CameraUpdateFactory.newLatLng(currentLocation)
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (locationPermissionState.status.isGranted) {
        ObserveLocationUpdates { location ->
            if (currentLocation.latitude != location.latitude &&
                currentLocation.longitude != location.longitude
            ) {
                viewModel.updateCurrentLocationToFirebase()
            }
            viewModel.updateElapsedTime()
            viewModel.updateSpeed()
            viewModel.updateDistance(
                previousLocation = LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude
                ),
                newPoint = LatLng(location.latitude, location.longitude)
            )
            currentLocation = LatLng(location.latitude, location.longitude)
            //viewModel.updatePolylineTracks(currentLocation)
        }
    }
    
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }
    LaunchedEffect(selectedBikers, tempBikers) {
        viewModel.checkSessionValidity()
    }
    // Existing LaunchedEffect for creators
    LaunchedEffect(viewModel.isActiveSession) {
        if (viewModel.isActiveSession) {
            viewModel.loadMembersRealtime()  // Show only ride members
        } else {
            viewModel.loadTempBikers()        // Show friends list if not in session
        }
    }
// NEW: LaunchedEffect for participants (to reset bikers when session ends)
    LaunchedEffect(viewModel.isAsMemberActive) {
        if (!viewModel.isAsMemberActive) {
            viewModel.loadTempBikers()  // Reset to friends when not in session
        }
    }




    Scaffold(
        modifier = Modifier
            .fillMaxSize()
    ) { innerPadding ->
        Box {
            MapComponent(
                modifier = Modifier,
                location = currentLocation,
                cameraPositionState = cameraPositionState,
                isLocationEnabled = locationPermissionState.status.isGranted,
                bikers = bikers,
                user = currentUser,
                direction = direction
            )
            AnimatedVisibility(
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart),
                visible = viewModel.directionPolyLine.isNotEmpty()
            ) {
                Column(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 76.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularSpeedDisplay(
                        speed = speed.toFloat(),
                        size = 100.dp,
                        backgroundColor = Color.DarkGray,
                        contentColor = Color.White,
                        speedTextColor = Color.Cyan
                    )
                }
            }
            if (session.isEmpty()) {
                Button(
                    modifier = Modifier
                        .padding(innerPadding)
                        .align(alignment = Alignment.BottomCenter)
                        .width(250.dp) // smaller fixed width
                        .height(60.dp) // optional: reduce height
                        .padding( bottom = 10.dp),
                    colors = ButtonColors(
                        contentColor = Color.White,
                        containerColor = GreenStart,
                        disabledContentColor = Color.White,
                        disabledContainerColor = Color.Gray
                    ),
                    enabled = isActiveSession || viewModel.trialSessionsLeft > 0,
                    onClick = {
                        if (!isActiveSession) {
                            if (viewModel.trialSessionsLeft > 0) {
                                viewModel.onShowBottomSheet()
                            } else {
                                Toast.makeText(
                                    context,
                                    "You've reached your trial session, you need to subscribe.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            viewModel.onStopSession()
                        }
                    }
                ) {
                    if (isActiveSession) {
                        Text(text = stringResource(R.string.ride_session_stop))
                    } else {
                        Text(
                            text = if (viewModel.trialSessionsLeft > 0) {
                                "Start Ride (${viewModel.trialSessionsLeft} left)"
                            } else {
                                "Subscribe to Continue"
                            }
                        )
                    }
                }
            }

            if (!locationPermissionState.status.isGranted) {
                RequestPermissionPopUp(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(alignment = Alignment.Center),
                    locationPermissionState
                )
            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .padding(top = 16.dp),
                onDismissRequest = viewModel::onShowBottomSheet,
                sheetState = bottomSheetState,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
                    ) {
                        PlaceSearchField(
                            label = stringResource(R.string.ride_from),
                            value = viewModel.fromQuery,
                            onValueChange = viewModel::onFromQueryChanged,
                            predictions = viewModel.fromPredictions,
                            onPredictionClick = viewModel::onFromPredictionSelected
                        )

                        PlaceSearchField(
                            label = stringResource(R.string.ride_to),
                            value = viewModel.toQuery,
                            onValueChange = viewModel::onToQueryChanged,
                            predictions = viewModel.toPredictions,
                            onPredictionClick = viewModel::onToPredictionSelected
                        )
                        // Dynamic warning based on session validity
                        Text(
                            text = sessionCheckMessage ?: "Note: Creating this ride will deduct 1 trial session for you and all selected members.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sessionCheckMessage != null) Color.Red else Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Surface(color = MaterialTheme.colorScheme.surface) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                            ) {
                                itemsIndexed(
                                    items = tempBikers, key = { pos, user -> tempBikers[pos].uid }
                                ) { index, user ->
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.onSelectBiker(user)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedBikers.contains(user),
                                            onCheckedChange = {
                                                viewModel.onSelectBiker(user)
                                            }
                                        )
                                        Column {  // Use Column to stack name and trials
                                            Text(
                                                text = user.username.ifEmpty { "Unknown" },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "${user.trialSessionsLeft} trials left",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                            .padding(bottom = 32.dp)
                            .align(alignment = Alignment.BottomCenter),
                        colors = ButtonColors(
                            contentColor = Color.White,
                            containerColor = GreenStart,
                            disabledContentColor = Color.White,
                            disabledContainerColor = Color.Gray
                        ),
                        enabled = canStartSession,
                        onClick = {
                            viewModel.onShowBottomSheet(startRide = true)
                        }
                    ) {
                        Text(
                            text = if (canStartSession) {
                                stringResource(R.string.ride_submit)
                            } else {
                                "Subscribe to Continue"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapComponent(
    modifier: Modifier,
    location: LatLng,
    cameraPositionState: CameraPositionState,
    isLocationEnabled: Boolean,
    bikers: List<User>,
    user: FirebaseUser?,
    direction: List<LatLng>
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var fromIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var toIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = isLocationEnabled),
        onMapLoaded = {
            icon = BitmapUtils.bitmapDescriptorFromVector(
                context = context,
                vectorResId = R.drawable.cycle_sync_marker,
                height = 110,
                width = 80
            )
            fromIcon = BitmapUtils.bitmapDescriptorFromVector(
                context = context,
                vectorResId = R.drawable.ic_marker_from
            )
            toIcon = BitmapUtils.bitmapDescriptorFromVector(
                context = context,
                vectorResId = R.drawable.ic_marker_to
            )
        }
    ) {
        bikers.forEach { biker ->
            if (user != null && user.uid == biker.uid) {
                Marker(
                    state = MarkerState(position = location),
                    title = stringResource(R.string.ride_snippet),
                    icon = icon
                )
            } else {
                biker.currentLocation?.let {
                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                it.latitude,
                                it.longitude
                            )
                        ),
                        title = biker.username.ifEmpty { "Unknown" },
                        snippet = biker.username.ifEmpty { "Unknown" },
                        icon = icon
                    )
                }

            }
        }
        if (direction.isNotEmpty()) {
            Polyline(
                points = direction,
                color = Color.Blue,
                width = 8f
            )
            Marker(
                state = MarkerState(position = direction.first()),
                icon = fromIcon
            )
            Marker(
                state = MarkerState(position = direction.last()),
                icon = toIcon
            )
        }
    }
}

@Composable
fun RequestPermissionPopUp(
    modifier: Modifier,
    permissionState: PermissionState
) {
    Column(modifier = modifier) {
        val textToShow = if (permissionState.status.shouldShowRationale) {
            // If the user has denied the permission but the rationale can be shown,
            // then gently explain why the app requires this permission
            stringResource(R.string.location_permission_rationale)
        } else {
            // If it's the first time the user lands on this feature, or the user
            // doesn't want to be asked again for this permission, explain that the
            // permission is required
            stringResource(R.string.location_permission_rationale_2)
        }
        Text(textToShow)
        Button(onClick = { permissionState.launchPermissionRequest() }) {
            Text("Request permission")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ObserveLocationUpdates(
    onLocationChanged: (Location) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000L) // every 5 seconds
            .setMinUpdateIntervalMillis(2000L)
            .build()
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    onLocationChanged(location)
                }
            }
        }
    }

    // Start location updates
    DisposableEffect(Unit) {
        @SuppressLint("MissingPermission") // Make sure to request permissions before this
        val startUpdates = {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        startUpdates()

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
