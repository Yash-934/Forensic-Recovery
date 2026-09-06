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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
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
import com.example.core.HashUtils
import com.example.model.RecoveryCandidate
import com.example.model.ValidationState
import com.example.ui.theme.CorruptedRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.ForensicCardBorder
import com.example.ui.theme.ForensicSurface
import com.example.ui.theme.FragmentPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.ValidatedGreen
import com.example.ui.theme.WarningAmber

@Composable
fun ResultsScreen(
  candidates: List<RecoveryCandidate>,
  selectedCandidate: RecoveryCandidate?,
  onBack: () -> Unit,
  onSelectCandidate: (RecoveryCandidate) -> Unit,
  onOpenHexViewer: (RecoveryCandidate) -> Unit,
  onRecoverCandidate: (RecoveryCandidate) -> Unit,
  modifier: Modifier = Modifier
) {
  var filterText by remember { mutableStateOf("") }
  var minConfidence by remember { mutableStateOf(0.0) }

  val filteredCandidates = candidates.filter {
    (filterText.isEmpty() || it.suggestedFilename.contains(filterText, ignoreCase = true) || it.extension.contains(filterText, ignoreCase = true)) &&
        it.confidenceScore >= minConfidence
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_back_results")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CyberCyan
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = "EVIDENCE CANDIDATES (${filteredCandidates.size})",
          color = TextPrimaryDark,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }
    }

    // Filter Bar
    item {
      OutlinedTextField(
        value = filterText,
        onValueChange = { filterText = it },
        placeholder = { Text("Filter by filename or extension (e.g. .bin, .jpg)") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("filter_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimaryDark,
          unfocusedTextColor = TextPrimaryDark,
          focusedBorderColor = CyberCyan,
          unfocusedBorderColor = ForensicCardBorder
        ),
        singleLine = true
      )
    }

    // Filter Chips
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          label = "All (${candidates.size})",
          isSelected = minConfidence == 0.0,
          onClick = { minConfidence = 0.0 }
        )
        FilterChip(
          label = "Probable (>=50%)",
          isSelected = minConfidence == 50.0,
          onClick = { minConfidence = 50.0 }
        )
        FilterChip(
          label = "Validated (>=80%)",
          isSelected = minConfidence == 80.0,
          onClick = { minConfidence = 80.0 }
        )
      }
    }

    // Selected Candidate Inspector Detail Card
    selectedCandidate?.let { c ->
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_candidate_inspector"),
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
                text = c.suggestedFilename,
                color = CyberCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(CyberCyan)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "${c.confidenceScore.toInt()}% SCORE",
                  color = Color.Black,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black
                )
              }
            }

            Spacer(Modifier.height(8.dp))
            Text(
              text = "State: ${c.validationState.name} | Size: ${HashUtils.formatFileSize(c.size)} (${c.size} B) | MIME: ${c.mimeGuess}",
              color = TextSecondaryDark,
              fontSize = 11.sp
            )
            Text(
              text = "SHA-256: ${c.sha256}",
              color = TextSecondaryDark,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1
            )
            Text(
              text = "CRC-32: 0x${c.crc32.toString(16).uppercase()} | Entropy: ${String.format("%.2f", c.entropy)}/8.0",
              color = TextSecondaryDark,
              fontSize = 10.sp
            )

            Spacer(Modifier.height(8.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF091424))
                .padding(8.dp)
            ) {
              Text(
                text = c.confidenceBreakdown,
                color = ValidatedGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
              )
            }

            Spacer(Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { onOpenHexViewer(c) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_inspect_hex")
              ) {
                Icon(Icons.Default.FindInPage, contentDescription = "Hex", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hex Viewer", fontSize = 11.sp)
              }
              Button(
                onClick = { onRecoverCandidate(c) },
                colors = ButtonDefaults.buttonColors(containerColor = ValidatedGreen),
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_recover_evidence")
              ) {
                Icon(Icons.Default.Download, contentDescription = "Recover", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Safe Recover", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // List of candidates
    if (filteredCandidates.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = ForensicSurface),
          border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
        ) {
          Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No candidates match current filters.", color = TextSecondaryDark, fontSize = 12.sp)
          }
        }
      }
    } else {
      items(filteredCandidates) { item ->
        CandidateRow(
          candidate = item,
          isSelected = item.id == selectedCandidate?.id,
          onClick = { onSelectCandidate(item) }
        )
      }
    }
  }
}

@Composable
fun FilterChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else ForensicSurface)
      .border(1.dp, if (isSelected) CyberCyan else ForensicCardBorder, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      color = if (isSelected) CyberCyan else TextSecondaryDark,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun CandidateRow(
  candidate: RecoveryCandidate,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("candidate_row_${candidate.id.take(8)}"),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) Color(0xFF13233E) else ForensicSurface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) CyberCyan else ForensicCardBorder
    )
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
        if (candidate.notes.isNotEmpty()) {
          Text(
            text = candidate.notes,
            color = WarningAmber,
            fontSize = 9.sp,
            maxLines = 1
          )
        }
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(
            when (candidate.validationState) {
              ValidationState.VALIDATED -> ValidatedGreen.copy(alpha = 0.2f)
              ValidationState.PROBABLE -> CyberCyan.copy(alpha = 0.2f)
              ValidationState.FRAGMENTARY -> FragmentPurple.copy(alpha = 0.2f)
              ValidationState.CORRUPTED -> WarningAmber.copy(alpha = 0.2f)
              ValidationState.REJECTED -> CorruptedRed.copy(alpha = 0.2f)
            }
          )
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${candidate.confidenceScore.toInt()}%",
          color = when (candidate.validationState) {
            ValidationState.VALIDATED -> ValidatedGreen
            ValidationState.PROBABLE -> CyberCyan
            ValidationState.FRAGMENTARY -> FragmentPurple
            ValidationState.CORRUPTED -> WarningAmber
            ValidationState.REJECTED -> CorruptedRed
          },
          fontSize = 11.sp,
          fontWeight = FontWeight.Black
        )
      }
    }
  }
}
