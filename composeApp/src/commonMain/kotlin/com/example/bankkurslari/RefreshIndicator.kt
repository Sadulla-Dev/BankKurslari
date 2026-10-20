package com.example.bankkurslari

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsRoseMark
import kotlin.math.PI
import kotlin.math.sin

private const val THRESHOLD_DP = 80f

/** How far the whole screen yields to the pull: the content steps down and makes room for the indicator. */
val RefreshShift = 60.dp

/** Same distance the pull needs before it counts as a refresh (Material default, kept explicit). */
val RefreshThreshold = THRESHOLD_DP.dp

/**
 * Pull-to-refresh in the app's own language: the bank-comparison bars from the list.
 * Pulling draws them one by one, the top (best) bar in teal. While loading the others keep
 * re-measuring against it, because that is the job: compare everyone, land on the best.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.BestBarsIndicator(state: PullToRefreshState, refreshing: Boolean, topInset: Dp) {
    // The clock only runs while loading; an idle indicator must not keep the frame loop alive.
    val scan = if (refreshing) rememberInfiniteTransition(label = "scan").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    ) else null
    val rest = VsRoseMark
    val best = VsEmerald
    val pillH = 40.dp
    // A window that starts right under the system bars: the pill slides out of its top edge and
    // never draws over the status bar, notch or clock, whatever the inset is.
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset(y = topInset)
            .size(width = 112.dp, height = 104.dp)
            .clipToBounds()
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    val gap = state.distanceFraction.coerceIn(0f, 1.25f) * RefreshShift.toPx()
                    translationY = gap - (pillH + 6.dp).toPx()
                    alpha = if (refreshing) 1f else (state.distanceFraction * 2f).coerceIn(0f, 1f)
                }
                .size(width = 64.dp, height = pillH)
                .vsFloating(VsShape.Pill)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val p = state.distanceFraction
                val phase = scan?.value ?: 0f
                val barH = 4.dp.toPx()
                val gap = 3.dp.toPx()
                val full = 28.dp.toPx()
                val base = floatArrayOf(1f, 0.74f, 0.52f, 0.34f)
                val top = (size.height - (barH * base.size + gap * (base.size - 1))) / 2f
                val left = (size.width - full) / 2f
                base.forEachIndexed { i, b ->
                    var len = b * ((p - i * 0.1f) / 0.55f).coerceIn(0f, 1f)
                    if (refreshing && i > 0) {
                        val s = 0.5f + 0.5f * sin(2f * PI.toFloat() * (phase - i * 0.21f))
                        len = b * (0.3f + 0.7f * s)
                    }
                    if (len > 0f) drawRoundRect(
                        color = if (i == 0) best else rest,
                        topLeft = Offset(left, top + i * (barH + gap)),
                        size = Size(full * len, barH),
                        cornerRadius = CornerRadius(barH / 2f)
                    )
                }
            }
        }
    }
}

/**
 * One choreography for the whole screen, in three beats:
 *  pull    : the content yields, the comparison bars retract (nothing is settled while you pull);
 *  measure : while data loads, numbers dim and every bar re-measures against the best one;
 *  settle  : new numbers land, bars sweep back in from the top down, rows that moved flash once.
 * Read [pull] and [settle] only in draw/graphicsLayer lambdas so the frame loop stays out of composition.
 */
@Stable
class RefreshFx(val pull: () -> Float, val measuring: Boolean, val settle: State<Float>) {
    companion object {
        val Rest = RefreshFx({ 0f }, false, object : State<Float> { override val value = 1f })
    }
}

internal fun easeOut(x: Float): Float { val k = 1f - x.coerceIn(0f, 1f); return 1f - k * k * k }

/** The row's gap-to-best bar. [frac] is its resting length (0..1 of the track). */
@Composable
fun CompareBar(frac: Float, index: Int, fx: RefreshFx, modifier: Modifier = Modifier) {
    val phase = if (fx.measuring) rememberInfiniteTransition(label = "rowscan").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "rowphase"
    ) else null
    val track = com.example.bankkurslari.ui.theme.VsGround
    val fill = VsRoseMark
    Canvas(modifier.size(width = 120.dp, height = 4.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(track, size = size, cornerRadius = r)
        val len = if (phase != null) {
            val s = 0.5f + 0.5f * sin(2f * PI.toFloat() * (phase.value - index * 0.13f))
            0.25f + 0.75f * s
        } else {
            val ix = index.coerceAtMost(10)
            val sweep = easeOut(fx.settle.value * (1f + 0.08f * 10) - ix * 0.08f)
            sweep * (1f - 0.7f * fx.pull().coerceIn(0f, 1f))
        }
        val l = frac * len
        if (frac > 0f && l > 0f) drawRoundRect(fill, size = Size(size.width * l, size.height), cornerRadius = r)
    }
}
