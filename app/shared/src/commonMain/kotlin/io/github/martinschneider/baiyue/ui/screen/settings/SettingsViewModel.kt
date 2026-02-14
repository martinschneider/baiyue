package io.github.martinschneider.baiyue.ui.screen.settings

import io.github.martinschneider.baiyue.data.backup.BackupFormat
import io.github.martinschneider.baiyue.data.backup.BackupManager
import io.github.martinschneider.baiyue.data.repository.MountainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(
    private val repository: MountainRepository,
    private val backupManager: BackupManager
) {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun exportNativeZip(outputStream: OutputStream, readPhotoBytes: (String) -> ByteArray?) {
        backupManager.exportNativeZip(outputStream, readPhotoBytes)
    }

    fun importFromStream(
        inputStream: InputStream,
        savePhotoFromBase64: (Long, String) -> String?,
        savePhotoBytes: (Long, ByteArray) -> String?
    ): Boolean {
        val buffered = BufferedInputStream(inputStream)
        buffered.mark(2)
        val header = ByteArray(2)
        val bytesRead = buffered.read(header)
        buffered.reset()

        if (bytesRead < 2) {
            _message.value = "Unknown backup format"
            return false
        }

        val format = backupManager.detectFormat(header)
        return when (format) {
            BackupFormat.WEB_JSON -> {
                backupManager.importWebJsonStreaming(buffered, savePhotoFromBase64)
            }
            BackupFormat.NATIVE_ZIP -> {
                backupManager.importNativeZip(buffered, savePhotoBytes)
            }
            BackupFormat.UNKNOWN -> {
                _message.value = "Unknown backup format"
                false
            }
        }
    }

    fun deleteAllData(deletePhotoFiles: () -> Unit) {
        deletePhotoFiles()
        repository.deleteAllData()
        _message.value = "All data deleted"
    }

    fun clearMessage() { _message.value = null }
}
