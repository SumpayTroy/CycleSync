package com.bsit.cyclesync.ui.calendar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object NotificationUtils {
    private const val CHANNEL_ID = "ride_channel"
    private const val CHANNEL_NAME = "CycleSync Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        val prefs = context.getSharedPreferences("CycleSyncPrefs", Context.MODE_PRIVATE)
        var isEnabled = prefs.getBoolean("notificationsEnabled", true)

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        // If local cache is false, skip immediately
        if (!isEnabled) {
            println("Notifications disabled locally — skipping notification.")
            return
        }

        // Optional: verify with Firebase (in background)
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(uid).child("notificationsEnabled").get()
            .addOnSuccessListener { snapshot ->
                val firebaseEnabled = snapshot.getValue(Boolean::class.java) ?: true
                if (!firebaseEnabled) {
                    println("Notifications disabled in Firebase — skipping notification.")
                    // Update local cache too
                    prefs.edit().putBoolean("notificationsEnabled", false).apply()
                    return@addOnSuccessListener
                }

                // Safe to show notification
                createNotificationChannel(context)
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                val manager = NotificationManagerCompat.from(context)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    manager.notify(System.currentTimeMillis().toInt(), builder.build())
                }
            }
    }
}
