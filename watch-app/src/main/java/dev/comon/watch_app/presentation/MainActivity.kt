package dev.comon.watch_app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import dev.comon.watch_app.presentation.onboarding.OnboardingRoute
import dev.comon.watch_app.presentation.theme.TosswatchTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TosswatchTheme {
                OnboardingRoute()
            }
        }
    }
}
