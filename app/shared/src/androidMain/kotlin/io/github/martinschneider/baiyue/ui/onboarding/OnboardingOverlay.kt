package io.github.martinschneider.baiyue.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class OnboardingStep(
    val title: String,
    val description: String,
    val targetBounds: Rect? = null,
    val route: String? = null
)

@Composable
fun OnboardingOverlay(
    steps: List<OnboardingStep>,
    currentStep: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentStep !in steps.indices) return
    val step = steps[currentStep]
    val isLastStep = currentStep == steps.lastIndex

    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )
    val pulseColor = MaterialTheme.colorScheme.primary

    // Show pulse for exactly one cycle then stop; resets whenever the step changes
    var showPulse by remember(currentStep) { mutableStateOf(step.targetBounds != null) }
    LaunchedEffect(currentStep) {
        if (step.targetBounds != null) {
            delay(1200)
            showPulse = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* intercept all touches */ }
    ) {
        // Nav-bar items sit in the bottom ~25 % of the screen; everything else
        // (e.g. the About button inside Settings) gets a tight rounded-rect spotlight.
        val isNavBarTarget = step.targetBounds != null &&
                step.targetBounds.center.y > constraints.maxHeight * 0.75f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.75f))

            step.targetBounds?.let { bounds ->
                if (isNavBarTarget) {
                    // Expose the full nav bar strip so no main-content pixels are revealed
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(0f, bounds.top),
                        size = Size(size.width, size.height - bounds.top),
                        blendMode = BlendMode.Clear
                    )
                } else {
                    // Tight rounded-rect cutout around the specific element
                    val pad = 12.dp.toPx()
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(bounds.left - pad, bounds.top - pad),
                        size = Size(bounds.width + pad * 2, bounds.height + pad * 2),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                }

                // Pulsing rings centred on the target, visible for ~2 s after step entry
                if (showPulse) {
                    for (phase in listOf(0f, 0.5f)) {
                        val p = (pulseProgress + phase) % 1f
                        val radius = 20.dp.toPx() + 36.dp.toPx() * p
                        val alpha = (1f - p) * 0.8f
                        if (alpha > 0.01f) {
                            drawCircle(
                                color = pulseColor.copy(alpha = alpha),
                                radius = radius,
                                center = bounds.center,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        val cardAlignment = when {
            step.targetBounds == null -> Alignment.Center
            step.targetBounds.center.y > constraints.maxHeight / 2f -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(
                    top = if (cardAlignment == Alignment.TopCenter) 32.dp else 0.dp,
                    bottom = if (cardAlignment == Alignment.BottomCenter) 100.dp else 0.dp
                ),
            contentAlignment = cardAlignment
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.indices.forEach { index ->
                            Surface(
                                modifier = Modifier.size(if (index == currentStep) 8.dp else 6.dp),
                                shape = CircleShape,
                                color = if (index == currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            ) {}
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            TextButton(onClick = onPrevious) { Text("Back") }
                        } else if (!isLastStep) {
                            TextButton(onClick = onSkip) { Text("Skip") }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Button(onClick = onNext) {
                            Text(if (isLastStep) "Get started!" else "Next")
                        }
                    }
                }
            }
        }
    }
}
