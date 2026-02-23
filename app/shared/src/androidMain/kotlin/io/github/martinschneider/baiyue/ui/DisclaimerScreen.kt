package io.github.martinschneider.baiyue.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.martinschneider.baiyue.shared.R

@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_round),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Disclaimer of Liability",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .drawWithContent {
                            drawContent()
                            if (scrollState.maxValue > 0) {
                                val barWidth = 4.dp.toPx()
                                val viewportH = size.height
                                val contentH = viewportH + scrollState.maxValue
                                val thumbH = (viewportH / contentH) * viewportH
                                val thumbTop = (scrollState.value.toFloat() / scrollState.maxValue) *
                                        (viewportH - thumbH)
                                drawRoundRect(
                                    color = scrollbarColor,
                                    topLeft = Offset(size.width - barWidth, thumbTop),
                                    size = Size(barWidth, thumbH),
                                    cornerRadius = CornerRadius(barWidth / 2)
                                )
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DisclaimerSection(
                            title = "Informational Use Only",
                            body = "This app provides reference data about Taiwan's hiking peaks. It is not a substitute for professional navigation equipment or guidance."
                        )
                        DisclaimerSection(
                            title = "Inherent Risks",
                            body = "Hiking and mountain activities involve serious risks including injury, death, and getting lost. Conditions can change rapidly and without warning."
                        )
                        DisclaimerSection(
                            title = "Data Accuracy",
                            body = "Trail data, peak locations, and route information may be inaccurate, incomplete, or out of date. Always verify conditions independently with official sources before setting out."
                        )
                        DisclaimerSection(
                            title = "Your Responsibility",
                            body = "You are solely responsible for your own safety. Inform others of your plans, carry appropriate equipment, and check weather forecasts before every hike."
                        )
                        DisclaimerSection(
                            title = "No Liability",
                            body = "The developer of this app accepts no responsibility or liability for any accidents, injuries, losses, or damages arising from use of this app or reliance on its data."
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it }
                    )
                    Text(
                        text = "I have read and accept this disclaimer",
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onAccept,
                    enabled = checked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun DisclaimerSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(text = body, fontSize = 14.sp)
    }
}
