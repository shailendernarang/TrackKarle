package com.example.wealthtracker.ui.components

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appodeal.ads.Appodeal
import kotlinx.coroutines.delay

/**
 * Appodeal Banner Ad Component
 *
 * Dimensions per Appodeal docs:
 * - Standard: 320x50 (phones)
 * - Tablet: 728x90 (devices > 7 inches)
 *
 * Key fixes:
 * - getBannerView() returns a shared singleton View — must detach from old
 *   parent before reattaching, otherwise it's invisible on non-first screens
 * - show() called per screen entry so banner content is active after navigation
 * - LaunchedEffect retries until SDK initializes (fixes blank banner on cold start)
 */
@Composable
fun AppodealBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Call show() each time this composable enters composition.
    // BANNER_VIEW show() is idempotent — safe to call on every screen entry.
    LaunchedEffect(Unit) {
        val activity = (context as? Activity) ?: (view.context as? Activity) ?: return@LaunchedEffect

        // Poll until SDK is initialized (max ~5 seconds, 200ms intervals)
        var attempts = 0
        while (!Appodeal.isInitialized(Appodeal.BANNER) && attempts < 25) {
            delay(200)
            attempts++
        }

        if (Appodeal.isInitialized(Appodeal.BANNER)) {
            Log.d("AppodealBanner", "show() called after ${attempts * 200}ms wait")
            Appodeal.show(activity, Appodeal.BANNER_VIEW)
        } else {
            Log.w("AppodealBanner", "SDK not initialized after 5s, banner skipped")
        }
    }

    AndroidView(
        factory = { ctx ->
            val bannerView = Appodeal.getBannerView(ctx)
            // BannerView is a shared singleton — detach from old parent first.
            // A View can only have one parent; skipping this makes it invisible
            // on every screen after the first one that displayed it.
            (bannerView.parent as? ViewGroup)?.removeView(bannerView)
            bannerView
        },
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
    )
}
