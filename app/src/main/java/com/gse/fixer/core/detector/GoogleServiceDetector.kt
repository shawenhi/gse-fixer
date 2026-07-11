package com.gse.fixer.core.detector

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import com.gse.fixer.model.TargetPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Inject
import org.koin.core.annotation.Single
import java.util.Locale

@Single
class GoogleServiceDetector @Inject constructor(
    private val context: Context,
    private val logger: SimpleLogger
) {
    private val pm = context.packageManager

    suspend fun detectAll(): List<PackageState> = withContext(Dispatchers.IO) {
        logger.i("Detector", "开始检测 Google 服务状态...")
        val results = TargetPackages.ALL_IN_ORDER.map { meta -> detectSingle(meta) }
        results.forEach { state ->
            logger.i("Detector", "${state.label} (${state.packageName}): ${state.displayStatus} v${state.displayVersion}")
        }
        results
    }

    private fun detectSingle(meta: TargetPackages.PackageMeta): PackageState {
        return try {
            val info = pm.getPackageInfo(meta.packageName, PackageManager.GET_DISABLED_COMPONENTS)
            val appInfo = info.applicationInfo

            val versionCode = info.longVersionCode
            val versionName = info.versionName ?: ""
            val installer = pm.getInstallSourceInfo(meta.packageName)?.installingPackageName ?: "system"
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            var status = Status.OK
            var isHidden = false
            var isDisabled = false
            var isFrozen = false
            var isStub = false

            // 1. 系统占位符判断：系统应用且版本过低
            if (isSystem && versionCode < meta.minVersion) {
                status = Status.STUB
                isStub = true
                logger.w("Detector", "${meta.label} 疑似系统占位符: versionCode=$versionCode < ${meta.minVersion}")
            }
            // 2. 启用状态检查
            else {
                val enabledSetting = pm.getApplicationEnabledSetting(meta.packageName)
                when (enabledSetting) {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> {
                        status = Status.DISABLED
                        isDisabled = true
                        logger.w("Detector", "${meta.label} 被禁用: enabledSetting=$enabledSetting")
                    }
                    else -> {
                        if (enabledSetting == 4) { // COMPONENT_ENABLED_STATE_HIDDEN (API 24+)
                            status = Status.HIDDEN
                            isHidden = true
                            logger.w("Detector", "${meta.label} 被隐藏")
                        } else if (isLikelyFrozen(meta.packageName)) {
                            status = Status.FROZEN
                            isFrozen = true
                            logger.w("Detector", "${meta.label} 疑似被冻结")
                        } else if (!hasLauncherIcon(meta.packageName)) {
                            status = Status.HIDDEN
                            isHidden = true
                            logger.w("Detector", "${meta.label} 无 Launcher 图标")
                        }
                    }
                }
            }

            PackageState(
                packageName = meta.packageName,
                label = meta.label,
                status = status,
                versionCode = versionCode,
                versionName = versionName,
                installer = installer,
                isSystemApp = isSystem,
                isHidden = isHidden,
                isDisabled = isDisabled,
                isFrozen = isFrozen,
                isStub = isStub,
                apkAssetName = meta.apkAssetName,
                downloadUrl = meta.downloadUrl,
                minVersionCode = meta.minVersion,
                isRequired = meta.isRequired
            )
        } catch (e: PackageManager.NameNotFoundException) {
            logger.w("Detector", "${meta.label} 未安装")
            PackageState(
                packageName = meta.packageName,
                label = meta.label,
                status = Status.MISSING,
                apkAssetName = meta.apkAssetName,
                downloadUrl = meta.downloadUrl,
                minVersionCode = meta.minVersion,
                isRequired = meta.isRequired
            )
        }
    }

    private fun isLikelyFrozen(packageName: String): Boolean {
        return try {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent == null) false else {
                context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                false
            }
        } catch (e: Exception) {
            val msg = e.message?.lowercase(Locale.getDefault()) ?: ""
            msg.contains("frozen") || msg.contains("stopped") || msg.contains("user restricted") || msg.contains("permission")
        }
    }

    private fun hasLauncherIcon(packageName: String): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        return pm.queryIntentActivities(intent, 0).isNotEmpty()
    }
}