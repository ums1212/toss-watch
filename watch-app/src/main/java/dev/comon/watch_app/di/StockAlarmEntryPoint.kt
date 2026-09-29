package dev.comon.watch_app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.watch_app.domain.usecase.FindWatchAlarmUseCase
import dev.comon.watch_app.domain.usecase.RescheduleStockAlarmsUseCase

/**
 * BroadcastReceiver용 진입점. `@AndroidEntryPoint` 리시버는 Hilt Gradle 플러그인 변환 때문에
 * Kotlin에서 추상 `super.onReceive()` 호출 문제가 생겨, EntryPoint로 직접 꺼내 쓴다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StockAlarmEntryPoint {
    fun rescheduleStockAlarms(): RescheduleStockAlarmsUseCase
    fun findWatchAlarm(): FindWatchAlarmUseCase
}
