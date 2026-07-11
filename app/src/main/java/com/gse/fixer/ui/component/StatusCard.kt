package com.gse.fixer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status

@Composable
fun StatusCard(
    state: PackageState,
    onActionClick: () -> Unit
) {
    val statusInfo = when (state.status) {
        Status.OK -> androidx.compose.material3.MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle
        Status.DISABLED, Status.FROZEN -> androidx.compose.material3.MaterialTheme.colorScheme.error to Icons.Default.Warning
        Status.HIDDEN, Status.STUB -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary to Icons.Default.Info
        Status.MISSING -> androidx.compose.material3.MaterialTheme.colorScheme.outline to Icons.Default.CloudSync
        Status.UNKNOWN -> androidx.compose.material3.MaterialTheme.colorScheme.outline to Icons.Default.Warning
    }
    val actionInfo = when (state.status) {
        Status.OK -> stringResource(com.gse.fixer.R.string.status_ok) to Icons.Default.CheckCircle
        Status.DISABLED, Status.FROZEN -> stringResource(com.gse.fixer.R.string.action_enable) to Icons.Default.PlayArrow
        Status.HIDDEN, Status.STUB -> stringResource(com.gse.fixer.R.string.action_fix) to Icons.Default.PlayArrow
        Status.MISSING -> stringResource(com.gse.fixer.R.string.action_install) to Icons.Default.ArrowDownward
        Status.UNKNOWN -> stringResource(com.gse.fixer.R.string.action_detect) to Icons.Default.Refresh
    }
    val statusColor = statusInfo.first
    val statusIcon = statusInfo.second
    val actionText = actionInfo.first
    val actionIcon = actionInfo.second

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (state.packageName) {
                            "com.google.android.gsf" -> Icons.Default.CloudSync
                            "com.google.android.gms" -> Icons.Default.PlayArrow
                            "com.android.vending" -> Icons.Default.ShoppingBag
                            "com.android.chrome" -> Icons.Default.Web
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp).padding(end = 12.dp)
                    )
                    Column {
                        Text(text = state.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (state.isInstalled) "${state.versionName} (${state.versionCode})" else state.displayVersion,
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                        Text(text = state.displayStatus, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = statusColor)
                    }
                    if (state.isSystemApp) {
                        Text(text = stringResource(com.gse.fixer.R.string.detail_system), fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Details
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = stringResource(com.gse.fixer.R.string.detail_installer, state.installer), fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.minVersionCode > 0 && state.isInstalled) {
                        Text(text = "最低: ${state.minVersionCode}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Action button
            if (state.isProblematic && state.isRequired) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = actionIcon, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                        Text(text = actionText, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}