package com.bsit.cyclesync.ui.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Context.sdpToDp(resId: Int): Dp {
    return (this.resources.getDimension(resId) / this.resources.displayMetrics.density).dp
}

@Composable
fun sdp(resId: Int): Dp {
    val context = LocalContext.current
    return context.sdpToDp(resId)
}
