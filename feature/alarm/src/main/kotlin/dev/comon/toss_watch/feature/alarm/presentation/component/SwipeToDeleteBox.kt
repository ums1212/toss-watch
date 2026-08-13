package dev.comon.toss_watch.feature.alarm.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing

/**
 * 알림 프로필 행을 왼쪽(End → Start)으로 슬라이드해 삭제를 트리거할 수 있게 감싸는 컨테이너.
 * 스와이프가 끝까지 도달해도 [content]를 즉시 목록에서 제거하지 않고 [onDelete]만 호출한다 —
 * 실제 삭제 여부는 호출부의 확인 다이얼로그 → UseCase 흐름에서 결정되므로, 다이얼로그가 취소되든
 * 확정되든 항목은 [SwipeToDismissBoxState.reset]으로 항상 원래 자리로 되돌아온다.
 * (`confirmValueChange`로 상태 변화를 막는 방식은 Material3에서 deprecated되어,
 * `onDismiss` 콜백 이후 수동으로 되돌리는 현재 권장 패턴을 사용한다.)
 *
 * @param enabled `false`면 스와이프 제스처 자체를 막는다 (예: 저장 중일 때).
 */
@Composable
internal fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.settledValue) {
        if (dismissState.settledValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        gesturesEnabled = enabled,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = TossSpacing.stackMd),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            content()
        }
    }
}
