package com.pano.tutorialdemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.pano.tutorialmaker.tagging.tutorialTag
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tutorial Demo") },
                    modifier = Modifier.tutorialTag("home_top_bar"),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                BottomAppBar(modifier = Modifier.tutorialTag("home_bottom_bar")) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.tutorialTag("home_nav_home")
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navigator.push(SettingsScreen()) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.tutorialTag("home_nav_settings")
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier.tutorialTag("home_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to the Demo App",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.tutorialTag("home_welcome_text")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().tutorialTag("home_primary_button")
                ) {
                    Text("Primary Action")
                }

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().tutorialTag("home_secondary_button")
                ) {
                    Text("Secondary Action")
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().tutorialTag("home_explore_button")
                ) {
                    Text("Explore Features")
                }

                OutlinedButton(
                    onClick = { navigator.push(SettingsScreen()) },
                    modifier = Modifier.fillMaxWidth().tutorialTag("home_settings_button")
                ) {
                    Text("Go to Settings")
                }
            }
        }
    }
}
