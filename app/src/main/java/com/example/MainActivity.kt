package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.SuperNovaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperNovaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = AppDatabase.getDatabase(context)
                    val repository = ChatRepository(database.chatDao())
                    val viewModel: ChatViewModel = viewModel(
                        factory = ChatViewModelFactory(repository, context.applicationContext)
                    )

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "setup",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) {
                        composable("setup") {
                            SetupScreen(
                                viewModel = viewModel,
                                onSetupComplete = {
                                    navController.navigate("chat") {
                                        popUpTo("setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("chat") {
                            ChatScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
