package dev.comon.watch_app.presentation.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.alarmsettings.AlarmSettingsScaffold
import dev.comon.watch_app.presentation.alarmsettings.SettingsButton

@Composable
fun WatchSettingsScreen(onPairingInfo: () -> Unit, onGenerateQr: () -> Unit) {
    AlarmSettingsScaffold(stringResource(R.string.watch_settings_title)) {
        item { SettingsButton(stringResource(R.string.watch_pairing_info), onClick = onPairingInfo) }
        item { SettingsButton(stringResource(R.string.onboarding_generate_qr), onClick = onGenerateQr) }
    }
}
