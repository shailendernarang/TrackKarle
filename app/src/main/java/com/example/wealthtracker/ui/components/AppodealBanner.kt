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
import com.appodeal.ads.NativeAd
import com.appodeal.ads.nativead.NativeAdViewNewsFeed

@Composable
fun AppodealNativeNewsFeed(
    modifier: Modifier = Modifier
) {
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(Unit) {
        // Poll until a native ad is available (max ~15s)
        repeat(10) { attempt ->
            kotlinx.coroutines.delay(if (attempt == 0) 4000L else 1000L)
            val ads = Appodeal.getNativeAds(1)
            Log.d("NativeAd", "attempt $attempt — available=${Appodeal.getAvailableNativeAdsCount()}, got=${ads.size}")
            if (ads.isNotEmpty()) {
                nativeAd = ads[0]
                return@LaunchedEffect
            }
        }
    }

    val ad = nativeAd ?: return

    AndroidView(
        factory = { ctx ->
            NativeAdViewNewsFeed(ctx).also { view ->
                view.registerView(ad, "default")
                Log.d("NativeAd", "NativeAdViewNewsFeed registered")
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Appodeal Banner Ad Component using BannerView approach.
 *
 * Renders the BannerView immediately and calls show() — the SDK internally
 * handles caching. We add a short startup delay so the SDK finishes
 * initializing before we attach the view.
 */
@Composable
fun AppodealBanner(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    // Delay briefly so the SDK completes initialization before attaching the view
    var delayDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3500) // 3.5s covers SDK init (~2-3s)
        delayDone = true
        Log.d("AppodealBanner", "startup delay done, isLoaded(BANNER)=${Appodeal.isLoaded(Appodeal.BANNER)}")
    }

    if (delayDone) {
        AndroidView(
            factory = { _ ->
                Log.d("AppodealBanner", "Factory: getBannerView")
                val bannerView = Appodeal.getBannerView(activity)
                // BannerView is a shared singleton — detach from any previous parent first
                (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                // Force full-width so the creative stretches edge-to-edge
                bannerView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                bannerView
            },
            update = { _ ->
                val shown = Appodeal.show(activity, Appodeal.BANNER_VIEW)
                Log.d("AppodealBanner", "show(BANNER_VIEW) result=$shown isLoaded=${Appodeal.isLoaded(Appodeal.BANNER)}")
            },
            modifier = modifier
        )
    }
}
