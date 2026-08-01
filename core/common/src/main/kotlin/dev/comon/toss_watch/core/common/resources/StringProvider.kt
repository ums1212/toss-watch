package dev.comon.toss_watch.core.common.resources

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 문자열 리소스 조회 계약.
 *
 * ViewModel/Repository는 Composable이 아니라 `stringResource()`를 쓸 수 없으므로,
 * 에러/토스트 메시지처럼 UiState에 즉시 채워 넣어야 하는 문자열은 이 인터페이스를 통해
 * `res/values(-ko)/strings.xml`에서 조회한다. [DispatcherProvider]와 동일하게 실제 구현을
 * 추상화해 단위 테스트에서는 결정적인 가짜 구현으로 교체한다.
 */
interface StringProvider {
    fun getString(@StringRes resId: Int): String

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}

class DefaultStringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : StringProvider {

    override fun getString(resId: Int): String = context.getString(resId)

    override fun getString(resId: Int, vararg formatArgs: Any): String =
        context.getString(resId, *formatArgs)
}
