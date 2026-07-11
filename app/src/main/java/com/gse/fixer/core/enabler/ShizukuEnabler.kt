package com.gse.fixer.core.enabler

import android.content.Context
import android.content.pm.PackageManager
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import moe.shizuku.privileged.api.Shizuku
import moe.shizuku.privileged.api.SystemServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import org.koin.core.annotation.Inject
import org.koin.core.annotation.Single

@Single
class ShizukuEnabler @Inject constructor(
    private val context: Context,
    private val logger: SimpleLogger
) {
    private val pm: PackageManager = SystemServiceHelper.getPackageManager()

    fun isShizukuAvailable(): Boolean = Shizuku.pingBinder()

    fun requestPermission() {
        Shizuku.requestPermission(context)
    }

    suspend fun enablePackage(state: PackageState): Boolean = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            logger.w("ShizukuEnabler", "Shizuku 不可用，跳过启用: ${state.packageName}")
            return@withContext false
        }
        
        val pkg = state.packageName
        var success = false
        
        when (state.status) {
            Status.DISABLED, Status.FROZEN -> {
                // 1. installExistingPackageAsUser (最强，能解冻+启用)
                success = try {
                    pm.installExistingPackageAsUser(pkg, 0)
                    logger.i("ShizukuEnabler", "installExistingPackageAsUser 成功: $pkg")
                    true
                } catch (e: Exception) {
                    logger.w("ShizukuEnabler", "installExistingPackageAsUser 失败: $pkg", e)
                    false
                }
                
                // 2. 兜底：setApplicationEnabledSetting
                if (!success) {
                    success = try {
                        pm.setApplicationEnabledSetting(pkg, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0)
                        logger.i("ShizukuEnabler", "setApplicationEnabledSetting 成功: $pkg")
                        true
                    } catch (e: Exception) {
                        logger.w("ShizukuEnabler", "setApplicationEnabledSetting 失败: $pkg", e)
                        false
                    }
                }
                
                // 3. 兜底：反射 unfreezePackage
                if (!success) {
                    success = try {
                        val method: Method = pm.javaClass.getMethod("unfreezePackage", String::class.java, Int::class.java)
                        method.invoke(pm, pkg, 0)
                        logger.i("ShizukuEnabler", "unfreezePackage 反射成功: $pkg")
                        true
                    } catch (e: Exception) {
                        logger.w("ShizukuEnabler", "unfreezePackage 反射失败: $pkg", e)
                        false
                    }
                }
            }
            
            Status.HIDDEN -> {
                // 取消隐藏
                success = try {
                    pm.setApplicationEnabledSetting(pkg, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0)
                    logger.i("ShizukuEnabler", "取消隐藏成功: $pkg")
                    true
                } catch (e: Exception) {
                    logger.w("ShizukuEnabler", "取消隐藏失败: $pkg", e)
                    false
                }
            }
            
            else -> {
                success = true // OK/STUB/MISSING 不处理
            }
        }
        
        // 无论哪种方式成功，都尝试授予运行时权限
        if (success) {
            grantRuntimePermissions(pkg)
        }
        
        success
    }

    private fun grantRuntimePermissions(packageName: String) {
        try {
            val permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions ?: emptyArray()
            permissions.forEach { perm ->
                try {
                    pm.grantRuntimePermission(packageName, perm, 0)
                } catch (_: Exception) { /* 忽略单个权限失败 */ }
            }
        } catch (_: Exception) { /* 忽略 */ }
    }

    fun installApk(apkFile: java.io.File): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            pm.installPackage(
                apkFile.absolutePath,
                0, // flags
                null, // observer
                0 // userId
            )
            logger.i("ShizukuEnabler", "静默安装成功: ${apkFile.name}")
            true
        } catch (e: Exception) {
            logger.w("ShizukuEnabler", "静默安装失败", e)
            false
        }
    }
}
