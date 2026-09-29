package dev.comon.watch_app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.comon.watch_app.presentation.navigation.WatchNavHost
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import dev.comon.watch_app.diagnostics.StartupTiming

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val startedAt = StartupTiming.now()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TosswatchTheme {
                RequestNotificationPermission()
                WatchNavHost()
            }
        }
        StartupTiming.mark("activity.onCreate", startedAt)
    }
}

/** Android 13+(API 33)에서는 POST_NOTIFICATIONS가 런타임 권한이라, 미허용 상태면 로컬 알람 알림이 울리지 않는다. */
@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
