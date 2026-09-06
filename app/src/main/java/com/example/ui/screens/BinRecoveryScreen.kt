package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carver.FileSignature
import com.example.carver.SignatureRegistry
import com.example.core.HashUtils
import com.example.model.BinLearnedProfile
import com.example.model.BinRecoveryConfig
import com.example.model.ScanLevel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.ForensicCardBorder
import com.example.ui.theme.ForensicSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.ValidatedGreen
import com.example.ui.theme.WarningAmber
import java.io.File

@Composable
fun BinRecoveryScreen(
  binConfig: BinRecoveryConfig,
  learnedProfile: BinLearnedProfile?,
  onBack: () -> Unit,
  onUpdateConfig: (BinRecoveryConfig) -> Unit,
  onLearnFromSample: (File) -> Unit,
  onStartTargetedScan: (config: BinRecoveryConfig) -> Unit,
  modifier: Modifier = Modifier
) {
  var folderPath by remember { mutableStateOf(binConfig.targetFolderPath) }
  var filename by remember { mutableStateOf(binConfig.knownFilename) }
  var headerHex by remember { mutableStateOf(binConfig.knownHeaderHex) }
  var footerHex by remember { mutableStateOf(binConfig.knownFooterHex) }
  var samplePathInput by remember { mutableStateOf(binConfig.sampleBinPath ?: "") }
  var strideInput by remember { mutableStateOf(binConfig.expectedRecordStride?.toString() ?: "") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_back_bin")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = WarningAmber
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = "RECOVER DELETED .BIN DATA",
          color = TextPrimaryDark,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }
    }

    // Explanation Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1911)),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = "Deep Mode", tint = WarningAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text = "TARGETED BINARY RECONSTRUCTION",
              color = WarningAmber,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(Modifier.height(6.dp))
          Text(
            text = ".BIN files have diverse formats (firmware, databases, sensor dumps, game saves). Provide known offsets, hex signatures, or supply a surviving .BIN sample to automatically train the carver.",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }
      }
    }

    // 1. Train from Surviving Sample
    item {
      Text(
        text = "LEARN FROM SURVIVING .BIN SAMPLE",
        color = TextSecondaryDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ForensicSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          OutlinedTextField(
            value = samplePathInput,
            onValueChange = { samplePathInput = it },
            label = { Text("Sample .BIN File Path") },
            placeholder = { Text("/storage/emulated/0/.../sample.bin") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_sample_bin"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedBorderColor = CyberCyan,
              unfocusedBorderColor = ForensicCardBorder
            ),
            singleLine = true
          )
          Spacer(Modifier.height(8.dp))
          Button(
            onClick = {
              val f = File(samplePathInput.trim())
              if (f.exists()) {
                onLearnFromSample(f)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = DeepCobalt),
            modifier = Modifier.testTag("btn_learn_sample")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Learn", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Extract Profile & Signatures", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          learnedProfile?.let { prof ->
            Spacer(Modifier.height(10.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF091624))
                .padding(10.dp)
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Learned", tint = ValidatedGreen, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("LEARNED .BIN PROFILE:", color = ValidatedGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("Header Hex: ${prof.detectedHeaderHex}", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("Avg Entropy: ${String.format("%.2f", prof.averageEntropy)} / 8.0", color = TextSecondaryDark, fontSize = 10.sp)
                Text("Detected Stride: ${prof.estimatedStride} bytes", color = TextSecondaryDark, fontSize = 10.sp)
                Text("Sample Size: ${prof.sampleSizeBytes} bytes", color = TextSecondaryDark, fontSize = 10.sp)
              }
            }
          }
        }
      }
    }

    // 2. Targeted Parameters
    item {
      Text(
        text = "KNOWN FILE ATTRIBUTES",
        color = TextSecondaryDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
    }

    item {
      OutlinedTextField(
        value = folderPath,
        onValueChange = { folderPath = it },
        label = { Text("Deleted Folder Path (e.g. /storage/emulated/0/MyBinFolder)") },
        placeholder = { Text("/storage/emulated/0/...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_bin_folder"),
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
        value = filename,
        onValueChange = { filename = it },
        label = { Text("Original Filename (if known)") },
        placeholder = { Text("data_dump.bin") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_bin_filename"),
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
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = headerHex,
          onValueChange = { headerHex = it },
          label = { Text("Header Magic Hex") },
          placeholder = { Text("50 4B 03 04") },
          modifier = Modifier
            .weight(1f)
            .testTag("input_bin_header_hex"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedBorderColor = CyberCyan,
            unfocusedBorderColor = ForensicCardBorder
          ),
          singleLine = true
        )
        OutlinedTextField(
          value = footerHex,
          onValueChange = { footerHex = it },
          label = { Text("Footer Hex (Optional)") },
          placeholder = { Text("FF D9") },
          modifier = Modifier
            .weight(1f)
            .testTag("input_bin_footer_hex"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedBorderColor = CyberCyan,
            unfocusedBorderColor = ForensicCardBorder
          ),
          singleLine = true
        )
      }
    }

    // Launch Targeted Search Button
    item {
      Spacer(Modifier.height(8.dp))
      Button(
        onClick = {
          val updatedConfig = binConfig.copy(
            targetFolderPath = folderPath.trim(),
            knownFilename = filename.trim(),
            knownHeaderHex = headerHex.trim(),
            knownFooterHex = footerHex.trim(),
            sampleBinPath = samplePathInput.trim().ifEmpty { null },
            expectedRecordStride = strideInput.toIntOrNull()
          )
          onUpdateConfig(updatedConfig)

          // Register user signature if provided
          if (headerHex.isNotBlank()) {
            val hBytes = HashUtils.bytesFromHex(headerHex)
            val fBytes = if (footerHex.isNotBlank()) HashUtils.bytesFromHex(footerHex) else null
            SignatureRegistry.registerCustomSignature(
              FileSignature(
                name = "Targeted .BIN Signature",
                extension = "bin",
                mimeType = "application/octet-stream",
                header = hBytes,
                footer = fBytes,
                minEntropy = 1.0,
                maxExpectedSize = 100L * 1024 * 1024
              )
            )
          }

          onStartTargetedScan(updatedConfig)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("btn_start_bin_recovery"),
        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Build, contentDescription = "Run", tint = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(
          text = "EXECUTE DEEP .BIN RECOVERY",
          color = Color.Black,
          fontWeight = FontWeight.Black,
          fontSize = 13.sp,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
