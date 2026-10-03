package dev.comon.toss_watch.feature.setting.presentation.alarmimage

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.designsystem.theme.adaptiveContentWidth
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.feature.setting.R
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImage
import dev.comon.toss_watch.feature.setting.presentation.alarmimage.component.AlarmImageCropEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PREVIEW_SIZE = 180.dp

/**
 * 워치 알람 이미지 설정 — 상승/보합/하락 이미지를 워치 화면과 같은 원형 틀로 보여주고,
 * 틀을 누르면 이미지 피커 → 크롭 편집을 거쳐 교체한다.
 *
 * @param onNavigateBack [AlarmImageUiSideEffect.NavigateBack] 수신 시 호출.
 */
@Composable
fun AlarmImageScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlarmImageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    AlarmImageUiSideEffect.NavigateBack -> onNavigateBack()
                    AlarmImageUiSideEffect.ShowLoadFailed ->
                        Toast.makeText(context, R.string.alarmimage_error_load, Toast.LENGTH_SHORT).show()
                    AlarmImageUiSideEffect.ShowSaveFailed ->
                        Toast.makeText(context, R.string.alarmimage_error_save, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 피커가 떠 있는 동안 프로세스가 재생성돼도 어느 슬롯을 고르던 중인지 잃지 않도록 저장한다.
    var pendingSlot by rememberSaveable { mutableStateOf<WatchAlarmImageSlot?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val slot = pendingSlot
        pendingSlot = null
        if (uri != null && slot != null) {
            viewModel.handleIntent(AlarmImageUiIntent.OnImagePicked(slot, uri.toString()))
        }
    }

    // 편집 중 시스템 뒤로가기는 화면을 닫지 않고 편집만 취소한다.
    BackHandler(enabled = uiState.editing != null) {
        viewModel.handleIntent(AlarmImageUiIntent.OnCropCancelled)
    }

    AlarmImageContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onPickImage = { slot ->
            pendingSlot = slot
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmImageContent(
    uiState: AlarmImageUiState,
    onIntent: (AlarmImageUiIntent) -> Unit,
    onPickImage: (WatchAlarmImageSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = uiState.editing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = if (editing != null) R.string.alarmimage_edit_top_bar_title else R.string.alarmimage_top_bar_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(AlarmImageUiIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.alarmimage_back_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (editing != null) {
                AlarmImageCropEditor(
                    source = editing.source,
                    isSaving = uiState.isSaving,
                    onConfirm = { crop -> onIntent(AlarmImageUiIntent.OnCropConfirmed(crop)) },
                    onCancel = { onIntent(AlarmImageUiIntent.OnCropCancelled) },
                    modifier = Modifier.adaptiveContentWidth(),
                )
            } else {
                AlarmImageList(uiState = uiState, onIntent = onIntent, onPickImage = onPickImage)
            }
        }
    }
}

@Composable
private fun AlarmImageList(
    uiState: AlarmImageUiState,
    onIntent: (AlarmImageUiIntent) -> Unit,
    onPickImage: (WatchAlarmImageSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .adaptiveContentWidth(),
        contentPadding = PaddingValues(
            horizontal = TossSpacing.containerMargin,
            vertical = TossSpacing.stackMd,
        ),
        verticalArrangement = Arrangement.spacedBy(TossSpacing.sectionPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hint") {
            Column(verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm)) {
                Text(
                    text = stringResource(id = R.string.alarmimage_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!uiState.isWatchPaired) {
                    Text(
                        text = stringResource(id = R.string.alarmimage_unpaired_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        items(items = uiState.images, key = { it.slot }) { image ->
            AlarmImageSlotItem(
                image = image,
                onPickImage = { onPickImage(image.slot) },
                onResetClicked = { onIntent(AlarmImageUiIntent.OnResetClicked(image.slot)) },
            )
        }
    }
}

@Composable
private fun AlarmImageSlotItem(
    image: AlarmImage,
    onPickImage: () -> Unit,
    onResetClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(id = image.slot.labelRes)
    val defaultPainter = painterResource(id = image.slot.defaultImageRes)
    // null = 사용자 이미지를 아직 디코딩 중. 읽지 못하면 워치와 마찬가지로 기본 이미지로 대체한다.
    val painter by produceState<Painter?>(
        initialValue = if (image.customPath == null) defaultPainter else null,
        image.customPath,
        image.updatedAt,
    ) {
        value = image.customPath
            ?.let { path -> withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) } }
            ?.let { BitmapPainter(it.asImageBitmap()) }
            ?: defaultPainter
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier
                .size(PREVIEW_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    onClickLabel = stringResource(id = R.string.alarmimage_change_action),
                    onClick = onPickImage,
                ),
        ) {
            painter?.let {
                Image(
                    painter = it,
                    contentDescription = stringResource(id = R.string.alarmimage_slot_image_desc, label),
                    // 워치 알람 화면(StockAlarmScreen의 ImageScene)과 같은 방식으로 원을 꽉 채운다.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        TextButton(onClick = onResetClicked, enabled = image.customPath != null) {
            Text(text = stringResource(id = R.string.alarmimage_reset_button))
        }
    }
}

@get:StringRes
private val WatchAlarmImageSlot.labelRes: Int
    get() = when (this) {
        WatchAlarmImageSlot.UP -> R.string.alarmimage_slot_up
        WatchAlarmImageSlot.FLAT -> R.string.alarmimage_slot_flat
        WatchAlarmImageSlot.DOWN -> R.string.alarmimage_slot_down
    }

/** 워치앱에 들어 있는 기본 알람 이미지와 같은 파일의 사본이다. */
@get:DrawableRes
private val WatchAlarmImageSlot.defaultImageRes: Int
    get() = when (this) {
        WatchAlarmImageSlot.UP -> R.drawable.stock_alarm_up
        WatchAlarmImageSlot.FLAT -> R.drawable.stock_alarm_flat
        WatchAlarmImageSlot.DOWN -> R.drawable.stock_alarm_down
    }

@Preview(showBackground = true)
@Composable
private fun AlarmImageContentPreview() {
    TossWatchTheme {
        AlarmImageContent(
            uiState = AlarmImageUiState(isWatchPaired = false),
            onIntent = {},
            onPickImage = {},
        )
    }
}
