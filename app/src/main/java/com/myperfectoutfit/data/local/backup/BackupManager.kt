package com.myperfectoutfit.data.local.backup

import android.content.Context
import android.net.Uri
import com.myperfectoutfit.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dbName = "wardrobe_database"

    suspend fun createBackupZip(): File? = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(context.cacheDir, "myperfectoutfit_backup_${System.currentTimeMillis()}.zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zos ->
                
                // 1. Respaldar Base de Datos
                val dbFile = context.getDatabasePath(dbName)
                val shmFile = File(dbFile.absolutePath + "-shm")
                val walFile = File(dbFile.absolutePath + "-wal")
                
                // Asegurarse de que los cambios en memoria se escriban al disco antes de copiar
                AppDatabase.closeInstance() 

                addToZip(zos, dbFile, "database/$dbName")
                if (shmFile.exists()) addToZip(zos, shmFile, "database/$dbName-shm")
                if (walFile.exists()) addToZip(zos, walFile, "database/$dbName-wal")

                // 2. Respaldar Imágenes (Carpeta files)
                val filesDir = context.filesDir
                filesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        addToZip(zos, file, "images/${file.name}")
                    }
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    suspend fun restoreFromZip(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = when {
                            entry.name.startsWith("database/") -> {
                                val name = entry.name.removePrefix("database/")
                                context.getDatabasePath(name)
                            }
                            entry.name.startsWith("images/") -> {
                                val name = entry.name.removePrefix("images/")
                                File(context.filesDir, name)
                            }
                            else -> null
                        }

                        outFile?.let {
                            it.parentFile?.mkdirs()
                            FileOutputStream(it).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
