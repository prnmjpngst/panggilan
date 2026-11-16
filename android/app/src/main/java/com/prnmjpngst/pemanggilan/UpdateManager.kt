package com.prnmjpngst.pemanggilan

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class ReleaseInfo(val tag: String, val apkUrl: String, val fileName: String)

object UpdateManager {
    private val client = OkHttpClient()

    suspend fun checkLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/prnmjpngst/panggilan/releases/latest")
            .header("User-Agent", "Pemanggilan-App")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body!!.string())
            val tag = json.optString("tag_name")
            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name.endsWith(".apk")) {
                    return@withContext ReleaseInfo(tag, a.optString("browser_download_url"), name)
                }
            }
            null
        }
    }

    suspend fun downloadApk(context: Context, url: String, fileName: String, onProgress: (Int) -> Unit = {}): File =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).header("User-Agent", "Pemanggilan-App").build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body!!
                val total = body.contentLength()
                val outFile = File(context.cacheDir, fileName)
                FileOutputStream(outFile).use { fos ->
                    val buf = ByteArray(8192)
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            fos.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                        }
                    }
                }
                outFile
            }
        }

    fun canInstall(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getLastTag(context: Context): String? =
        context.getSharedPreferences("update", Context.MODE_PRIVATE).getString("last_tag", null)

    fun setLastTag(context: Context, tag: String) {
        context.getSharedPreferences("update", Context.MODE_PRIVATE).edit().putString("last_tag", tag).apply()
    }
}