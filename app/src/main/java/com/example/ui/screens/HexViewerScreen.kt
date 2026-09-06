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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.HashUtils
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ForensicCardBorder
import com.example.ui.theme.ForensicNavyDark
import com.example.ui.theme.ForensicSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber

@Composable
fun HexViewerScreen(
  title: String,
  data: ByteArray,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var offsetJumpInput by remember { mutableStateOf("") }
  var jumpOffset by remember { mutableStateOf(0) }

  val lineCount = (data.size + 15) / 16

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ForensicNavyDark)
      .padding(horizontal = 12.dp),
    contentPadding = PaddingValues(vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
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
            modifier = Modifier.testTag("btn_back_hex")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = CyberCyan
            )
          }
          Spacer(Modifier.width(6.dp))
          Text(
            text = title,
            color = TextPrimaryDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = "Read-Only", tint = WarningAmber, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("READ ONLY", color = WarningAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Search and Jump Toolbar
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ForensicSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForensicCardBorder)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search text / hex pattern", fontSize = 11.sp) },
              modifier = Modifier
                .weight(1f)
                .testTag("hex_search_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = ForensicCardBorder
              ),
              singleLine = true
            )
            OutlinedTextField(
              value = offsetJumpInput,
              onValueChange = {
                offsetJumpInput = it
                jumpOffset = it.toIntOrNull(16) ?: it.toIntOrNull() ?: 0
              },
              placeholder = { Text("Offset (0x)", fontSize = 11.sp) },
              modifier = Modifier
                .width(110.dp)
                .testTag("hex_jump_offset"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = ForensicCardBorder
              ),
              singleLine = true
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = "Total bytes loaded: ${data.size} bytes | CRC-32: 0x${HashUtils.crc32(data).toString(16).uppercase()}",
            color = TextSecondaryDark,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // Column Headers
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF0F1A2C))
          .padding(horizontal = 8.dp, vertical = 6.dp)
      ) {
        Text(
          text = "OFFSET",
          color = CyberCyan,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(68.dp)
        )
        Text(
          text = "00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F",
          color = TextSecondaryDark,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = "ASCII",
          color = WarningAmber,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(72.dp)
        )
      }
    }

    // Hex rows
    if (data.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No evidence bytes loaded for this candidate.", color = TextSecondaryDark, fontSize = 12.sp)
        }
      }
    } else {
      items(minOf(lineCount, 512)) { lineIdx ->
        val lineOffset = lineIdx * 16
        val remaining = data.size - lineOffset
        val count = minOf(16, remaining)

        val hexSb = StringBuilder()
        val asciiSb = StringBuilder()

        for (i in 0 until 16) {
          if (i < count) {
            val b = data[lineOffset + i]
            hexSb.append(String.format("%02X", b.toInt() and 0xFF))
            val ch = b.toInt() and 0xFF
            if (ch in 32..126) {
              asciiSb.append(ch.toChar())
            } else {
              asciiSb.append('.')
            }
          } else {
            hexSb.append("  ")
            asciiSb.append(' ')
          }
          if (i == 7) hexSb.append("  ") else hexSb.append(" ")
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = String.format("%06X", lineOffset),
            color = CyberCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(68.dp)
          )
          Text(
            text = hexSb.toString(),
            color = TextPrimaryDark,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = asciiSb.toString(),
            color = WarningAmber,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(72.dp)
          )
        }
      }
    }
  }
}
