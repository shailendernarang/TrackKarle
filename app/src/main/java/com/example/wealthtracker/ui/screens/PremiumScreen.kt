package com.example.wealthtracker.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.hilt.navigation.compose.hiltViewModel
// import com.example.wealthtracker.billing.BillingManager
// import com.android.billingclient.api.ProductDetails

/**
 * Premium Features Screen
 * 
 * Shows pricing options and premium features
 * Allows users to purchase subscriptions or lifetime access
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    billingManager: Any? = null  // Temporarily Any to avoid billing dependency
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Temporary: Show static UI without billing integration
    val isPremium = false
    val isReady = true
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Features") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium status or header
            if (isPremium) {
                PremiumBadge()
            } else {
                PremiumHeader()
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pricing cards (static for now - billing integration pending)
            if (!isPremium && isReady) {
                // Monthly and Yearly side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PricingCard(
                        title = "Monthly",
                        price = "₹99",
                        period = "/month",
                        features = listOf("Cancel anytime"),
                        modifier = Modifier.weight(1f),
                        onPurchase = {
                            // TODO: Implement billing when Play Console is set up
                        }
                    )
                    
                    PricingCard(
                        title = "Yearly",
                        price = "₹999",
                        period = "/year",
                        badge = "SAVE 17%",
                        features = listOf("Best value"),
                        modifier = Modifier.weight(1f),
                        isRecommended = true,
                        onPurchase = {
                            // TODO: Implement billing when Play Console is set up
                        }
                    )
                }
                
                // Lifetime
                PricingCard(
                    title = "Lifetime",
                    price = "₹2999",
                    period = "one-time",
                    features = listOf("Pay once, use forever", "Best for long-term users"),
                    modifier = Modifier.fillMaxWidth(),
                    onPurchase = {
                        // TODO: Implement billing when Play Console is set up
                    }
                )
            } else if (!isReady) {
                CircularProgressIndicator()
                Text("Loading pricing...", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Premium features list
            PremiumFeaturesList()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Restore purchases button
            if (!isPremium) {
                TextButton(
                    onClick = { /* TODO: Implement when billing is set up */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Purchases")
                }
            }
        }
    }
}

@Composable
private fun PremiumHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Stars,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Unlock Premium",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Get the most out of TrackKaro",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PremiumBadge() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "Premium Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "You have access to all premium features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    period: String,
    features: List<String>,
    modifier: Modifier = Modifier,
    badge: String? = null,
    isRecommended: Boolean = false,
    onPurchase: () -> Unit
) {
    Card(
        modifier = modifier
            .then(
                if (isRecommended) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Badge
            if (badge != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Price
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // Features
            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Purchase button
            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Plan")
            }
        }
    }
}

@Composable
private fun PremiumFeaturesList() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Premium Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.Block,
            title = "Ad-Free Experience",
            description = "Enjoy TrackKaro without any advertisements"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.Cloud,
            title = "Cloud Sync & Backup",
            description = "Automatic backup to Google Drive, sync across devices"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.FileDownload,
            title = "Unlimited Exports",
            description = "Export your data to CSV/PDF anytime, no limits"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.Group,
            title = "Family Accounts",
            description = "Manage investments for up to 5 family members"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.Analytics,
            title = "Advanced Analytics",
            description = "XIRR calculations, sector analysis, and more"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.Notifications,
            title = "Unlimited Price Alerts",
            description = "Set unlimited alerts for stock prices and FD rates"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.AutoAwesome,
            title = "AI-Powered Insights",
            description = "Get personalized investment recommendations"
        )
        
        PremiumFeatureItem(
            icon = Icons.Default.Support,
            title = "Priority Support",
            description = "Get help faster with priority email support"
        )
    }
}

@Composable
private fun PremiumFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
