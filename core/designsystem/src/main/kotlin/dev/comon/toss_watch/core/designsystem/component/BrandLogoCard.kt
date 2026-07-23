package dev.comon.toss_watch.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.R
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * 스플래시/로그인 등 브랜드 최초 노출 화면에서 공통으로 쓰는 로고 카드.
 * 크기만 화면별로 다르고, 색상·그림자·모양은 항상 동일하게 유지한다.
 */
@Composable
fun BrandLogoCard(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 12.dp,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_toss_watch_brand),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // adaptive icon 전경 리소스는 세이프존 여백이 포함돼 있어
            // 카드 전체를 채우려면 확대해 그 여백을 잘라내야 한다.
            modifier = Modifier
                .fillMaxSize()
                .scale(1.5f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrandLogoCardPreview() {
    TossWatchTheme {
        BrandLogoCard()
    }
}

@Preview(showBackground = true)
@Composable
private fun BrandLogoCardSmallPreview() {
    TossWatchTheme {
        BrandLogoCard(size = 96.dp)
    }
}
