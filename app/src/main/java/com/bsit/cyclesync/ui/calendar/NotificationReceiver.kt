package com.bsit.cyclesync.ui.calendar

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bsit.cyclesync.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Upcoming Ride"
        val message = intent.getStringExtra("message") ?: "It's time for your ride!"

        val prefs = context.getSharedPreferences("CycleSyncPrefs", Context.MODE_PRIVATE)
        val localEnabled = prefs.getBoolean("notificationsEnabled", true)

        if (!localEnabled) {
            println("Notifications disabled locally — skipping notification.")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().reference

        db.child("users").child(uid).child("notificationsEnabled").get()
            .addOnSuccessListener { snapshot ->
                val firebaseEnabled = snapshot.getValue(Boolean::class.java) ?: true
                if (!firebaseEnabled) {
                    prefs.edit().putBoolean("notificationsEnabled", false).apply()
                    println("Notifications disabled — skipping notification.")
                    return@addOnSuccessListener

                }

                val notification = NotificationCompat.Builder(context, "ride_channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(context)
                        .notify(System.currentTimeMillis().toInt(), notification)
                }
            }
    }
}
