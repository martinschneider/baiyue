package io.github.martinschneider.baiyue.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.martinschneider.baiyue.ui.theme.BaiyueTheme
import java.io.File

class PhotoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_OSM_ID = "osm_id"
        const val RESULT_UPDATE = 1
        const val RESULT_DELETE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH) ?: run {
            finish()
            return
        }

        setContent {
            BaiyueTheme {
                val photoFile = File(photoPath)
                val imageRequest = ImageRequest.Builder(this@PhotoActivity)
                    .data(photoFile)
                    .memoryCacheKey("${photoPath}_${photoFile.lastModified()}")
                    .diskCacheKey("${photoPath}_${photoFile.lastModified()}")
                    .build()

                var showDeleteDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete photo?") },
                        text = { Text("This cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    setResult(RESULT_DELETE)
                                    finish()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .systemBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        OutlinedButton(
                            onClick = { finish() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.Transparent
                            ),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Text("Close")
                        }
                        Button(onClick = {
                            setResult(RESULT_UPDATE)
                            finish()
                        }) {
                            Text("Update")
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.Transparent
                            ),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
