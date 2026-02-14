package io.github.martinschneider.baiyue.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.ui.screen.detail.DetailViewModel
import java.io.File

internal fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open: $url", Toast.LENGTH_SHORT).show()
    }
}

internal fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Baiyue", text))
}

internal fun handlePhotoSelected(
    context: Context,
    uri: Uri,
    mountain: Mountain?,
    viewModel: DetailViewModel
) {
    mountain ?: return
    val osmId = mountain.osmId
    val photosDir = File(context.filesDir, "photos")
    photosDir.mkdirs()
    val photoFile = File(photosDir, "photo_$osmId.jpg")

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = android.graphics.BitmapFactory.decodeStream(input)
            val maxWidth = 1600
            val maxHeight = 1200
            val ratio = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height,
                1f
            )
            val scaledBitmap = if (ratio < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            photoFile.outputStream().use { output ->
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
            }

            viewModel.setPhotoPath(osmId, photoFile.absolutePath)

            if (scaledBitmap !== bitmap) scaledBitmap.recycle()
            bitmap.recycle()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
    }
}

internal fun openGpxFile(context: Context, gpxFilename: String) {
    try {
        val cacheFile = File(context.cacheDir, gpxFilename)
        context.assets.open("gpx/$gpxFilename").use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/gpx+xml")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, gpxFilename)
            cacheFile.copyTo(destFile, overwrite = true)
            Toast.makeText(context, "GPX saved to Downloads/$gpxFilename", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open GPX file", Toast.LENGTH_SHORT).show()
    }
}

internal fun launchDetailActivity(context: Context, osmId: Long) {
    val intent = Intent()
    intent.setClassName(context, "io.github.martinschneider.baiyue.ui.DetailActivity")
    intent.putExtra("osm_id", osmId)
    context.startActivity(intent)
}

internal fun navigateToMainActivity(context: Context) {
    val intent = Intent()
    intent.setClassName(context, "io.github.martinschneider.baiyue.MainActivity")
    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    context.startActivity(intent)
}
