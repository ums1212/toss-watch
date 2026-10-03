package dev.comon.toss_watch.feature.setting.presentation.alarmimage

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmImageCropTransformTest {

    // 200x100 이미지를 100px 틀에 맞추면 기본 배율은 1 — 세로는 딱 맞고 가로는 좌우 50px씩 넘친다.
    private val width = 200
    private val height = 100
    private val frame = 100f

    @Test
    fun `초기 상태는 워치의 Crop과 같은 가운데 정사각형이다`() {
        val crop = CropTransform().toCrop(width, height, frame)

        assertEquals(0.25f, crop.left, DELTA)
        assertEquals(0f, crop.top, DELTA)
        assertEquals(0.75f, crop.right, DELTA)
        assertEquals(1f, crop.bottom, DELTA)
    }

    @Test
    fun `이동은 틀 안에 빈 곳이 생기지 않는 범위로 제한된다`() {
        val moved = CropTransform().transformBy(1_000f, 1_000f, 1f, width, height, frame)

        assertEquals(50f, moved.offsetX, DELTA)
        assertEquals(0f, moved.offsetY, DELTA)
        assertEquals(0f, moved.toCrop(width, height, frame).left, DELTA)
    }

    @Test
    fun `확대하면 잘라낼 영역이 같은 비율로 줄고 배율은 범위를 벗어나지 않는다`() {
        val zoomed = CropTransform().transformBy(0f, 0f, 2f, width, height, frame)
        val crop = zoomed.toCrop(width, height, frame)

        assertEquals(0.25f, crop.right - crop.left, DELTA)
        assertEquals(0.5f, crop.bottom - crop.top, DELTA)
        assertEquals(MAX_CROP_ZOOM, zoomed.transformBy(0f, 0f, 100f, width, height, frame).zoom, DELTA)
        assertEquals(1f, zoomed.transformBy(0f, 0f, 0.01f, width, height, frame).zoom, DELTA)
    }

    @Test
    fun `축소하면 넘친 이동량이 다시 범위 안으로 당겨진다`() {
        val zoomedAndMoved = CropTransform()
            .transformBy(0f, 0f, 2f, width, height, frame)
            .transformBy(0f, 1_000f, 1f, width, height, frame)
        assertEquals(50f, zoomedAndMoved.offsetY, DELTA)

        val restored = zoomedAndMoved.transformBy(0f, 0f, 0.5f, width, height, frame)
        assertEquals(0f, restored.offsetY, DELTA)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
