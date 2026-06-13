package com.example.wealthtracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.api.CurrencyApiService
import com.example.wealthtracker.data.api.CountryCurrencyData
import kotlinx.coroutines.launch
import com.example.wealthtracker.analytics.AnalyticsManager
import com.example.wealthtracker.analytics.TrackScreen
import androidx.compose.ui.platform.LocalContext

sealed class LoadingState {
    object Loading : LoadingState()
    data class Success(val countries: List<CountryCurrencyData>) : LoadingState()
    data class Error(val message: String) : LoadingState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionScreen(
    onCountrySelected: (CountryCurrencyData) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<CountryCurrencyData?>(null) }
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Loading) }
    val scope = rememberCoroutineScope()
    
    // Load currencies from API
    LaunchedEffect(Unit) {
        scope.launch {
            loadingState = LoadingState.Loading
            val result = CurrencyApiService.fetchSupportedCurrencies()
            loadingState = result.fold(
                onSuccess = { currencies ->
                    val mappedCountries = CurrencyApiService.getMappedCountries(currencies)
                    LoadingState.Success(mappedCountries)
                },
                onFailure = { error ->
                    // Fallback to default countries if API fails
                    LoadingState.Success(CurrencyApiService.getDefaultCountries())
                }
            )
        }
    }
    
    val filteredCountries = remember(loadingState, searchQuery) {
        when (val state = loadingState) {
            is LoadingState.Success -> {
                val countries = if (searchQuery.isBlank()) {
                    state.countries
                } else {
                    state.countries.filter {
                        it.countryName.contains(searchQuery, ignoreCase = true) ||
                        it.currencyCode.contains(searchQuery, ignoreCase = true) ||
                        it.currencyName.contains(searchQuery, ignoreCase = true) ||
                        it.countryCode.contains(searchQuery, ignoreCase = true)
                    }
                }
                // Sort: India first, then alphabetically
                countries.sortedWith(compareBy(
                    { it.countryName != "India" }, // India comes first
                    { it.countryName } // Then alphabetical
                ))
            }
            else -> emptyList()
        }
    }
    
    val context = LocalContext.current
    val analytics = remember(context) { AnalyticsManager(context) }
    TrackScreen(screenName = "CountrySelection", analyticsManager = analytics)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Your Country",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Explanation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Why do we need this?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "We need to understand your country to set the correct currency for your investments and financial tracking. This ensures accurate calculations and proper currency formatting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                enabled = loadingState is LoadingState.Success,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search country or currency") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content based on loading state
            when (val state = loadingState) {
                is LoadingState.Loading -> {
                    LoadingContent()
                }
                is LoadingState.Success -> {
                    if (filteredCountries.isEmpty()) {
                        EmptySearchResult()
                    } else {
                        CountriesList(
                            countries = filteredCountries,
                            selectedCountry = selectedCountry,
                            onCountryClick = { selectedCountry = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is LoadingState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            scope.launch {
                                loadingState = LoadingState.Loading
                                val result = CurrencyApiService.fetchSupportedCurrencies()
                                loadingState = result.fold(
                                    onSuccess = { currencies ->
                                        val mappedCountries = CurrencyApiService.getMappedCountries(currencies)
                                        LoadingState.Success(mappedCountries)
                                    },
                                    onFailure = {
                                        LoadingState.Success(CurrencyApiService.getDefaultCountries())
                                    }
                                )
                            }
                        }
                    )
                }
            }
            
            // Continue Button
            Button(
                onClick = {
                    selectedCountry?.let {
                        // Analytics: country preference set and user property
                        analytics.logCountryPreferenceSet(country = it.countryName, context = "settings_change")
                        analytics.setUserPreferences(preferredCurrency = it.currencyCode)
                        onCountrySelected(it)
                    }
                },
                enabled = selectedCountry != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedCountry != null) {
                        "Continue with ${selectedCountry!!.currencyCode}"
                    } else {
                        "Select a country to continue"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading currencies...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Fetching latest currency data from API",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ColumnScope.ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Failed to load currencies",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptySearchResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No countries found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Try a different search term",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CountriesList(
    countries: List<CountryCurrencyData>,
    selectedCountry: CountryCurrencyData?,
    onCountryClick: (CountryCurrencyData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(countries, key = { it.countryCode }) { country ->
            CountryItem(
                country = country,
                isSelected = selectedCountry == country,
                onClick = { onCountryClick(country) }
            )
        }
    }
}

@Composable
private fun CountryItem(
    country: CountryCurrencyData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag
            Text(
                text = country.flag,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.size(40.dp)
            )
            
            // Country Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = country.countryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = country.currencyCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                    Text(
                        text = "•",
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    Text(
                        text = country.currencySymbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
                if (isSelected) {
                    Text(
                        text = country.currencyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }
    }
}
