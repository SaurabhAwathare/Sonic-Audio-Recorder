package com.saurabh.sonicaudiorecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saurabh.sonicaudiorecorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val audioFormat by viewModel.audioFormat.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val storageLocation by viewModel.storageLocation.collectAsState()

    var showFormatDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            SettingsSection(
                title = stringResource(R.string.settings_section_audio_engine),
                items = listOf(
                    SettingItemData(
                        title = stringResource(R.string.settings_audio_format_title),
                        value = audioFormat,
                        icon = Icons.Default.AudioFile
                    ) {
                        showFormatDialog = true
                    },
                    SettingItemData(
                        title = stringResource(R.string.settings_storage_location_title),
                        value = if (storageLocation == "Internal") {
                            stringResource(R.string.storage_internal)
                        } else {
                            stringResource(R.string.storage_external)
                        },
                        icon = Icons.Default.Folder
                    ) {
                        showStorageDialog = true
                    }
                )
            )

            SettingsSection(
                title = stringResource(R.string.settings_section_appearance),
                items = listOf(
                    SettingItemData(
                        title = stringResource(R.string.settings_theme_title),
                        value = when (themeMode) {
                            "Light" -> stringResource(R.string.theme_light)
                            "Dark" -> stringResource(R.string.theme_dark)
                            else -> stringResource(R.string.theme_system)
                        },
                        icon = Icons.Default.Palette
                    ) {
                        showThemeDialog = true
                    }
                )
            )

            SettingsSection(
                title = stringResource(R.string.settings_section_support),
                items = listOf(
                    SettingItemData(
                        title = stringResource(R.string.settings_privacy_policy_title),
                        value = "",
                        icon = Icons.Default.PrivacyTip
                    ) {
                        showPrivacyDialog = true
                    },
                    SettingItemData(
                        title = stringResource(R.string.settings_about_title),
                        value = stringResource(R.string.settings_about_version),
                        icon = Icons.Default.Info
                    ) {
                        showAboutDialog = true
                    }
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Dialogs
        if (showFormatDialog) {
            SelectionDialog(
                title = stringResource(R.string.settings_dialog_select_format),
                options = listOf("WAV", "M4A"),
                optionLabels = listOf(
                    stringResource(R.string.format_wav_label),
                    stringResource(R.string.format_m4a_label)
                ),
                selectedOption = audioFormat,
                onOptionSelected = {
                    viewModel.updateAudioFormat(it)
                    showFormatDialog = false
                },
                onDismiss = { showFormatDialog = false }
            )
        }

        if (showThemeDialog) {
            SelectionDialog(
                title = stringResource(R.string.settings_dialog_theme),
                options = listOf("Light", "Dark", "System Default"),
                optionLabels = listOf(
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark),
                    stringResource(R.string.theme_system)
                ),
                selectedOption = themeMode,
                onOptionSelected = {
                    viewModel.updateThemeMode(it)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showStorageDialog) {
            SelectionDialog(
                title = stringResource(R.string.settings_dialog_storage_location),
                options = listOf("Internal", "External"),
                optionLabels = listOf(
                    "${stringResource(R.string.storage_internal_label)}\n${viewModel.internalStoragePath}",
                    "${stringResource(R.string.storage_external_label)}\n${viewModel.externalStoragePath}"
                ),
                selectedOption = storageLocation,
                onOptionSelected = {
                    viewModel.updateStorageLocation(it)
                    showStorageDialog = false
                },
                onDismiss = { showStorageDialog = false }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text(stringResource(R.string.settings_dialog_close))
                    }
                },
                title = { Text(stringResource(R.string.settings_about_title)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_about_subtitle),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_about_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_about_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text(stringResource(R.string.settings_dialog_understand))
                    }
                },
                title = { Text(stringResource(R.string.settings_privacy_policy_title)) },
                text = {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }
}

data class SettingItemData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingItemData>
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingItem(item)
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(item: SettingItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.value.isNotEmpty()) {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    optionLabels: List<String> = options,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = { onOptionSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val rawLabel = optionLabels[index]
                            val lines = rawLabel.split("\n")
                            Text(
                                text = lines[0],
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (lines.size > 1) {
                                Text(
                                    text = lines[1],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
