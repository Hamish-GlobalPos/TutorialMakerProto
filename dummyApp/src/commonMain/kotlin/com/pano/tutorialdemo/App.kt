package com.pano.tutorialdemo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.pano.tutorialmaker.TutorialMaker

@Composable
fun App(basePath: String = ".") {
    MaterialTheme {
        TutorialMaker(basePath = basePath) {
            Navigator(screen = HomeScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
