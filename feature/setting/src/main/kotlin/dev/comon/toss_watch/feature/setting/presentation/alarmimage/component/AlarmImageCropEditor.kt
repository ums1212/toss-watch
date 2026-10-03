package dev.comon.toss_watch.feature.setting.presentation.alarmimage.component

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.component.TossWatchButton
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.feature.setting.R
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop
import dev.comon.toss_watch.feature.setting.presentation.alarmimage.CropTransform
import dev.comon.toss_watch.feature.setting.presentation.alarmimage.cropBaseScale
import dev.comon.toss_watch.feature.setting.presentation.alarmimage.toCrop
import dev.comon.toss_watch.feature.setting.presentation.alarmimage.transformBy
import kotlin.math.roundToInt

/**
 * 워치 화면과 같은 원형 틀 안에서 [source]를 드래그/핀치로 맞춘 뒤 적용하는 크롭 편집기.
 * 틀 안에 보이는 그대로가 워치 알람 이미지가 된다.
 */
@Composable
internal fun AlarmImageCropEditor(
    source: Bitmap,
    isSaving: Boolean,
    onConfirm: (AlarmImageCrop) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = remember(source) { source.asImageBitmap() }
    var transform by remember(source) { mutableStateOf(CropTransform()) }
    var frame by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TossSpacing.containerMargin, vertical = TossSpacing.stackMd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TossSpacing.stackMd),
    ) {
        Text(
            text = stringResource(id = R.string.alarmimage_crop_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Image(ContentScale.Crop)는 잘려 나간 부분을 자기 경계에서 클립해 버려 이동/확대해도
            // 가려진 영역이 드러나지 않는다 — 그래서 이미지 전체를 Canvas에 직접 그린다.
            Canvas(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .onSizeChanged { frame = it.width }
                    .pointerInput(source) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            transform = transform.transformBy(
                                panX = pan.x,
                                panY = pan.y,
                                zoomChange = zoom,
                                imageWidth = source.width,
                                imageHeight = source.height,
                                frame = size.width.toFloat(),
                            )
                        }
                    },
            ) {
                val scale = cropBaseScale(source.width, source.height, size.width) * transform.zoom
                val width = source.width * scale
                val height = source.height * scale
                drawImage(
                    image = image,
                    dstOffset = IntOffset(
                        ((size.width - width) / 2f + transform.offsetX).roundToInt(),
                        ((size.height - height) / 2f + transform.offsetY).roundToInt(),
                    ),
                    dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackSm)) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
            ) {
                Text(text = stringResource(id = R.string.alarmimage_crop_cancel))
            }
            TossWatchButton(
                text = stringResource(id = R.string.alarmimage_crop_apply),
                onClick = { onConfirm(transform.toCrop(source.width, source.height, frame.toFloat())) },
                // 틀 크기를 재기 전에는 크롭 영역을 계산할 수 없다.
                enabled = frame > 0,
                isLoading = isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
