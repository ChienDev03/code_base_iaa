package com.example.base.core.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.base.ui.theme.*
private val ActionMinHeight = 48.dp
/** Per-control debounce with an updated callback and monotonic clock. */
@Composable
fun rememberDebouncedClick(action: () -> Unit): () -> Unit {
    val current by rememberUpdatedState(action)
    var last by remember { mutableLongStateOf(Long.MIN_VALUE) }
    return remember { {
        val now = android.os.SystemClock.elapsedRealtime()
        if (last == Long.MIN_VALUE || now - last >= AppMotion.DebounceMillis) { last = now; current() }
    } }
}
/** Shared pressable action with a minimum touch target and configurable enabled state. */
@Composable
fun AppButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed) AppMotion.PressScale else 1f
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(AppShapes.medium)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f))
        .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, onClick = rememberDebouncedClick(onClick))
        .heightIn(min = ActionMinHeight).padding(AppSpacing.space16)) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, style = LocalAppTypography.current.body)
    }
}
/** Data refresh boundary; first-load state remains owned by presentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullToRefreshBox(refreshing: Boolean, onRefresh: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = rememberDebouncedClick(onRefresh), content = content)
}

private val SkeletonHeight = 48.dp
/** Static reduced-motion loading skeleton; never clears previously loaded content. */
@Composable
fun AppLoadingSkeleton(label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space08)) {
        Text(label, style = LocalAppTypography.current.body)
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(SkeletonHeight).clip(AppShapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}
/** Painter-based reusable image boundary; remote loading must use one bounded sized loader. */
@Composable
fun AppImage(painter: androidx.compose.ui.graphics.painter.Painter, description: String?, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(painter, description, modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Fit)
}
