package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScanLevel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ForensicCardBorder
import com.example.ui.theme.ForensicSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.Screen

@Composable
fun NewScanScreen(
  onBack: () -> Unit,
  onStartScan: (scanType: String, level: ScanLevel, targetPath: String, targetExt: String?) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedLevel by remember { mutableStateOf(ScanLevel.BALANCED) }
  var targetPath by remember { mutableStateOf("") }
  var targetExtension by remember { mutableStateOf("bin") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_back")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CyberCyan
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = "CONFIG SCAN PIPELINE",
          color = TextPrimaryDark,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }
    }

    // Honest Technical Scope Notice
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132034)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Forensic Scope",
            tint = WarningAmber,
            modifier = Modifier.size(18.dp)
          )
          Spacer(Modifier.width(10.dp))
          Text(
            text = "Forensic Integrity Note: Standard unrooted Android allows logical inspection, MediaStore recycle queries, and storage carving. Physical raw blocks require root access or offline disk image parsing. We never fabricate recovery results.",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }
      }
    }

    // Scan Levels Selection
    item {
      Text(
        text = "ANALYSIS DEPTH LEVEL",
        color = TextSecondaryDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
    }

    items(ScanLevel.values().size) { idx ->
      val level = ScanLevel.values()[idx]
      val isSelected = level == selectedLevel
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { selectedLevel = level }
          .testTag("scan_level_${level.name}"),
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) Color(0xFF0F2647) else ForensicSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (isSelected) CyberCyan else ForensicCardBorder
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = level.title,
              color = if (isSelected) CyberCyan else TextPrimaryDark,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            if (isSelected) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(CyberCyan)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text("SELECTED", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
              }
            }
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = level.description,
            color = TextSecondaryDark,
            fontSize = 11.sp
          )
        }
      }
    }

    // Target Options
    item {
      Text(
        text = "TARGET PARAMETERS",
        color = TextSecondaryDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
    }

    item {
      OutlinedTextField(
        value = targetPath,
        onValueChange = { targetPath = it },
        label = { Text("Target Storage Path (Optional, blank for full storage)") },
        placeholder = { Text("/storage/emulated/0/...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_target_path"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimaryDark,
          unfocusedTextColor = TextPrimaryDark,
          focusedBorderColor = CyberCyan,
          unfocusedBorderColor = ForensicCardBorder
        ),
        singleLine = true
      )
    }

    item {
      OutlinedTextField(
        value = targetExtension,
        onValueChange = { targetExtension = it },
        label = { Text("Target Extension (e.g. bin, jpg, db, or empty for all)") },
        placeholder = { Text("bin") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_target_extension"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimaryDark,
          unfocusedTextColor = TextPrimaryDark,
          focusedBorderColor = CyberCyan,
          unfocusedBorderColor = ForensicCardBorder
        ),
        singleLine = true
      )
    }

    // Execute Button
    item {
      Spacer(Modifier.height(8.dp))
      Button(
        onClick = {
          onStartScan(
            "STORAGE_SCAN",
            selectedLevel,
            targetPath.trim(),
            targetExtension.trim().ifEmpty { null }
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("btn_execute_scan"),
        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Execute", tint = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(
          text = "EXECUTE FORENSIC SCAN",
          color = Color.Black,
          fontWeight = FontWeight.Black,
          fontSize = 13.sp,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
