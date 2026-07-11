package com.gse.fixer.core.enabler

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.ShizukuManager
import moe.shizuku.api.ShizukuApi
import org.koin.core.annotation.Inject
import org.koin.core.annotation.Single

@Single
class ShizukuEnabler @Inject constructor(
    private val context: Context,
    private val logger: SimpleLogger
) {
    fun isShizukuAvailable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return ShizukuManager.checkSelfPermission(context) == PackageManager.PERMISSION_GRANTED
        }
        return ShizukuManager.getService() != null
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ShizukuManager.requestPermission(context)
        } else {
            ShizukuManager.getInstance(context).requestPermission()
        }
    }

    suspend fun enablePackage(state: PackageState): Boolean = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            logger.w("ShizukuEnabler", "Shizuku 不可用，无法启用 ${state.label}")
            return@withContext false
        }

        val pkg = state.packageName
        val currentState = state.status
        
        return@withContext when (currentState) {
            Status.DISABLED, Status.FROZEN -> {
                logger.i("ShizukuEnabler", "尝试启用: $pkg")
                try {
                    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                    appOps.setMode(android.app.AppOpsManager.OPSTR_RUN_IN_BACKGROUND, 0, pkg, android.app.AppOpsManager.MODE_ALLOWED)
                    ShizukuApi.setApplicationEnabledSetting(pkg, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0)
                    logger.i("ShizukuEnabler", "启用成功: $pkg")
                    true
                } catch (e: Exception) {
                    logger.e("ShizukuEnabler", "启用失败: $pkg", e)
                    false
                }
            }
            Status.HIDDEN -> {
                logger.i("ShizukuEnabler", "尝试取消隐藏: $pkg")
                try {
                    ShizukuApi.setApplicationEnabledSetting(pkg, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0)
                    logger.i("ShizukuEnabler", "取消隐藏成功: $pkg")
                    true
                } catch (e: Exception) {
                    logger.e("ShizukuEnabler", "取消隐藏失败: $pkg", e)
                    false
                }
            }
            else -> {
                logger.d("ShizukuEnabler", "无需处理: $pkg (状态: ${currentState.label})")
                true
            }
        }
    }

    suspend fun installApk(apkFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            logger.w("ShizukuEnabler", "Shizuku 不可用，无法静默安装")
            return@withContext false
        }
        logger.i("ShizukuEnabler", "静默安装: ${apkFile.name}")
        return@withContext try {
            ShizukuApi.installPackage(apkFile.absolutePath, null, 0)
            logger.i("ShizukuEnabler", "静默安装成功: ${apkFile.name}")
            true
        } catch (e: Exception) {
            logger.e("ShizukuEnabler", "静默安装失败", e)
            false
        }
    }

    fun grantRuntimePermissions(packageName: String) {
        if (!isShizukuAvailable()) return
        try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val permissions = info.requestedPermissions ?: emptyArray()
            permissions.forEach { perm ->
                try {
                    ShizukuApi.grantRuntimePermission(packageName, perm, 0)
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }
}