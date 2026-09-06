package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DeviceForensicCapabilities
import com.example.model.RecoveryCandidate
import com.example.model.ScanSession
import com.example.reporting.ForensicReportGenerator
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ForensicCardBorder
import com.example.ui.theme.ForensicNavyDark
import com.example.ui.theme.ForensicSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ReportScreen(
  activeSession: ScanSession?,
  capabilities: DeviceForensicCapabilities,
  candidates: List<RecoveryCandidate>,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val reportText = remember(activeSession, candidates) {
    if (activeSession != null) {
      ForensicReportGenerator.generateMarkdownReport(activeSession, capabilities, candidates)
    } else {
      "No active scan session available. Launch a scan from the dashboard to generate a certified forensic evidence report."
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ForensicNavyDark)
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("btn_back_report")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = CyberCyan
            )
          }
          Spacer(Modifier.width(8.dp))
          Text(
            text = "FORENSIC AUDIT REPORT",
            color = TextPrimaryDark,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }

        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Forensic Report", reportText)
            clipboard.setPrimaryClip(clip)
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.testTag("btn_copy_report")
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(4.dp))
          Text("Copy", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Report Box
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ForensicSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
      ) {
        Box(modifier = Modifier.padding(14.dp)) {
          Text(
            text = reportText,
            color = TextPrimaryDark,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
          )
        }
      }
    }
  }
}
