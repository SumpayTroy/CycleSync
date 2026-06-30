package com.bsit.cyclesync.ui.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


object FirebaseUtils {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    val userRef = database.reference.child(TableConstants.USERS)
    val sessionRef = database.reference.child(TableConstants.SESSION)

}