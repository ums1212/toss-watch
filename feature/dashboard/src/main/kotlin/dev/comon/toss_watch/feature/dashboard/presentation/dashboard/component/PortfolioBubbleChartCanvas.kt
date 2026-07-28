package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.comon.toss_watch.core.designsystem.theme.toss
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** 카드 안에 작게 표시할 때 가져갈 최대 버블 수. */
internal const val CARD_MAX_BUBBLES = 7

/** 전체화면 팝업에서 표시할 최대 버블 수 — 더 넓은 캔버스라 더 많은 종목을 담을 수 있다. */
internal const val EXPANDED_MAX_BUBBLES = 12

private const val MIN_RADIUS = 0.16f
private const val MAX_RADIUS = 0.42f
private const val BUBBLE_PADDING = 0.02f
private const val SPIRAL_STEP = 0.05f
private const val MAX_PLACEMENT_ATTEMPTS = 600
private val GOLDEN_ANGLE = (Math.PI * (3.0 - sqrt(5.0))).toFloat()

/** 풍선처럼 위아래로 흔들리는 애니메이션 한 주기의 길이. */
private const val FLOAT_ANIMATION_DURATION_MS = 3400

/** 버블별 흔들림 위상 간격(라디안) — 버블마다 다른 위상을 줘서 동시에 움직이지 않게 한다. */
private const val PHASE_STEP = 0.9f

/**
 * 버블 차트 1개 원 — 좌표/반지름은 그리기 영역 절반을 1로 둔 정규화 값.
 *
 * @param weight 포트폴리오 내 평가금액 비중(0~1). 전체화면 팝업의 비중 라벨에 쓰인다.
 * @param phaseSeed 흔들림 애니메이션의 위상(라디안) 오프셋. 버블마다 달라 서로 어긋나게 움직인다.
 * @param floatSpeed 흔들림 속도 배수(정수). [FLOAT_ANIMATION_DURATION_MS] 주기가 매끄럽게
 *   반복되도록(끊김 없이) 정수 배수만 사용한다 — 정수가 아니면 한 주기가 끝나고 되돌아갈 때
 *   위상이 어긋나 순간적으로 튀어 보인다.
 */
private data class Bubble(
    val x: Float,
    val y: Float,
    val radius: Float,
    val holding: HoldingStock,
    val weight: Float,
    val phaseSeed: Float,
    val floatSpeed: Int,
)

/**
 * 보유 종목의 평가금액 비중을 원 크기로 보여주는 버블 차트("Market Performance") 순수 캔버스.
 *
 * 반지름은 비중(sqrt로 면적 비례 보정)에 따라 결정되며, 겹치지 않도록 나선형 탐색으로
 * 배치한다(물리 시뮬레이션 없는 근사 패킹 — 장식용 시각화 목적이라 충분함).
 *
 * 카드 안 미리보기와 [PortfolioChartFullScreenDialog] 전체화면 뷰가 이 캔버스를 공유한다 —
 * 카드/껍데기(배경, 캡션 등)는 각 호출부가 책임지고, 이 컴포저블은 그리기 로직만 담당한다.
 *
 * 계좌에 KRW/USD 종목이 섞여 있으면 환율 환산 없이 평가금액을 그대로 합산해 비중을
 * 계산하므로 다소 왜곡될 수 있다. 대부분 계좌가 단일 통화라 실용상 허용한다.
 *
 * @param maxBubbles 상위 몇 종목까지 표시할지 — 카드에서는 [CARD_MAX_BUBBLES], 전체화면에서는
 *   [EXPANDED_MAX_BUBBLES]를 쓴다.
 * @param showWeightLabel true면 충분히 큰 버블에 티커/수익률 아래로 비중(%) 한 줄을 더 그린다 —
 *   전체화면처럼 캔버스가 넓어 정보 밀도를 더 가져갈 수 있을 때만 켠다.
 */
@Composable
internal fun PortfolioBubbleChartCanvas(
    holdings: List<HoldingStock>,
    modifier: Modifier = Modifier,
    maxBubbles: Int = CARD_MAX_BUBBLES,
    showWeightLabel: Boolean = false,
) {
    if (holdings.isEmpty()) return

    val bubbles = remember(holdings, maxBubbles) { layoutBubbles(holdings, maxBubbles) }
    if (bubbles.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val riseColor = MaterialTheme.toss.rise
    val fallColor = MaterialTheme.toss.fall
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Canvas의 draw 람다 안에서 .value를 읽으므로 매 프레임 값이 바뀌어도
    // 리컴포지션이 아니라 다시 그리기만 발생한다(by 위임으로 구조분해하면
    // 이 컴포저블 전체가 매 프레임 리컴포지션된다).
    val floatPhaseState = rememberInfiniteTransition(label = "bubbleFloat").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FLOAT_ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bubbleFloatPhase",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val scale = minOf(size.width, size.height) / 2f
        val floatPhase = floatPhaseState.value

        bubbles.forEach { bubble ->
            val color = when {
                bubble.holding.returnRate > 0 -> riseColor
                bubble.holding.returnRate < 0 -> fallColor
                else -> neutralColor
            }
            val radiusPx = bubble.radius * scale
            val bobAmplitudePx = (radiusPx * 0.14f).coerceIn(3.dp.toPx(), 9.dp.toPx())
            val bobOffsetY = sin(floatPhase * bubble.floatSpeed + bubble.phaseSeed) * bobAmplitudePx
            val bubbleCenter = Offset(
                x = center.x + bubble.x * scale,
                y = center.y + bubble.y * scale + bobOffsetY,
            )

            drawCircle(color = color.copy(alpha = 0.14f), radius = radiusPx, center = bubbleCenter)
            drawCircle(
                color = color,
                radius = radiusPx,
                center = bubbleCenter,
                style = Stroke(width = 1.5.dp.toPx()),
            )

            val showSublabel = radiusPx >= 26.dp.toPx()
            val showWeightSublabel = showWeightLabel && radiusPx >= 34.dp.toPx()
            if (radiusPx >= 18.dp.toPx()) {
                val tickerLayout = textMeasurer.measure(
                    text = bubble.holding.stockCode,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                    ),
                )
                val rateLayout = if (showSublabel) {
                    textMeasurer.measure(
                        text = formatRatePercent(bubble.holding.returnRate),
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = color,
                            textAlign = TextAlign.Center,
                        ),
                    )
                } else {
                    null
                }
                val weightLayout = if (showWeightSublabel) {
                    textMeasurer.measure(
                        text = "%.1f%%".format(bubble.weight * 100),
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        ),
                    )
                } else {
                    null
                }

                val blockHeight = tickerLayout.size.height +
                    (rateLayout?.size?.height ?: 0) +
                    (weightLayout?.size?.height ?: 0)
                var y = bubbleCenter.y - blockHeight / 2f
                drawText(
                    textLayoutResult = tickerLayout,
                    topLeft = Offset(bubbleCenter.x - tickerLayout.size.width / 2f, y),
                )
                if (rateLayout != null) {
                    y += tickerLayout.size.height
                    drawText(
                        textLayoutResult = rateLayout,
                        topLeft = Offset(bubbleCenter.x - rateLayout.size.width / 2f, y),
                    )
                }
                if (weightLayout != null) {
                    y += rateLayout?.size?.height ?: 0
                    drawText(
                        textLayoutResult = weightLayout,
                        topLeft = Offset(bubbleCenter.x - weightLayout.size.width / 2f, y),
                    )
                }
            }
        }
    }
}

/** 평가금액 상위 [maxBubbles]개 종목을 비중 기반 원으로 나선형 탐색 배치한다. */
private fun layoutBubbles(holdings: List<HoldingStock>, maxBubbles: Int): List<Bubble> {
    val totalAmount = holdings.sumOf { it.totalEvaluationAmount }
    if (totalAmount <= 0.0) return emptyList()

    val top = holdings.sortedByDescending { it.totalEvaluationAmount }.take(maxBubbles)
    val weights = top.map { (it.totalEvaluationAmount / totalAmount).toFloat() }
    val rawRadii = weights.map { sqrt(it) }
    val maxRaw = rawRadii.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val radii = rawRadii.map { raw -> MIN_RADIUS + (raw / maxRaw) * (MAX_RADIUS - MIN_RADIUS) }

    val placed = mutableListOf<Bubble>()
    top.forEachIndexed { index, holding ->
        val radius = radii[index]
        // 위상은 버블마다 어긋나게, 속도는 정수 배수(1배/2배)로 번갈아 줘서 애니메이션이
        // 한 주기 끝에서 다시 시작될 때 튀지 않으면서도 자연스럽게 제각각 흔들리게 한다.
        val phaseSeed = index * PHASE_STEP
        val floatSpeed = 1 + (index % 2)

        if (placed.isEmpty()) {
            placed += Bubble(0f, 0f, radius, holding, weights[index], phaseSeed, floatSpeed)
            return@forEachIndexed
        }

        var point = Offset.Zero
        for (attempt in 0 until MAX_PLACEMENT_ATTEMPTS) {
            val angle = attempt * GOLDEN_ANGLE
            val r = SPIRAL_STEP * sqrt(attempt.toFloat())
            val candidate = Offset(r * cos(angle), r * sin(angle))
            val fits = placed.all { existing ->
                hypot((candidate.x - existing.x).toDouble(), (candidate.y - existing.y).toDouble()) >=
                    (existing.radius + radius + BUBBLE_PADDING)
            }
            if (fits) {
                point = candidate
                break
            }
        }
        placed += Bubble(point.x, point.y, radius, holding, weights[index], phaseSeed, floatSpeed)
    }
    return placed
}

private fun formatRatePercent(rate: Double): String {
    val sign = if (rate >= 0) "+" else ""
    return "$sign${"%.1f".format(rate)}%"
}

/** 프리뷰 전용 샘플 데이터 — 카드/전체화면 프리뷰가 공유한다. */
internal fun sampleHoldings(): List<HoldingStock> = listOf(
    HoldingStock(
        stockCode = "NVDA", stockName = "Nvidia Corp", currency = Currency.USD,
        quantity = 10.0, averageBuyPrice = 700.0, totalBuyAmount = 7_000.0,
        currentPrice = 875.28, totalEvaluationAmount = 8_752.8, profitLoss = 1_752.8, returnRate = 5.7,
    ),
    HoldingStock(
        stockCode = "TSLA", stockName = "Tesla Inc", currency = Currency.USD,
        quantity = 20.0, averageBuyPrice = 180.0, totalBuyAmount = 3_600.0,
        currentPrice = 175.40, totalEvaluationAmount = 3_508.0, profitLoss = -92.0, returnRate = -3.1,
    ),
    HoldingStock(
        stockCode = "AAPL", stockName = "Apple Inc", currency = Currency.USD,
        quantity = 15.0, averageBuyPrice = 178.0, totalBuyAmount = 2_670.0,
        currentPrice = 182.52, totalEvaluationAmount = 2_737.8, profitLoss = 67.8, returnRate = 2.4,
    ),
    HoldingStock(
        stockCode = "MSFT", stockName = "Microsoft Corp", currency = Currency.USD,
        quantity = 6.0, averageBuyPrice = 408.0, totalBuyAmount = 2_448.0,
        currentPrice = 415.50, totalEvaluationAmount = 2_493.0, profitLoss = 45.0, returnRate = 1.84,
    ),
    HoldingStock(
        stockCode = "GOOGL", stockName = "Alphabet Inc", currency = Currency.USD,
        quantity = 10.0, averageBuyPrice = 165.0, totalBuyAmount = 1_650.0,
        currentPrice = 168.9, totalEvaluationAmount = 1_689.0, profitLoss = 39.0, returnRate = 2.4,
    ),
    HoldingStock(
        stockCode = "AMZN", stockName = "Amazon.com Inc", currency = Currency.USD,
        quantity = 8.0, averageBuyPrice = 180.0, totalBuyAmount = 1_440.0,
        currentPrice = 177.8, totalEvaluationAmount = 1_422.4, profitLoss = -17.6, returnRate = -1.2,
    ),
)

/** 프리뷰 전용 단일 종목 샘플. */
internal fun sampleSingleHolding(): List<HoldingStock> = listOf(
    HoldingStock(
        stockCode = "005930", stockName = "삼성전자", currency = Currency.KRW,
        quantity = 10.0, averageBuyPrice = 65_000.0, totalBuyAmount = 650_000.0,
        currentPrice = 72_500.0, totalEvaluationAmount = 725_000.0, profitLoss = 75_000.0, returnRate = 11.54,
    ),
)
