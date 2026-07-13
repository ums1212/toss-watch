package dev.comon.toss_watch.feature.setting.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile

interface SettingRepository {

    suspend fun fetchAlarmProfiles(): NetworkResult<List<AlarmProfile>>

    suspend fun addAlarmProfile(
        stockCode: String,
        hour: Int,
        minute: Int,
    ): NetworkResult<AlarmProfile>

    suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile>

    suspend fun registerWatchToken(fcmToken: String): NetworkResult<Unit>
}
