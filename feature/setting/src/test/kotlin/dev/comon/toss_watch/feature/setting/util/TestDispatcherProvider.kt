package dev.comon.toss_watch.feature.setting.util

import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

/** 모든 디스패처를 단일 TestDispatcher로 통일해 가상 시간 제어를 단순화한다. */
class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
