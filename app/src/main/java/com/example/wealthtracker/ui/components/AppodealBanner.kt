package com.example.wealthtracker.ui.components

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appodeal.ads.Appodeal
import com.appodeal.ads.BannerView

/**
 * Appodeal Banner Ad Component
 * 
 * Dimensions per Appodeal docs:
 * - Standard: 320x50 (phones)
 * - Tablet: 728x90 (devices > 7 inches)
 * - Smart banners auto-adjust (enabled by default)
 * 
 * Caching: Auto-caching enabled by default
 * - Banners load automatically after initialization
 * - Shared instance across activities prevents reload on tab changes
 * - Call show() once, then banner persists across screens
 */
@Composable
fun AppodealBanner(
    modifier: Modifier = Modifier,
    placementName: String = "default"
) {
    val context = LocalContext.current
    val view = LocalView.current
    var bannerShown by remember { mutableStateOf(false) }
    
    // Track when banner is shown to avoid calling show() multiple times
    DisposableEffect(Unit) {
        val activity = (context as? Activity) ?: (view.context as? Activity)
        
        // Show banner only if not already shown and SDK is initialized
        if (!bannerShown && activity != null && Appodeal.isInitialized(Appodeal.BANNER)) {
            Log.d("AppodealBanner", "Showing banner for the first time")
            Appodeal.show(activity, Appodeal.BANNER_VIEW)
            bannerShown = true
        }
        
        onDispose {
            // Don't hide banner on dispose - let it persist across screens
            Log.d("AppodealBanner", "Banner composable disposed, keeping banner visible")
        }
    }
    
    AndroidView(
        factory = { ctx ->
            Log.d("AppodealBanner", "Getting BannerView from Appodeal SDK")
            
            // Get the BannerView from Appodeal SDK
            // With setSharedAdsInstanceAcrossActivities(true), this returns the same instance
            val bannerView = Appodeal.getBannerView(ctx)
            
            if (bannerView != null) {
                Log.d("AppodealBanner", "Got banner view from Appodeal")
            } else {
                Log.w("AppodealBanner", "BannerView is null, creating fallback")
            }
            
            bannerView ?: BannerView(ctx)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp) // Standard height, will auto-adjust for tablets (728x90)
    )
}
