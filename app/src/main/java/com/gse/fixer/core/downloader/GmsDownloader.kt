package com.gse.fixer.core.downloader

import android.content.Context
import com.gse.fixer.core.log.SimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File
import java.util.concurrent.TimeUnit
import org.koin.core.annotation.Inject

class GmsDownloader @Inject constructor(
    private val context: Context,
    private val logger: SimpleLogger
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun downloadGms(
        url: String,
        destFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        logger.i("Downloader", "开始下载 GMS: $url")
        destFile.parentFile?.mkdirs()

        val request = Request.Builder().url(url).build()
        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.e("Downloader", "下载失败: HTTP ${response.code}")
                    return@use false
                }

                val body = response.body
                val totalBytes = body?.contentLength() ?: -1L
                var downloaded = 0L

                body?.source()?.use { source ->
                    destFile.outputStream().buffer().use { sink ->
                        while (true) {
                            val read = source.read(sink.buffer, 8192)
                            if (read == -1L) break
                            downloaded += read
                            if (totalBytes > 0) {
                                onProgress(downloaded.toFloat() / totalBytes)
                            }
                        }
                        sink.flush()
                    }
                }

                logger.i("Downloader", "下载完成: ${destFile.length()} bytes")
                true
            }
        } catch (e: Exception) {
            logger.e("Downloader", "下载异常", e)
            false
        }
    }
}