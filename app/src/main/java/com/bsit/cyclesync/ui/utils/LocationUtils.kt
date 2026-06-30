package com.bsit.cyclesync.ui.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority


object LocationUtils {

    fun checkLocationSettings(context: Context, onGpsAvailable: () -> Unit,
                              onResolutionRequired: (IntentSenderRequest) -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // show dialog even if previously denied
            .build()

        val settingsClient = LocationServices.getSettingsClient(context)
        val task = settingsClient.checkLocationSettings(settingsRequest)

        task.addOnSuccessListener {
            // GPS is ON
            onGpsAvailable()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // GPS is OFF, show dialog
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                onResolutionRequired(intentSenderRequest)
            }
        }
    }


    @Composable
    fun GpsEnableRequest(
        onGpsEnabled: () -> Unit
    ) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onGpsEnabled()
            } else {
                Toast.makeText(context, "Location GPS is required", Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(Unit) {
            checkLocationSettings(
                context = context,
                onGpsAvailable = onGpsEnabled,
                onResolutionRequired = { intentSenderRequest ->
                    launcher.launch(intentSenderRequest)
                }
            )
        }
    }

}