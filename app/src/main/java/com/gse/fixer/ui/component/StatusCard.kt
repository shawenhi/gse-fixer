package com.gse.fixer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gse.fixer.model.PackageState
import com.gse.fixer.model.Status
import com.gse.fixer.R

@Composable
fun StatusCard(
    state: PackageState,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = Color(state.displayStatusColor)
    val isProblematic = state.isProblematic
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isProblematic) statusColor.copy(alpha = 0.1f) else statusColor.copy(alpha = 0.05f),
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isProblematic) androidx.compose.ui.graphics.Outline.Border(2.dp, statusColor) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Icon + Label + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = getIconForPackage(state.packageName),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 12.dp))
                    Column {
                        Text(
                            text = state.label,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Text(
                            text = state.displayStatus,
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Version badge
                if (state.isInstalled) {
                    Text(
                        text = "v${state.versionName}",
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            
            // Detail row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.isInstalled) {
                    Text(
                        text = stringResource(R.string.detail_installer, state.installer),
                        fontSize = 11.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (state.isSystemApp) stringResource(R.string.detail_system) else "",
                        fontSize = 11.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "需安装",
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Action button
            if (state.isProblematic) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                androidx.compose.material3.Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (state.needsInstall) stringResource(R.string.action_install)
                        else stringResource(R.string.action_enable),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            } else {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 4.dp))
                    Text(
                        text = stringResource(R.string.status_ok),
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun getIconForPackage(packageName: String) = when (packageName) {
    "com.google.android.gsf" -> androidx.compose.material.icons.Icons.Default.Settings
    "com.google.android.gms" -> androidx.compose.material.icons.Icons.Default.CloudSync
    "com.android.vending" -> androidx.compose.material.icons.Icons.Default.ShoppingBag
    "com.android.chrome" -> androidx.compose.material.icons.Icons.Default.Web
    else -> androidx.compose.material.icons.Icons.Default.Android
}