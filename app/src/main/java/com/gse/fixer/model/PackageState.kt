package com.gse.fixer.model

import android.os.Build

data class PackageState(
    val packageName: String,
    val label: String,
    val status: Status,
    val versionCode: Long = -1,
    val versionName: String = "",
    val installer: String = "",
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val isDisabled: Boolean = false,
    val isFrozen: Boolean = false,
    val isStub: Boolean = false,
    val apkAssetName: String? = null,
    val downloadUrl: String? = null,
    val minVersionCode: Long = 0,
    val isRequired: Boolean = true,
) {
    val isInstalled: Boolean get() = versionCode != -1
    val isProblematic: Boolean get() = status != Status.OK
    val needsInstall: Boolean get() = status == Status.MISSING || status == Status.STUB
    val needsEnable: Boolean get() = status in setOf(Status.DISABLED, Status.FROZEN, Status.HIDDEN)
    val displayVersion: String get() = if (isInstalled) versionName else "未安装"
    val displayStatus: String get() = status.label
    val displayStatusColor: Int get() = status.color
}

enum class Status(
    val label: String,
    val color: Int
) {
    OK("正常", 0xFF4CAF50),
    DISABLED("被禁用", 0xFFF44336),
    FROZEN("被冻结", 0xFFF44336),
    HIDDEN("隐藏图标", 0xFFFF9800),
    STUB("版本过旧", 0xFFFF9800),
    MISSING("未安装", 0xFF9E9E9E),
    UNKNOWN("未知", 0xFF607D8B)
}

object TargetPackages {
    const val GSF = "com.google.android.gsf"
    const val GMS = "com.google.android.gms"
    const val PLAY_STORE = "com.android.vending"
    const val CHROME = "com.android.chrome"

    val ALL_IN_ORDER = listOf(
        PackageMeta(GSF, "Google 服务框架 (GSF)", "GoogleServicesFramework.apk", 
            minVersion = 200000000L, downloadUrl = null),
        PackageMeta(GMS, "Google Play 服务 (GMS)", "base.apk", 
            minVersion = 210000000L, downloadUrl = "https://github.com/your-repo/gms-releases/releases/download/latest/base.apk"),
        PackageMeta(PLAY_STORE, "Google Play 商店", "Phonesky.apk",
            minVersion = 30000000L, downloadUrl = null),
        PackageMeta(CHROME, "Chrome 浏览器", "Chrome.apk",
            minVersion = 100000000L, downloadUrl = null, isRequired = false)
    )

    data class PackageMeta(
        val packageName: String,
        val label: String,
        val apkAssetName: String?,
        val minVersion: Long,
        val downloadUrl: String?,
        val isRequired: Boolean = true
    )
}