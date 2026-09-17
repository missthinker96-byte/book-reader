package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AdminScreen
import com.example.ui.LibraryScreen
import com.example.ui.LibraryViewModel
import com.example.ui.ReaderScreen
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookReaderApp()
                }
            }
        }
    }
}

@Composable
fun BookReaderApp(viewModel: LibraryViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle Android system back gestures
    BackHandler(enabled = currentScreen !is Screen.Library) {
        viewModel.navigateTo(Screen.Library)
    }

    when (currentScreen) {
        is Screen.Library -> {
            LibraryScreen(
                viewModel = viewModel,
                onOpenReader = { book ->
                    viewModel.openReaderForBook(book)
                },
                onOpenAdmin = {
                    viewModel.navigateTo(Screen.Admin)
                }
            )
        }
        is Screen.Admin -> {
            AdminScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.navigateTo(Screen.Library)
                }
            )
        }
        is Screen.Reader -> {
            ReaderScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.navigateTo(Screen.Library)
                }
            )
        }
    }
}
