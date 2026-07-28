package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * [PortfolioChartTypeSelector]/[PortfolioChartFullScreenDialog]/[PortfolioChartCard]에서
 * 전환 가능한 차트 종류. 새 차트 종류를 추가하려면 이 enum에 항목을 더하고, 각 캔버스
 * ([PortfolioBubbleChartCanvas], [PortfolioTreemapChartCanvas])와 소비처의 `when`을
 * 마저 채우면 된다 — [isAvailable]은 캔버스 구현이 아직 없는 항목을 선택기에는 노출하되
 * 비활성 상태로 자리만 잡아 두고 싶을 때 쓴다.
 */
internal enum class PortfolioChartType(
    val label: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
) {
    BUBBLE(label = "버블", icon = Icons.Filled.BubbleChart, isAvailable = true),
    TREEMAP(label = "트리맵", icon = Icons.Filled.GridView, isAvailable = true),
}

/** [PortfolioChartCard]/[PortfolioChartFullScreenDialog] 캡션에 쓰이는, 차트별 비중 표기 안내문. */
internal val PortfolioChartType.weightCaption: String
    get() = when (this) {
        PortfolioChartType.BUBBLE -> "원 크기는 포트폴리오 내 평가금액 비중을 나타내요"
        PortfolioChartType.TREEMAP -> "네모 크기는 포트폴리오 내 평가금액 비중을 나타내요"
    }
