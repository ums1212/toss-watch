package dev.comon.toss_watch.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.comon.toss_watch.core.designsystem.R

/**
 * Google Fonts Provider(Play 서비스) 설정.
 * 서명 인증서는 [R.array.com_google_android_gms_fonts_certs] (`font_certs.xml`) 참조.
 */
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val InterGoogleFont = GoogleFont("Inter")

/**
 * DESIGN.md가 지정한 브랜드 서체. Play 서비스를 통해 런타임에 다운로드된다.
 * (Play 서비스 미탑재 기기/오프라인 프리뷰에서는 시스템 폰트로 자동 폴백)
 */
val InterFontFamily = FontFamily(
    Font(googleFont = InterGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
)

/**
 * Toss Watch 공용 타이포그래피 스케일.
 * DESIGN.md `typography` 토큰을 Material 3 슬롯에 매핑했다.
 *
 * | M3 슬롯 | DESIGN 토큰 |
 * |---|---|
 * | displayLarge | display-lg |
 * | headlineLarge | headline-lg |
 * | headlineMedium | headline-lg-mobile |
 * | titleLarge | headline-md |
 * | bodyLarge | body-lg |
 * | bodyMedium | body-md |
 * | labelMedium | label-md |
 *
 * `numeric-data`는 M3 슬롯이 없어 [TossNumericStyle]로 별도 노출한다.
 */
val TossWatchTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em,
    ),
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
    ),
)

/**
 * DESIGN.md `numeric-data` 토큰. 주가 등 금액 표시 전용 스타일로,
 * M3 [Typography] 슬롯에 대응하는 항목이 없어 별도로 노출한다.
 * 리스트에서 세로 정렬이 맞도록 tabular(고정폭) 숫자를 사용한다.
 */
val TossNumericStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.01).em,
    fontFeatureSettings = "tnum",
)
