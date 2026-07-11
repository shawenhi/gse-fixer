package com.gse.fixer.core.verifier

import android.content.Context
import android.content.pm.PackageManager
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceVerifier @Inject constructor(
    private val context: Context,
    private val logger: SimpleLogger
) {
    private val pm = context.packageManager

    data class VerifyResult(
        val packageName: String,
        val checks: Map<String, Boolean>,
        val allPassed: Boolean
    )

    suspend fun verifyAll(states: List<PackageState>): List<VerifyResult> = withContext(Dispatchers.IO) {
        states.map { state ->
            if (state.status == Status.MISSING || state.status == Status.STUB) {
                VerifyResult(state.packageName, mapOf("installed" to false), false)
            } else {
                val checks = mutableMapOf<String, Boolean>()
                
                // 1. 包存在且启用
                val info = try {
                    pm.getPackageInfo(state.packageName, 0)
                } catch (e: Exception) {
                    null
                }
                checks["package_exists"] = info != null
                checks["enabled"] = info != null && pm.getApplicationEnabledSetting(state.packageName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                
                // 2. 版本达标
                if (info != null) {
                    checks["version_ok"] = info.longVersionCode >= state.minVersionCode
                }
                
                // 3. 关键服务/进程在运行 (GMS 特有)
                if (state.packageName == "com.google.android.gms") {
                    checks["gms_core_running"] = isProcessRunning("com.google.android.gms")
                    checks["gms_chimera_running"] = isProcessRunning("com.google.android.gms.chimera")
                }
                
                // 4. 签名一致性 (Google 官方签名)
                checks["signature_google"] = verifyGoogleSignature(state.packageName)
                
                // 5. Play Store 可启动
                if (state.packageName == "com.android.vending") {
                    checks["store_launchable"] = pm.getLaunchIntentForPackage(state.packageName) != null
                }
                
                VerifyResult(state.packageName, checks, checks.values.all { it })
            }
        }
    }

    private fun isProcessRunning(packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val processes = am.getRunningAppProcesses() ?: emptyList()
            processes.any { it.processName.startsWith(packageName) }
        } catch (_: Exception) { false }
    }

    private fun verifyGoogleSignature(packageName: String): Boolean {
        return try {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = info.signingInfo.getApkContentsSigners() ?: info.signatures
            signatures.any { sig ->
                val sha256 = sig.toByteArray().let { 
                    java.security.MessageDigest.getInstance("SHA-256").digest(it) 
                }.joinToString(":") { "%02X".format(it) }
                // Google 官方发布证书 SHA-256 (示例，实际需对比完整证书链)
                sha256.startsWith("38:91:8A") || sha256.startsWith("C3:7C:4D") || sha256.startsWith("9A:1E:1E")
            }
        } catch (_: Exception) { false }
    }
}