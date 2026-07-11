package com.gse.fixer.core.asset

import android.content.Context
import com.gse.fixer.core.log.SimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BundledApks(
    private val context: Context,
    private val logger: SimpleLogger
) {
    private val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    private val baseDir = "gms/$abi/"

    suspend fun extractIfNeeded(assetName: String): File? = withContext(Dispatchers.IO) {
        val dest = File(context.cacheDir, "gms/$assetName")
        if (dest.exists() && dest.length() > 0) {
            logger.d("Asset", "APK 已存在: $dest (${dest.length()} bytes)")
            return@withContext dest
        }

        val assetPath = baseDir + assetName
        logger.i("Asset", "释放 APK: $assetPath -> $dest")

        try {
            context.assets.open(assetPath).use { input ->
                dest.parentFile?.mkdirs()
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0) {
                logger.i("Asset", "释放成功: ${dest.name} (${dest.length()} bytes)")
                dest
            } else {
                logger.e("Asset", "释放失败: 文件为空")
                null
            }
        } catch (e: Exception) {
            logger.e("Asset", "释放异常: $assetPath", e)
            null
        }
    }

    fun getCachedApk(assetName: String): File? {
        val dest = File(context.cacheDir, "gms/$assetName")
        return if (dest.exists() && dest.length() > 0) dest else null
    }

    fun clearCache() {
        File(context.cacheDir, "gms").deleteRecursively()
    }
}