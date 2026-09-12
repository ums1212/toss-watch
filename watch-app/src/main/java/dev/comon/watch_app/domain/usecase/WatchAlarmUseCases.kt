package dev.comon.watch_app.domain.usecase

import dev.comon.toss_watch.core.model.watch.WatchAlarmRequest
import dev.comon.watch_app.domain.repository.WatchAlarmRepository
import javax.inject.Inject

class ObserveWatchAlarmsUseCase @Inject constructor(private val repository: WatchAlarmRepository) {
    operator fun invoke() = repository.observe()
}

class RefreshWatchAlarmsUseCase @Inject constructor(private val repository: WatchAlarmRepository) {
    suspend operator fun invoke() = repository.refresh()
}

class SaveWatchAlarmUseCase @Inject constructor(private val repository: WatchAlarmRepository) {
    suspend operator fun invoke(request: WatchAlarmRequest) = repository.submit(request)
}
