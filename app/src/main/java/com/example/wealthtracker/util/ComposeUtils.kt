package com.example.wealthtracker.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.compositionLocalOf

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** CompositionLocal for the host Activity — survives context overrides like createConfigurationContext(). */
val LocalActivity = compositionLocalOf<Activity?> { null }
