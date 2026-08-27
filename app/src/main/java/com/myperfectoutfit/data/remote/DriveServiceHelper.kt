package com.myperfectoutfit.data.remote

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile

class DriveServiceHelper(private val driveService: Drive) {

    suspend fun uploadBackupFile(javaFile: JavaFile): String? = withContext(Dispatchers.IO) {
        try {
            val metadata = File()
                .setName(javaFile.name)
                .setMimeType("application/zip")
                // appDataFolder es una carpeta privada de la app en el Drive del usuario
                .setParents(listOf("appDataFolder"))

            val content = FileContent("application/zip", javaFile)
            val file = driveService.files().create(metadata, content).execute()
            file.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadLatestBackup(targetFile: JavaFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime)")
                .execute()

            val latestFile = result.files.maxByOrNull { it.createdTime.value } ?: return@withContext false
            
            driveService.files().get(latestFile.id).executeMediaAndDownloadTo(targetFile.outputStream())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
