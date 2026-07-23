package com.mediacollector.app.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImage
import kotlin.math.sqrt

/**
 * 支持双指缩放、平移的图片组件
 *
 * 手势策略（关键）：
 * - **单指**且未缩放 → 不消费事件，放行给 HorizontalPager 做左右滑动
 * - **单指**且已缩放 → 消费事件，拖动平移图片
 * - **双指捏合** → 始终消费，缩放+平移
 * - 缩回 1x 自动归位
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        var prevDistance = 0f
                        var prevCentroid = Offset.Zero

                        firstDown.consume() // 至少需要一个 pointer，但不阻止后续传递

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val active = event.changes.filter { it.pressed }

                            when {
                                // ── 双指捏合 ──
                                active.size >= 2 -> {
                                    val p0 = active[0].position
                                    val p1 = active[1].position
                                    val distance = sqrt(
                                        (p0.x - p1.x) * (p0.x - p1.x) +
                                        (p0.y - p1.y) * (p0.y - p1.y)
                                    )
                                    val centroid = Offset(
                                        (p0.x + p1.x) / 2f,
                                        (p0.y + p1.y) / 2f
                                    )

                                    if (prevDistance > 0f) {
                                        val zoom = distance / prevDistance
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale
                                        if (newScale > 1f) {
                                            val extraX = centroid.x * (1 - zoom)
                                            val extraY = centroid.y * (1 - zoom)
                                            offsetX += extraX
                                            offsetY += extraY
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                    prevDistance = distance
                                    prevCentroid = centroid

                                    // 双指必须消费，阻止 HorizontalPager 滑动
                                    event.changes.forEach { it.consume() }
                                }

                                // ── 单指且已缩放 → 平移 ──
                                active.size == 1 && scale > 1f -> {
                                    val change = active[0]
                                    val pan = change.position - change.previousPosition
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    change.consume()
                                    prevDistance = 0f
                                }

                                // ── 单指未缩放 → 不消费，放行给 HorizontalPager ──
                                else -> {
                                    prevDistance = 0f
                                    // 不调用 consume，事件继续向上传递
                                }
                            }
                        } while (active.isNotEmpty())
                    }
                }
        )
    }
}
