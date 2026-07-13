package dev.comon.toss_watch.core.common.coroutine

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 코루틴 디스패처 주입 계약.
 *
 * ViewModel/Repository가 [Dispatchers]를 직접 참조하지 않고 이 인터페이스에만
 * 의존하게 하여, 단위 테스트에서 TestDispatcher로 완전히 교체할 수 있도록 한다.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
