package io.github.martinschneider.baiyue.data.backup

import io.github.martinschneider.baiyue.data.model.ClimbDate
import io.github.martinschneider.baiyue.data.repository.MountainRepository
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles backup/restore.
 *
 * Export: native ZIP format (photos as raw bytes).
 * Import: auto-detects native ZIP or legacy web JSON format.
 */
class BackupManager(
    private val repository: MountainRepository
) {
    /**
     * Import data from web-compatible JSON format, streaming from an InputStream.
     * Processes one key at a time to avoid loading the entire file into memory.
     */
    fun importWebJsonStreaming(
        inputStream: InputStream,
        savePhotoFromBase64: (Long, String) -> String?
    ): Boolean {
        return try {
            val reader = android.util.JsonReader(InputStreamReader(inputStream, "UTF-8"))
            var photosStr: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                when {
                    name == "climbed" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val osmIdStr = reader.nextName()
                            val isClimbed = reader.nextBoolean()
                            val osmId = osmIdStr.toLongOrNull()
                            if (osmId != null) {
                                repository.setClimbed(osmId, isClimbed)
                            }
                        }
                        reader.endObject()
                    }
                    name == "photos" -> {
                        photosStr = reader.nextString()
                    }
                    name.startsWith("photo_") -> {
                        val osmId = name.removePrefix("photo_").toLongOrNull()
                        if (osmId != null) {
                            val dataUri = reader.nextString()
                            val base64 = dataUri.removePrefix("data:image/jpeg;base64,")
                            val path = savePhotoFromBase64(osmId, base64)
                            if (path != null) {
                                repository.setPhotoPath(osmId, path)
                            }
                        } else {
                            reader.skipValue()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            reader.close()

            repository.loadClimbedStatus()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export data as a native ZIP backup, streaming directly to the given output.
     */
    // TODO: Add progress reporting callback for export (determinate: photo count known upfront)
    fun exportNativeZip(
        outputStream: OutputStream,
        readPhotoBytes: (String) -> ByteArray?
    ) {
        ZipOutputStream(outputStream).use { zip ->
            // manifest.json
            val manifest = buildJsonObject {
                put("version", 1)
                put("created", Instant.now().toString())
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(Json.encodeToString(JsonObject.serializer(), manifest).toByteArray())
            zip.closeEntry()

            // climbed.json
            val climbedData = buildJsonObject {
                repository.getAllClimbedWithDates().forEach { (osmId, climbed, date) ->
                    put(osmId.toString(), buildJsonObject {
                        put("climbed", climbed)
                        if (date != null) put("date", date)
                    })
                }
            }
            zip.putNextEntry(ZipEntry("climbed.json"))
            zip.write(Json.encodeToString(JsonObject.serializer(), climbedData).toByteArray())
            zip.closeEntry()

            // climb_dates.json
            val climbDatesData = buildJsonArray {
                repository.getAllClimbDatesMap().flatMap { (_, dates) -> dates }.forEach { climbDate ->
                    add(buildJsonObject {
                        put("osmId", climbDate.osmId)
                        put("date", climbDate.date)
                    })
                }
            }
            zip.putNextEntry(ZipEntry("climb_dates.json"))
            zip.write(Json.encodeToString(JsonArray.serializer(), climbDatesData).toByteArray())
            zip.closeEntry()

            // photos/
            repository.getAllPhotoEntries().forEach { (osmId, path) ->
                val bytes = readPhotoBytes(path)
                if (bytes != null) {
                    zip.putNextEntry(ZipEntry("photos/photo_$osmId.jpg"))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Import data from a native ZIP backup.
     */
    fun importNativeZip(
        inputStream: InputStream,
        savePhotoBytes: (Long, ByteArray) -> String?
    ): Boolean {
        return try {
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "climbed.json" -> {
                            val json = zip.readBytes().decodeToString()
                            val climbedObj = Json.parseToJsonElement(json).jsonObject
                            for ((key, value) in climbedObj) {
                                val osmId = key.toLongOrNull() ?: continue
                                val obj = value.jsonObject
                                val climbed = obj["climbed"]?.jsonPrimitive?.booleanOrNull ?: false
                                val date = obj["date"]?.jsonPrimitive?.contentOrNull
                                repository.setClimbed(osmId, climbed, date)
                            }
                        }
                        entry.name == "climb_dates.json" -> {
                            val json = zip.readBytes().decodeToString()
                            val datesArray = Json.parseToJsonElement(json).jsonArray
                            for (item in datesArray) {
                                val obj = item.jsonObject
                                val osmId = obj["osmId"]?.jsonPrimitive?.longOrNull ?: continue
                                val date = obj["date"]?.jsonPrimitive?.contentOrNull ?: continue
                                repository.addClimbDate(osmId, date)
                            }
                        }
                        entry.name.startsWith("photos/") && entry.name.endsWith(".jpg") -> {
                            val filename = entry.name.removePrefix("photos/")
                            val osmId = filename.removePrefix("photo_").removeSuffix(".jpg").toLongOrNull()
                            if (osmId != null) {
                                val bytes = zip.readBytes()
                                val path = savePhotoBytes(osmId, bytes)
                                if (path != null) {
                                    repository.setPhotoPath(osmId, path)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            repository.loadClimbedStatus()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Detect backup format by looking at the first bytes.
     * ZIP files start with PK (0x504B), JSON starts with '{'.
     */
    fun detectFormat(data: ByteArray): BackupFormat {
        if (data.size < 2) return BackupFormat.UNKNOWN
        return when {
            data[0] == 0x50.toByte() && data[1] == 0x4B.toByte() -> BackupFormat.NATIVE_ZIP
            data[0] == '{'.code.toByte() -> BackupFormat.WEB_JSON
            else -> BackupFormat.UNKNOWN
        }
    }
}

enum class BackupFormat {
    NATIVE_ZIP,
    WEB_JSON,
    UNKNOWN
}
