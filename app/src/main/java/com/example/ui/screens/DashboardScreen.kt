package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DeviceForensicCapabilities
import com.example.core.HashUtils
import com.example.core.RecoveryCapabilityLevel
import com.example.model.RecoveryCandidate
import com.example.model.ScanProgress
import com.example.ui.theme.CorruptedRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.ForensicCardBorder
import com.example.ui.theme.ForensicNavyDark
import com.example.ui.theme.ForensicSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.ValidatedGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.Screen

@Composable
fun DashboardScreen(
  capabilities: DeviceForensicCapabilities,
  scanProgress: ScanProgress?,
  isScanning: Boolean,
  candidates: List<RecoveryCandidate>,
  onNavigate: (Screen) -> Unit,
  onCancelScan: () -> Unit,
  onSelectCandidate: (RecoveryCandidate) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Title
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "AEGIS FORENSICS",
            color = CyberCyan,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
          )
          Text(
            text = "Digital Evidence Recovery Engine",
            color = TextSecondaryDark,
            fontSize = 12.sp
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13233C))
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = capabilities.capabilityLevel.name,
            color = when (capabilities.capabilityLevel) {
              RecoveryCapabilityLevel.ROOTED -> ValidatedGreen
              RecoveryCapabilityLevel.STANDARD -> CyberCyan
              RecoveryCapabilityLevel.IMAGE_ANALYSIS -> DeepCobalt
              RecoveryCapabilityLevel.LIMITED -> WarningAmber
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Active Scan Progress Banner
    item {
      AnimatedVisibility(visible = isScanning && scanProgress != null) {
        scanProgress?.let { prog ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("scan_progress_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E38)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = prog.phase,
                  color = CyberCyan,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Button(
                  onClick = onCancelScan,
                  colors = ButtonDefaults.buttonColors(containerColor = CorruptedRed),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("cancel_scan_button")
                ) {
                  Icon(Icons.Default.Stop, contentDescription = "Halt Scan", modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Halt", fontSize = 11.sp)
                }
              }
              Spacer(Modifier.height(8.dp))
              LinearProgressIndicator(
                progress = { prog.percentage },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(CircleShape),
                color = CyberCyan,
                trackColor = Color(0xFF1E293B)
              )
              Spacer(Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Scanned: ${HashUtils.formatFileSize(prog.bytesProcessed)}",
                  color = TextSecondaryDark,
                  fontSize = 11.sp
                )
                Text(
                  text = "Candidates: ${prog.candidatesFound} (${prog.validCandidates} valid)",
                  color = ValidatedGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Text(
                text = "Target: ${prog.currentTarget}",
                color = TextSecondaryDark,
                fontSize = 10.sp,
                maxLines = 1,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }

    // Hardware & Environment Capability Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ForensicSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Security Status",
              tint = CyberCyan,
              modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "DEVICE CAPABILITY ASSESSMENT",
              color = TextPrimaryDark,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          }

          Spacer(Modifier.height(12.dp))

          CapabilityRow("Target Hardware", "${capabilities.manufacturer} ${capabilities.model}")
          CapabilityRow("Platform OS", capabilities.androidVersion)
          CapabilityRow("Primary FS", capabilities.primaryFilesystem)
          CapabilityRow(
            "Root Privilege",
            if (capabilities.isRootAvailable) "VERIFIED ROOT (Block access possible)" else "UNROOTED (Standard Android)"
          )
          CapabilityRow("Storage Scope", if (capabilities.hasManageExternalStorage) "All-Files Access (Granted)" else "Restricted Scoped Sandbox")

          Spacer(Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0A1322))
              .padding(8.dp)
          ) {
            Text(
              text = capabilities.capabilityExplanation,
              color = TextSecondaryDark,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }
    }

    // Quick Action Matrix
    item {
      Text(
        text = "RECOVERY OPERATIONS",
        color = TextSecondaryDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OperationCard(
          title = "New Scan",
          subtitle = "General files & storage",
          icon = Icons.Default.Analytics,
          accentColor = CyberCyan,
          modifier = Modifier.weight(1f),
          testTag = "btn_new_scan",
          onClick = { onNavigate(Screen.NEW_SCAN) }
        )
        OperationCard(
          title = "Deep .BIN",
          subtitle = "Targeted binary carving",
          icon = Icons.Default.Build,
          accentColor = WarningAmber,
          modifier = Modifier.weight(1f),
          testTag = "btn_deep_bin",
          onClick = { onNavigate(Screen.BIN_RECOVERY) }
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OperationCard(
          title = "Candidates",
          subtitle = "${candidates.size} evidence hits",
          icon = Icons.Default.FolderOpen,
          accentColor = ValidatedGreen,
          modifier = Modifier.weight(1f),
          testTag = "btn_results",
          onClick = { onNavigate(Screen.RESULTS) }
        )
        OperationCard(
          title = "Audit Report",
          subtitle = "Chain-of-custody",
          icon = Icons.Default.Description,
          accentColor = DeepCobalt,
          modifier = Modifier.weight(1f),
          testTag = "btn_report",
          onClick = { onNavigate(Screen.REPORT) }
        )
      }
    }

    // Recent Candidates Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "RECENT EVIDENCE CANDIDATES",
          color = TextSecondaryDark,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
        if (candidates.isNotEmpty()) {
          Text(
            text = "View All (${candidates.size})",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onNavigate(Screen.RESULTS) }
          )
        }
      }
    }

    if (candidates.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = ForensicSurface),
          border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = "Empty",
              tint = TextSecondaryDark,
              modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = "No scan sessions recorded yet",
              color = TextPrimaryDark,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Launch a scan or configure a targeted .BIN recovery above",
              color = TextSecondaryDark,
              fontSize = 11.sp
            )
          }
        }
      }
    } else {
      items(candidates.take(5)) { candidate ->
        CandidateItemCard(
          candidate = candidate,
          onClick = { onSelectCandidate(candidate) }
        )
      }
    }
  }
}

@Composable
fun CapabilityRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
    Text(
      text = value,
      color = TextPrimaryDark,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1
    )
  }
}

@Composable
fun OperationCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  testTag: String = "",
  onClick: () -> Unit
) {
  Card(
    modifier = modifier
      .testTag(testTag)
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = ForensicSurface),
    border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(18.dp))
      }
      Spacer(Modifier.height(10.dp))
      Text(text = title, color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      Text(text = subtitle, color = TextSecondaryDark, fontSize = 10.sp)
    }
  }
}

@Composable
fun CandidateItemCard(
  candidate: RecoveryCandidate,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("candidate_card_${candidate.id.take(8)}"),
    colors = CardDefaults.cardColors(containerColor = ForensicSurface),
    border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = candidate.suggestedFilename,
          color = TextPrimaryDark,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = "${candidate.extension.uppercase()} • ${HashUtils.formatFileSize(candidate.size)} • Entropy: ${String.format("%.2f", candidate.entropy)}",
          color = TextSecondaryDark,
          fontSize = 10.sp
        )
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(
            when {
              candidate.confidenceScore >= 80.0 -> ValidatedGreen.copy(alpha = 0.2f)
              candidate.confidenceScore >= 50.0 -> WarningAmber.copy(alpha = 0.2f)
              else -> CorruptedRed.copy(alpha = 0.2f)
            }
          )
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${candidate.confidenceScore.toInt()}%",
          color = when {
            candidate.confidenceScore >= 80.0 -> ValidatedGreen
            candidate.confidenceScore >= 50.0 -> WarningAmber
            else -> CorruptedRed
          },
          fontSize = 11.sp,
          fontWeight = FontWeight.Black
        )
      }
    }
  }
}
