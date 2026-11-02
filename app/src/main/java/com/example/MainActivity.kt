package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.editor.CppEditorScreen
import com.example.ui.library.AppLibraryScreen
import com.example.ui.runner.AppRunnerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TechNavyBg

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TechNavyBg
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        is Screen.Library -> {
            AppLibraryScreen(
                viewModel = viewModel,
                onNewApp = {
                    viewModel.currentEditingAppId.value = null
                    viewModel.navigateTo(Screen.Editor)
                },
                onLaunchApp = { app ->
                    viewModel.launchApp(app)
                }
            )
        }
        is Screen.Editor -> {
            CppEditorScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.navigateTo(Screen.Library)
                }
            )
        }
        is Screen.Runner -> {
            AppRunnerScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.navigateTo(Screen.Library)
                }
            )
        }
        is Screen.ApkBuilder -> {
            com.example.ui.builder.ApkBuildScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.navigateTo(Screen.Editor)
                }
            )
        }
    }
}
