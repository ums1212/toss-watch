package dev.comon.toss_watch.feature.setting.presentation.alarmimage

import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop
import kotlin.math.max

internal const val MAX_CROP_ZOOM = 5f

/**
 * 크롭 편집에서 사용자가 조정한 확대/이동 상태.
 *
 * @property zoom 틀을 꽉 채우는 기본 배율([cropBaseScale]) 대비 추가 확대 배율. 1 미만으로는 내려가지 않는다.
 * @property offsetX 이미지 중심이 틀 중심에서 벗어난 거리(px). [offsetY]도 같다.
 */
internal data class CropTransform(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

/** 한 변이 [frame]px인 정사각형 틀을 이미지가 빈틈없이 덮는 최소 배율 — 워치의 `ContentScale.Crop`과 같다. */
internal fun cropBaseScale(imageWidth: Int, imageHeight: Int, frame: Float): Float =
    max(frame / imageWidth, frame / imageHeight)

/** 드래그([panX], [panY])와 핀치([zoomChange])를 반영하되, 틀 안에 빈 곳이 생기지 않도록 제한한다. */
internal fun CropTransform.transformBy(
    panX: Float,
    panY: Float,
    zoomChange: Float,
    imageWidth: Int,
    imageHeight: Int,
    frame: Float,
): CropTransform {
    val newZoom = (zoom * zoomChange).coerceIn(1f, MAX_CROP_ZOOM)
    val applied = newZoom / zoom
    val scale = cropBaseScale(imageWidth, imageHeight, frame) * newZoom
    val maxX = ((imageWidth * scale - frame) / 2f).coerceAtLeast(0f)
    val maxY = ((imageHeight * scale - frame) / 2f).coerceAtLeast(0f)
    return CropTransform(
        zoom = newZoom,
        // 틀 중심을 기준으로 확대되도록 기존 이동량도 같은 비율로 늘린다.
        offsetX = (offsetX * applied + panX).coerceIn(-maxX, maxX),
        offsetY = (offsetY * applied + panY).coerceIn(-maxY, maxY),
    )
}

/** 현재 틀 안에 보이는 영역을 원본 이미지에 대한 비율로 환산한다. */
internal fun CropTransform.toCrop(imageWidth: Int, imageHeight: Int, frame: Float): AlarmImageCrop {
    val scale = cropBaseScale(imageWidth, imageHeight, frame) * zoom
    val width = imageWidth * scale
    val height = imageHeight * scale
    val left = ((width - frame) / 2f - offsetX) / width
    val top = ((height - frame) / 2f - offsetY) / height
    return AlarmImageCrop(left = left, top = top, right = left + frame / width, bottom = top + frame / height)
}
