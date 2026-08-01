package dev.comon.toss_watch.feature.auth.util

import dev.comon.toss_watch.core.common.resources.StringProvider

/**
 * 테스트용 [StringProvider] 더블. 실제 문자열 대신 리소스 ID를 그대로 드러내
 * strings.xml 실제 문구를 몰라도 결정적으로 검증할 수 있게 한다.
 */
class FakeStringProvider : StringProvider {
    override fun getString(resId: Int): String = "res:$resId"

    override fun getString(resId: Int, vararg formatArgs: Any): String =
        "res:$resId:${formatArgs.joinToString()}"
}
