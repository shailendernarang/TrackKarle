package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ss.wealthtracker.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    requireDeviceLock: Boolean,
    onToggleRequireDeviceLock: () -> Unit,
    useHindiNumerals: Boolean,
    onToggleHindiNumerals: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = { Text(stringResource(id = R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRow(
                title = stringResource(id = R.string.toggle_dark_mode),
                checked = darkMode,
                onToggle = onToggleDarkMode
            )
            HorizontalDivider()
            SettingRow(
                title = if (requireDeviceLock) stringResource(id = R.string.require_device_lock_on) else stringResource(id = R.string.require_device_lock_off),
                checked = requireDeviceLock,
                onToggle = onToggleRequireDeviceLock
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(id = R.string.change_language_hindi),
                checked = useHindiNumerals,
                onToggle = onToggleHindiNumerals
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (checked) stringResource(id = R.string.settings_on) else stringResource(id = R.string.settings_off), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
