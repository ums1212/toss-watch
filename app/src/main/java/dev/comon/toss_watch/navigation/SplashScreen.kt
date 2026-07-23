package dev.comon.toss_watch.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.comon.toss_watch.R
import dev.comon.toss_watch.core.designsystem.component.BrandLogoCard
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/** 세션 판별(LOADING) 동안 표시되는 스플래시 — 브랜드 카드와 타이틀만 노출해 라우팅 깜빡임을 가린다. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TossSpacing.containerMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogoCard()

            Spacer(modifier = Modifier.height(TossSpacing.stackLg))

            Text(
                text = stringResource(id = R.string.splash_subtitle),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 페이지 인디케이터 도트 — 첫 번째만 활성(primary) 상태로 표시한다. */
@Composable
private fun SplashPageDots(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(3) { index ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(TossSpacing.stackSm))
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    TossWatchTheme {
        SplashScreen(modifier = Modifier.fillMaxWidth())
    }
}
