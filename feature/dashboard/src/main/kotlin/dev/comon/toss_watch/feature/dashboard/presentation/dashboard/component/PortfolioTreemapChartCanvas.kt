package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.comon.toss_watch.core.designsystem.theme.toss
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock

private const val CELL_GAP_DP = 3
private const val CELL_CORNER_RADIUS_DP = 8
private const val MIN_LABEL_WIDTH_DP = 44
private const val MIN_LABEL_HEIGHT_DP = 30
private const val MIN_SUBLABEL_HEIGHT_DP = 46
private const val MIN_WEIGHT_LABEL_HEIGHT_DP = 62

/** 픽셀 단위 사각형 — 트리맵 배치 계산은 좌표계 그대로 캔버스 픽셀 공간에서 이뤄진다. */
private data class TreemapRect(val x: Float, val y: Float, val width: Float, val height: Float)

/**
 * @param weight 포트폴리오 내 평가금액 비중(0~1). 전체화면 팝업의 비중 라벨에 쓰인다.
 */
private data class TreemapCell(val rect: TreemapRect, val holding: HoldingStock, val weight: Float)

/**
 * 보유 종목의 평가금액 비중을 사각형 넓이로 보여주는 트리맵 차트 순수 캔버스.
 *
 * "Squarified treemap" 알고리즘(Bruls, Huizing, van Wijk 1999)으로 배치한다 — 단순
 * slice-and-dice 방식과 달리 각 셀이 너무 가늘고 길어지지 않도록 매 행마다 정사각형에
 * 가까운 종횡비를 유지하며 채운다. 정확한 종횡비 최적화를 위해 실제 캔버스 픽셀 크기가
 * 필요하므로 [BoxWithConstraints]로 크기를 먼저 얻은 뒤 배치를 계산한다([PortfolioBubbleChartCanvas]는
 * 원형 패킹이라 정규화 좌표만으로 충분했지만, 트리맵은 컨테이너 종횡비에 따라 배치 결과가
 * 달라지기 때문).
 *
 * 카드 안 미리보기와 [PortfolioChartFullScreenDialog] 전체화면 뷰가 이 캔버스를 공유한다 —
 * [PortfolioBubbleChartCanvas]와 동일하게 카드/껍데기는 각 호출부가 책임지고, 이 컴포저블은
 * 그리기 로직만 담당한다.
 *
 * @param maxItems 상위 몇 종목까지 표시할지 — 카드에서는 [CARD_MAX_CHART_ITEMS], 전체화면에서는
 *   [EXPANDED_MAX_CHART_ITEMS]를 쓴다.
 * @param showWeightLabel true면 충분히 큰 셀에 티커/수익률 아래로 비중(%) 한 줄을 더 그린다.
 */
@Composable
internal fun PortfolioTreemapChartCanvas(
    holdings: List<HoldingStock>,
    modifier: Modifier = Modifier,
    maxItems: Int = CARD_MAX_CHART_ITEMS,
    showWeightLabel: Boolean = false,
) {
    if (holdings.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val riseColor = MaterialTheme.toss.rise
    val fallColor = MaterialTheme.toss.fall
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cells = remember(holdings, maxItems, widthPx, heightPx) {
            layoutTreemapCells(holdings, maxItems, widthPx, heightPx)
        }
        if (cells.isEmpty()) return@BoxWithConstraints

        Canvas(modifier = Modifier.fillMaxSize()) {
            val gap = CELL_GAP_DP.dp.toPx()
            val cornerRadius = CornerRadius(CELL_CORNER_RADIUS_DP.dp.toPx())
            val minLabelWidth = MIN_LABEL_WIDTH_DP.dp.toPx()
            val minLabelHeight = MIN_LABEL_HEIGHT_DP.dp.toPx()
            val minSublabelHeight = MIN_SUBLABEL_HEIGHT_DP.dp.toPx()
            val minWeightLabelHeight = MIN_WEIGHT_LABEL_HEIGHT_DP.dp.toPx()
            val labelPadding = 6.dp.toPx()

            cells.forEach { cell ->
                val color = when {
                    cell.holding.returnRate > 0 -> riseColor
                    cell.holding.returnRate < 0 -> fallColor
                    else -> neutralColor
                }
                val left = cell.rect.x + gap / 2f
                val top = cell.rect.y + gap / 2f
                val cellWidth = (cell.rect.width - gap).coerceAtLeast(0f)
                val cellHeight = (cell.rect.height - gap).coerceAtLeast(0f)
                val topLeft = Offset(left, top)
                val cellSize = Size(cellWidth, cellHeight)

                drawRoundRect(
                    color = color.copy(alpha = 0.16f),
                    topLeft = topLeft,
                    size = cellSize,
                    cornerRadius = cornerRadius,
                )
                drawRoundRect(
                    color = color,
                    topLeft = topLeft,
                    size = cellSize,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                if (cellWidth < minLabelWidth || cellHeight < minLabelHeight) return@forEach

                val tickerLayout = textMeasurer.measure(
                    text = cell.holding.stockCode,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                    ),
                    maxLines = 1,
                )
                val rateLayout = if (cellHeight >= minSublabelHeight) {
                    textMeasurer.measure(
                        text = formatRatePercent(cell.holding.returnRate),
                        style = TextStyle(fontSize = 9.sp, color = color),
                        maxLines = 1,
                    )
                } else {
                    null
                }
                val weightLayout = if (showWeightLabel && cellHeight >= minWeightLabelHeight) {
                    textMeasurer.measure(
                        text = "%.1f%%".format(cell.weight * 100),
                        style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant),
                        maxLines = 1,
                    )
                } else {
                    null
                }

                var y = top + labelPadding
                drawText(textLayoutResult = tickerLayout, topLeft = Offset(left + labelPadding, y))
                if (rateLayout != null) {
                    y += tickerLayout.size.height
                    drawText(textLayoutResult = rateLayout, topLeft = Offset(left + labelPadding, y))
                }
                if (weightLayout != null) {
                    y += rateLayout?.size?.height ?: 0
                    drawText(textLayoutResult = weightLayout, topLeft = Offset(left + labelPadding, y))
                }
            }
        }
    }
}

/**
 * 평가금액 상위 [maxItems]개 종목을 비중 기반 넓이로 squarified 배치한다.
 * 결과 좌표는 (0,0)~([widthPx],[heightPx]) 픽셀 공간 그대로다.
 */
private fun layoutTreemapCells(
    holdings: List<HoldingStock>,
    maxItems: Int,
    widthPx: Float,
    heightPx: Float,
): List<TreemapCell> {
    if (widthPx <= 0f || heightPx <= 0f) return emptyList()
    val totalAmount = holdings.sumOf { it.totalEvaluationAmount }
    if (totalAmount <= 0.0) return emptyList()

    val top = holdings.sortedByDescending { it.totalEvaluationAmount }.take(maxItems)
    val weights = top.map { (it.totalEvaluationAmount / totalAmount).toFloat() }
    val areas = weights.map { it * widthPx * heightPx }

    val rects = squarify(areas, TreemapRect(0f, 0f, widthPx, heightPx))
    return top.indices.map { index ->
        TreemapCell(rect = rects[index], holding = top[index], weight = weights[index])
    }
}

private fun formatRatePercent(rate: Double): String {
    val sign = if (rate >= 0) "+" else ""
    return "$sign${"%.1f".format(rate)}%"
}

/**
 * "Squarified treemap" 배치. [values]는 넓이 합이 [rect]의 넓이(width*height)와 같도록
 * 미리 정규화돼 있어야 하며, 결과 [TreemapRect] 리스트는 입력 순서를 그대로 유지한다.
 */
private fun squarify(values: List<Float>, rect: TreemapRect): List<TreemapRect> {
    if (values.isEmpty()) return emptyList()

    val results = mutableListOf<TreemapRect>()
    var remainingValues = values
    var remainingRect = rect

    while (remainingValues.isNotEmpty()) {
        var rowSize = 1
        while (rowSize < remainingValues.size) {
            val current = remainingValues.subList(0, rowSize)
            val next = remainingValues.subList(0, rowSize + 1)
            if (worstRatio(next, remainingRect) <= worstRatio(current, remainingRect)) {
                rowSize++
            } else {
                break
            }
        }
        val row = remainingValues.subList(0, rowSize)
        val (rowRects, nextRect) = layoutRow(row, remainingRect)
        results += rowRects
        remainingValues = remainingValues.subList(rowSize, remainingValues.size)
        remainingRect = nextRect
    }
    return results
}

/** 행에 [row]를 추가했을 때 가장 나쁜(정사각형에서 가장 먼) 종횡비 — squarify의 정지 조건 판단에 쓰인다. */
private fun worstRatio(row: List<Float>, rect: TreemapRect): Float {
    val side = minOf(rect.width, rect.height)
    if (side <= 0f) return Float.MAX_VALUE
    val sum = row.sum()
    if (sum <= 0f) return Float.MAX_VALUE
    val rowMax = row.max()
    val rowMin = row.min()
    val sideSq = side * side
    val sumSq = sum * sum
    return maxOf(sideSq * rowMax / sumSq, sumSq / (sideSq * rowMin))
}

/**
 * [row]를 [rect]의 짧은 변을 따라 배치하고, 남은 공간(나머지 행들이 채울 사각형)을 함께 반환한다.
 */
private fun layoutRow(row: List<Float>, rect: TreemapRect): Pair<List<TreemapRect>, TreemapRect> {
    val sum = row.sum()
    val rects = mutableListOf<TreemapRect>()

    return if (rect.width >= rect.height) {
        // 세로가 짧은 변 — 행을 왼쪽에 세로 열(column)로 쌓는다.
        val colWidth = if (rect.height > 0f) sum / rect.height else 0f
        var offsetY = rect.y
        for (value in row) {
            val h = if (colWidth > 0f) value / colWidth else 0f
            rects += TreemapRect(rect.x, offsetY, colWidth, h)
            offsetY += h
        }
        rects to TreemapRect(rect.x + colWidth, rect.y, rect.width - colWidth, rect.height)
    } else {
        // 가로가 짧은 변 — 행을 위쪽에 가로 줄(row)로 늘어놓는다.
        val rowHeight = if (rect.width > 0f) sum / rect.width else 0f
        var offsetX = rect.x
        for (value in row) {
            val w = if (rowHeight > 0f) value / rowHeight else 0f
            rects += TreemapRect(offsetX, rect.y, w, rowHeight)
            offsetX += w
        }
        rects to TreemapRect(rect.x, rect.y + rowHeight, rect.width, rect.height - rowHeight)
    }
}
