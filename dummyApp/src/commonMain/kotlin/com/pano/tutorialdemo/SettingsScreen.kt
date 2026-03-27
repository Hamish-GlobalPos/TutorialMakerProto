package com.pano.tutorialdemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.pano.tutorialmaker.LocalTutorialContext
import com.pano.tutorialmaker.TutorialHelpButton
import com.pano.tutorialmaker.player.SectionTrigger
import com.pano.tutorialmaker.tagging.tutorialTag
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tutorialContext = LocalTutorialContext.current

        val notificationsEnabled = remember { mutableStateOf(true) }
        val darkModeEnabled = remember { mutableStateOf(false) }
        val analyticsEnabled = remember { mutableStateOf(true) }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings") },
                        modifier = Modifier.tutorialTag("settings_top_bar"),
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = { TutorialHelpButton() },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.titleLarge
                    )

                    SettingsToggleRow(
                        label = "Notifications",
                        checked = notificationsEnabled.value,
                        onCheckedChange = { notificationsEnabled.value = it },
                        modifier = Modifier.tutorialTag("settings_toggle_notifications")
                    )

                    SettingsToggleRow(
                        label = "Dark Mode",
                        checked = darkModeEnabled.value,
                        onCheckedChange = { darkModeEnabled.value = it },
                        modifier = Modifier.tutorialTag("settings_toggle_darkmode")
                    )

                    SettingsToggleRow(
                        label = "Analytics",
                        checked = analyticsEnabled.value,
                        onCheckedChange = { analyticsEnabled.value = it },
                        modifier = Modifier.tutorialTag("settings_toggle_analytics")
                    )

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().tutorialTag("settings_save_button")
                    ) {
                        Text("Save Settings")
                    }

                    Text(
                        text = "Tutorial",
                        style = MaterialTheme.typography.titleLarge
                    )

                    val tutCtx = tutorialContext
                    if (tutCtx != null) {
                        SettingsToggleRow(
                            label = "Play tutorial on every startup",
                            checked = tutCtx.playOnEveryStart,
                            onCheckedChange = { tutCtx.setPlayOnEveryStart(it) }
                        )

                        OutlinedButton(
                            onClick = { tutCtx.resetAndReplay() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset Tutorial (plays on next start)")
                        }
                    }
                }
            }

            // SectionTrigger on top of Scaffold so spotlight is visible
            val tutorial = tutorialContext?.tutorial
            val version = tutorialContext?.version ?: 0
            if (tutorial != null) {
                SectionTrigger(
                    tutorial = tutorial,
                    screenTag = "settings",
                    progressManager = tutorialContext.progressManager,
                    version = version
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
