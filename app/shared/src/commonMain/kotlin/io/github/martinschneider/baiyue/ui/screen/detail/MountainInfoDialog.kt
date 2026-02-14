package io.github.martinschneider.baiyue.ui.screen.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.MountainType
import io.github.martinschneider.baiyue.ui.theme.BaiyuePrimary
import io.github.martinschneider.baiyue.ui.theme.XiaobaiyuePrimary

@Composable
fun MountainInfoDialog(
    mountain: Mountain,
    isClimbed: Boolean,
    onDetails: () -> Unit,
    onDismiss: () -> Unit,
) {
    val idStr = mountain.displayId?.let { "#$it " } ?: ""
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "$idStr${mountain.chinese}",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mountain.english, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${mountain.elevationInt} m", fontSize = 14.sp)
                    val (typeLabel, typeColor) = when (mountain.type) {
                        MountainType.BAIYUE -> "百岳" to BaiyuePrimary
                        MountainType.XIAOBAIYUE -> "小百岳" to XiaobaiyuePrimary
                        MountainType.XIAOBAIYUE_OLD -> "舊小百岳" to XiaobaiyuePrimary
                    }
                    Text(
                        text = typeLabel,
                        fontSize = 12.sp,
                        color = typeColor
                    )
                    if (isClimbed) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Climbed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDetails) { Text("Details") }
                }
            }
        },
        confirmButton = {}
    )
}
