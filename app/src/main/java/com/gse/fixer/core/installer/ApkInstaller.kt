package com.gse.fixer.core.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gse.fixer.core.asset.BundledApks
import com.gse.fixer.core.downloader.GmsDownloader
import com.gse.fixer.core.enabler.ShizukuEnabler
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ApkInstaller(
    private val context: Context,
    private val bundledApks: BundledApks,
    private val gmsDownloader: GmsDownloader,
    private val shizukuEnabler: ShizukuEnabler,
    private val logger: SimpleLogger
) {
    suspend fun installIfNeeded(
        state: PackageState,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (!state.needsInstall) return@withContext true

        logger.i("Installer", "开始安装: ${state.label} (${state.packageName})")

        // 1. 获取 APK 文件
        val apkFile = when {
            state.apkAssetName != null -> {
                bundledApks.extractIfNeeded(state.apkAssetName!!)
            }
            state.downloadUrl != null -> {
                val dest = File(context.cacheDir, "gms/${state.packageName}.apk")
                if (gmsDownloader.downloadGms(state.downloadUrl!!, dest, onProgress)) dest else null
            }
            else -> null
        }

        apkFile?.let { file ->
            logger.i("Installer", "APK 准备就绪: ${file.length()} bytes")

            // 2. 尝试 Shizuku 静默安装
            if (shizukuEnabler.isShizukuAvailable()) {
                val success = shizukuEnabler.installApk(file)
                if (success) return@withContext true
                logger.w("Installer", "Shizuku 安装失败，回退到系统安装器")
            }

            // 3. 回退：FileProvider 启动系统安装页面
            installViaSystemInstaller(file)
        } ?: run {
            logger.e("Installer", "无法获取 APK 文件: ${state.packageName}")
            false
        }
    }

    private fun installViaSystemInstaller(apkFile: File): Boolean {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            // 通过 Context 启动 (需 Activity 上下文)
            logger.i("Installer", "已启动系统安装器: ${apkFile.name}")
            true
        } catch (e: Exception) {
            logger.e("Installer", "启动系统安装器失败", e)
            false
        }
    }
}