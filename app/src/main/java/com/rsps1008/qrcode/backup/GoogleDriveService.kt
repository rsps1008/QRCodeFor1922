package com.rsps1008.qrcode.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class GoogleDriveBackupFile(
    val content: ByteArray,
    val modifiedAtMillis: Long
)

@Suppress("DEPRECATION")
class GoogleDriveService(context: Context, account: GoogleSignInAccount) {
    private val drive: Drive

    init {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            setOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        drive = Drive.Builder(
            com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("QR Code Scanner").build()
    }

    suspend fun uploadBackup(
        fileName: String,
        content: ByteArray,
        mimeType: String = "application/json"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fileList = drive.files().list()
                .setQ("name='$fileName' and 'appDataFolder' in parents and trashed = false")
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .setOrderBy("modifiedTime desc")
                .execute()
            val mediaContent = ByteArrayContent(mimeType, content)

            if (fileList.files.isNullOrEmpty()) {
                val metadata = File().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                drive.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute()
            } else {
                drive.files().update(fileList.files.first().id, null, mediaContent)
                    .setFields("id")
                    .execute()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupWithModifiedTime(fileName: String): Result<GoogleDriveBackupFile?> =
        withContext(Dispatchers.IO) {
            try {
                val fileList = drive.files().list()
                    .setQ("name='$fileName' and 'appDataFolder' in parents and trashed = false")
                    .setSpaces("appDataFolder")
                    .setFields("files(id, name, modifiedTime)")
                    .setOrderBy("modifiedTime desc")
                    .execute()
                val file = fileList.files?.firstOrNull()
                    ?: return@withContext Result.success(null)
                val output = ByteArrayOutputStream()
                drive.files().get(file.id).executeMediaAndDownloadTo(output)
                Result.success(
                    GoogleDriveBackupFile(
                        content = output.toByteArray(),
                        modifiedAtMillis = file.modifiedTime?.value ?: 0L
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getBackupModifiedTime(fileName: String): Result<Long?> =
        withContext(Dispatchers.IO) {
            try {
                val fileList = drive.files().list()
                    .setQ("name='$fileName' and 'appDataFolder' in parents and trashed = false")
                    .setSpaces("appDataFolder")
                    .setFields("files(modifiedTime)")
                    .setOrderBy("modifiedTime desc")
                    .execute()
                Result.success(fileList.files?.firstOrNull()?.modifiedTime?.value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
