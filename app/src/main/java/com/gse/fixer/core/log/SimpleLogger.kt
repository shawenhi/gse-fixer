package com.gse.fixer.core.log

import android.content.Context
import android.util.Log
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import org.koin.core.annotation.Inject
import org.koin.core.annotation.Single

@Single
class SimpleLogger @Inject constructor(
    private val context: Context
) {
    private val mmkv = MMKV.defaultMMKV()
    private val memoryLog = ConcurrentLinkedQueue<String>()
    private val maxMemoryLines = 500
    private val logFile = File(context.filesDir, "logs/gse_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.log")
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        logFile.parentFile?.mkdirs()
    }

    fun d(tag: String, msg: String) = log("D", tag, msg)
    fun i(tag: String, msg: String) = log("I", tag, msg)
    fun w(tag: String, msg: String) = log("W", tag, msg)
    fun e(tag: String, msg: String, throwable: Throwable? = null) = log("E", tag, "$msg${throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""}")

    private fun log(level: String, tag: String, msg: String) {
        val timestamp = dateFormat.format(Date())
        val line = "[$timestamp] [$level/$tag] $msg"

        // 内存环形缓冲
        memoryLog.add(line)
        while (memoryLog.size > maxMemoryLines) memoryLog.poll()

        // 文件落盘 (异步)
        scope.launch {
            try {
                FileWriter(logFile, true).use { it.write("$line\n") }
            } catch (e: Exception) { /* 忽略写入失败 */ }
        }

        // Android Logcat
        when (level) {
            "D" -> Log.d(tag, msg)
            "I" -> Log.i(tag, msg)
            "W" -> Log.w(tag, msg)
            "E" -> Log.e(tag, msg)
        }
    }

    fun getMemoryLogs(): List<String> = memoryLog.toList()

    suspend fun exportLogs(destFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            destFile.parentFile?.mkdirs()
            FileWriter(destFile).use { writer ->
                memoryLog.forEach { writer.write("$it\n") }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearLogs() {
        memoryLog.clear()
        logFile.delete()
    }
}