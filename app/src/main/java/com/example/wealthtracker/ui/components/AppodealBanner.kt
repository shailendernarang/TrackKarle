package com.example.wealthtracker.ui.components

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appodeal.ads.Appodeal

/**
 * Appodeal Banner Ad Component
 *
 * Per Appodeal SDK documentation:
 * - getBannerView(activity) requires Activity context
 * - show(activity, BANNER_VIEW) requires Activity context
 * - BannerView is a shared singleton — must detach from old parent before reattaching
 *
 * @param activity The Activity context required by Appodeal SDK
 * @param modifier Compose modifier for layout
 */
@Composable
fun AppodealBanner(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    // Track if SDK is initialized - show banner immediately once initialized
    var isSdkInitialized by remember { mutableStateOf(Appodeal.isInitialized(Appodeal.BANNER)) }
    
    // Wait for SDK initialization only (not for each ad load)
    LaunchedEffect(Unit) {
        if (!isSdkInitialized) {
            var attempts = 0
            while (attempts < 50) { // Wait up to 10 seconds (50 * 200ms)
                if (Appodeal.isInitialized(Appodeal.BANNER)) {
                    Log.d("AppodealBanner", "🎯 SDK initialized after ${attempts * 200}ms")
                    isSdkInitialized = true
                    break
                }
                kotlinx.coroutines.delay(200)
                attempts++
            }
            if (attempts >= 50) {
                Log.w("AppodealBanner", "⚠️ SDK not initialized after 10 seconds")
            }
        }
    }
    
    // Show banner space once SDK is initialized (banner view is shared singleton)
    if (isSdkInitialized) {
        AndroidView(
            factory = { _ ->
                Log.d("AppodealBanner", "🏗️ Factory: Creating BannerView")
                // Per Appodeal docs: getBannerView() requires Activity
                val bannerView = Appodeal.getBannerView(activity)
                
                // BannerView is a shared singleton — detach from old parent first
                val hadParent = bannerView.parent != null
                (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                Log.d("AppodealBanner", "🏗️ BannerView created (had parent: $hadParent)")
                bannerView
            },
            update = { _ ->
                // Always call show() - SDK handles whether ad is available
                val shown = Appodeal.show(activity, Appodeal.BANNER_VIEW)
                Log.d("AppodealBanner", "✅ show() called, result=$shown, isLoaded=${Appodeal.isLoaded(Appodeal.BANNER)}")
            },
            modifier = modifier
                .fillMaxWidth()
                .height(90.dp)
        )
    }
}
