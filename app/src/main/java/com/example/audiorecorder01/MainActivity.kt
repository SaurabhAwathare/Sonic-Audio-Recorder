package com.saurabh.sonicaudiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saurabh.sonicaudiorecorder.data.SettingsRepository
import com.saurabh.sonicaudiorecorder.ui.RecorderScreen
import com.saurabh.sonicaudiorecorder.ui.RecordingScreen
import com.saurabh.sonicaudiorecorder.ui.RecorderViewModel
import com.saurabh.sonicaudiorecorder.ui.SettingsScreen
import com.saurabh.sonicaudiorecorder.ui.SettingsViewModel
import com.saurabh.sonicaudiorecorder.ui.theme.AudioRecorder01Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Suppress("KotlinConstantConditions")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "System Default")
            
            AudioRecorder01Theme(themeMode = themeMode) {
                val context = LocalContext.current
                val navController = rememberNavController()
                
                // State to track if all necessary permissions are granted
                var permissionsGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                         ContextCompat.checkSelfPermission(
                             context,
                             Manifest.permission.POST_NOTIFICATIONS
                         ) == PackageManager.PERMISSION_GRANTED)
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val audioGranted = result[Manifest.permission.RECORD_AUDIO] ?: false
                    val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result[Manifest.permission.POST_NOTIFICATIONS] ?: false
                    } else true

                    permissionsGranted = audioGranted && notificationGranted
                    
                    if (!audioGranted) {
                        Toast.makeText(
                            context,
                            "Audio recording permission is required",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(
                            context,
                            "Notification permission is recommended for background controls",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    
                    if (!permissionsGranted) {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }

                NavHost(navController = navController, startDestination = "recorder") {
                    composable("recorder") {
                        val recorderViewModel: RecorderViewModel = hiltViewModel()
                        RecorderScreen(
                            viewModel = recorderViewModel,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToRecording = { navController.navigate("recording_flow") }
                        )
                    }
                    composable("recording_flow") {
                        val recorderViewModel: RecorderViewModel = hiltViewModel()
                        RecordingScreen(
                            viewModel = recorderViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
