package com.gse.fixer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gse.fixer.core.detector.GoogleServiceDetector
import com.gse.fixer.core.downloader.GmsDownloader
import com.gse.fixer.core.enabler.ShizukuEnabler
import com.gse.fixer.core.installer.ApkInstaller
import com.gse.fixer.core.log.SimpleLogger
import com.gse.fixer.core.verifier.ServiceVerifier
import com.gse.fixer.di.Module
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import com.gse.fixer.ui.component.StatusCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.androidx.compose.get
import org.koin.core.parameter.parametersOf

@Composable
fun MainScreen() {
    val logger = remember { get<SimpleLogger>(parametersOf(Module)) }
    val detector = remember { get<GoogleServiceDetector>(parametersOf(Module)) }
    val enabler = remember { get<ShizukuEnabler>(parametersOf(Module)) }
    val installer = remember { get<ApkInstaller>(parametersOf(Module)) }
    val verifier = remember { get<ServiceVerifier>(parametersOf(Module)) }

    var states by remember { mutableStateOf<List<PackageState>>(emptyList()) }
    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    var shizukuAuthorized by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var fixProgress by remember { mutableStateOf<FixProgress?>(null) }

    // 检查 Shizuku 状态
    androidx.compose.runtime.LaunchedEffect(Unit) {
        shizukuAuthorized = enabler.isShizukuAvailable()
        if (!shizukuAuthorized) {
            logger.i("MainScreen", "Shizuku 不可用，需引导用户授权")
        }
        detect()
    }

    fun detect() {
        uiState = UiState.Loading
        CoroutineScope(Dispatchers.IO).launch {
            val result = detector.detectAll()
            androidx.compose.runtime.Snapshot.sendApplyNotifications()
            states = result
            shizukuAuthorized = enabler.isShizukuAvailable()
            uiState = UiState.Ready
        }
    }

    fun startFix() {
        val problematicStates = states.filter { it.isProblematic && it.isRequired }
        if (problematicStates.isEmpty()) return

        fixProgress = FixProgress(total = problematicStates.size, current = 0, currentLabel = "")
        uiState = UiState.Fixing

        CoroutineScope(Dispatchers.IO).launch {
            var allSuccess = true
            var completed = 0

            for (state in problematicStates) {
                val label = state.label
                fixProgress = fixProgress.copy(current = completed, currentLabel = label)

                var success = false
                if (state.needsEnable) {
                    fixProgress = fixProgress.copy(currentLabel = stringResource(R.string.fixing_step_enable, label))
                    success = enabler.enablePackage(state)
                } else if (state.needsInstall) {
                    fixProgress = fixProgress.copy(currentLabel = stringResource(R.string.fixing_step_install, label))
                    success = installer.installIfNeeded(state) { progress ->
                        // TODO: 更新下载进度
                    }
                }

                if (!success) allSuccess = false
                completed++
                fixProgress = fixProgress.copy(current = completed)
            }

            // 验证
            fixProgress = fixProgress.copy(currentLabel = stringResource(R.string.fixing_step_verify, "全部"))
            val verifyResults = verifier.verifyAll(states)
            val allVerified = verifyResults.all { it.allPassed }

            uiState = if (allSuccess && allVerified) UiState.Success
            else if (allSuccess) UiState.Partial
            else UiState.Failed
            fixProgress = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(text = stringResource(R.string.app_name_short), fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { showLogs = !showLogs }) {
                        Icon(imageVector = Icons.Default.ListAlt, contentDescription = "日志")
                    }
                    IconButton(onClick = { detect() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }

            // Shizuku Status Banner
            if (!shizukuAuthorized) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.error)
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                                Text(text = stringResource(R.string.shizuku_not_installed), fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                            }
                            Text(text = stringResource(R.string.shizuku_not_installed_desc), fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                            Button(
                                onClick = { enabler.requestPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error)
                            ) {
                                Text(text = stringResource(R.string.btn_install_shizuku))
                            }
                        }
                    }
                }
            }

            // Progress / Fixing UI
            when (uiState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                            Text(text = stringResource(R.string.scan_title))
                        }
                    }
                }

                UiState.Fixing -> {
                    fixProgress?.let { progress ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        progress = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f,
                                        modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                                    )
                                    Text(text = progress.currentLabel, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                    Text(text = "${progress.current}/${progress.total}", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                UiState.Success, UiState.Partial, UiState.Failed -> {
                    val (title, subtitle) = when (uiState) {
                        UiState.Success -> stringResource(R.string.fix_complete) to stringResource(R.string.fix_complete_subtitle)
                        UiState.Partial -> stringResource(R.string.fix_partial) to "部分项目修复成功，请检查日志"
                        UiState.Failed -> stringResource(R.string.fix_failed) to "修复失败，请查看日志或手动处理"
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(24.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when (uiState) {
                                        UiState.Success -> Icons.Default.CheckCircle
                                        UiState.Partial -> Icons.Default.Warning
                                        UiState.Failed -> Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = when (uiState) {
                                        UiState.Success -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                                        UiState.Partial -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                                        UiState.Failed -> androidx.compose.material3.MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                                )
                                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text(text = subtitle, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)

                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { /* TODO: 打开 Play Store */ },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(text = stringResource(R.string.btn_open_play_store)) }
                                    Button(
                                        onClick = { /* TODO: 打开 Chrome */ },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(text = stringResource(R.string.btn_open_chrome)) }
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                                Button(
                                    onClick = { detect() },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                                ) { Text(text = stringResource(R.string.btn_retry)) }
                            }
                        }
                    }
                }

                UiState.Ready -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(states) { state ->
                            StatusCard(
                                state = state,
                                onActionClick = {
                                    // 单项修复
                                    CoroutineScope(Dispatchers.IO).launch {
                                        if (state.needsEnable) enabler.enablePackage(state)
                                        else if (state.needsInstall) installer.installIfNeeded(state)
                                        detect()
                                    }
                                }
                            )
                        }
                    }

                    // 一键修复按钮
                    val hasProblematic = states.any { it.isProblematic && it.isRequired }
                    if (hasProblematic) {
                        Button(
                            onClick = { startFix() },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = stringResource(R.string.btn_start_fix), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Logs overlay
        if (showLogs) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showLogs = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.log_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { showLogs = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                logs.reversed().forEach { line ->
                                    Text(text = line, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface UiState {
    object Loading : UiState
    object Ready : UiState
    object Fixing : UiState
    object Success : UiState
    object Partial : UiState
    object Failed : UiState
}

data class FixProgress(
    val total: Int,
    val current: Int,
    val currentLabel: String
)