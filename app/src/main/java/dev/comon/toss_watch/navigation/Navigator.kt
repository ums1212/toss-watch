package dev.comon.toss_watch.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dev.comon.toss_watch.core.model.navigation.AppRoute
import javax.inject.Inject

/**
 * Navigation 3 백스택의 단일 소유자.
 *
 * [ActivityRetainedScoped]라 화면 회전 등 구성 변경에도 백스택이 유지된다.
 * feature 모듈은 이 클래스를 모른다 — SideEffect 콜백을 통해서만 라우팅이 일어나
 * 화면 간 결합이 없다.
 *
 * 백스택은 빈 상태로 시작한다 — 시작 루트(Auth/Dashboard/TossKey)는 세션 판별이
 * 끝난 뒤 [dev.comon.toss_watch.navigation.MainViewModel]의 세션 스트림에 의해서만
 * 결정되며, 그 전까지는 TossWatchNavHost가 스플래시를 유지한다.
 */
@ActivityRetainedScoped
class Navigator @Inject constructor() {

    val backStack: SnapshotStateList<AppRoute> = mutableStateListOf()

    fun goTo(route: AppRoute) {
        backStack.add(route)
    }

    fun goBack() {
        // 루트 화면에서는 pop하지 않는다 — 시스템 back은 Activity finish로 처리된다.
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /** 인증 상태 전환(로그인/로그아웃) 시 백스택 전체를 새 루트로 교체한다. */
    fun setRoot(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }
}
