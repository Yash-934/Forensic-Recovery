package com.example.binanalysis

import com.example.core.HashUtils
import com.example.model.BinLearnedProfile
import com.example.nativeengine.NativeForensicScanner
import java.io.File
import kotlin.math.abs

data class BinaryStructuralAnalysis(
  val entropy: Double,
  val printableStringDensity: Double,
  val nullByteRatio: Double,
  val encryptionLikelihood: Boolean,
  val compressionLikelihood: Boolean,
  val detectedRecordStride: Int?,
  val potentialHeaderHex: String,
  val embeddedStrings: List<String>,
  val inferredAlignment: Int,
  val hasEmbeddedSqlite: Boolean,
  val hasEmbeddedZip: Boolean
)

object BinForensicAnalyzer {

  fun analyzeBuffer(data: ByteArray, length: Int = data.size): BinaryStructuralAnalysis {
    val actualLen = minOf(data.size, length)
    if (actualLen <= 0) {
      return BinaryStructuralAnalysis(
        entropy = 0.0,
        printableStringDensity = 0.0,
        nullByteRatio = 0.0,
        encryptionLikelihood = false,
        compressionLikelihood = false,
        detectedRecordStride = null,
        potentialHeaderHex = "",
        embeddedStrings = emptyList(),
        inferredAlignment = 0,
        hasEmbeddedSqlite = false,
        hasEmbeddedZip = false
      )
    }

    val entropy = NativeForensicScanner.calculateEntropy(data, actualLen)

    var printableCount = 0
    var nullCount = 0
    for (i in 0 until actualLen) {
      val b = data[i].toInt() and 0xFF
      if (b == 0) {
        nullCount++
      } else if ((b in 32..126) || b == 9 || b == 10 || b == 13) {
        printableCount++
      }
    }

    val printableRatio = printableCount.toDouble() / actualLen
    val nullRatio = nullCount.toDouble() / actualLen

    // High entropy (> 7.7) with very few nulls & low printable text signals encryption or high compression
    val isHighEntropy = entropy >= 7.6
    val isEncrypted = isHighEntropy && nullRatio < 0.01 && printableRatio < 0.05
    val isCompressed = isHighEntropy && !isEncrypted

    // Detect repeated record stride (e.g. 16, 32, 64, 128, 256, 512, 1024, 4096 bytes)
    val stride = detectStride(data, actualLen)

    // First 8-16 bytes as candidate header
    val headerBytesCount = minOf(16, actualLen)
    val headerSub = data.copyOfRange(0, headerBytesCount)
    val headerHex = HashUtils.toHex(headerSub)

    // Extract ASCII/UTF-8 strings longer than 4 chars
    val extractedStrings = extractPrintableStrings(data, actualLen, minLength = 5, limit = 8)

    // Alignment test (4-byte, 8-byte, 16-byte, 512-byte or 4096-byte boundary)
    val alignment = when {
      actualLen % 4096 == 0 -> 4096
      actualLen % 512 == 0 -> 512
      actualLen % 16 == 0 -> 16
      actualLen % 8 == 0 -> 8
      actualLen % 4 == 0 -> 4
      else -> 1
    }

    // Check embedded signatures
    val sqliteSig = HashUtils.bytesFromHex("53 51 4C 69 74 65")
    val zipSig = HashUtils.bytesFromHex("50 4B 03 04")
    val hasSqlite = NativeForensicScanner.findPattern(data, actualLen, sqliteSig) != -1L
    val hasZip = NativeForensicScanner.findPattern(data, actualLen, zipSig) != -1L

    return BinaryStructuralAnalysis(
      entropy = entropy,
      printableStringDensity = printableRatio,
      nullByteRatio = nullRatio,
      encryptionLikelihood = isEncrypted,
      compressionLikelihood = isCompressed,
      detectedRecordStride = stride,
      potentialHeaderHex = headerHex,
      embeddedStrings = extractedStrings,
      inferredAlignment = alignment,
      hasEmbeddedSqlite = hasSqlite,
      hasEmbeddedZip = hasZip
    )
  }

  fun learnFromSample(sampleFile: File): BinLearnedProfile? {
    if (!sampleFile.exists() || !sampleFile.canRead() || sampleFile.length() == 0L) return null
    val fileSize = sampleFile.length()
    val readSize = minOf(fileSize, 256L * 1024).toInt()
    val sampleBuffer = ByteArray(readSize)

    sampleFile.inputStream().use { stream ->
      stream.read(sampleBuffer)
    }

    val analysis = analyzeBuffer(sampleBuffer, readSize)

    val headerHex = if (readSize >= 8) {
      HashUtils.toHex(sampleBuffer.copyOfRange(0, 8))
    } else {
      HashUtils.toHex(sampleBuffer)
    }

    val footerHex = if (readSize >= 8) {
      HashUtils.toHex(sampleBuffer.copyOfRange(readSize - 8, readSize))
    } else {
      ""
    }

    val sha256 = HashUtils.sha256(sampleFile)

    return BinLearnedProfile(
      detectedHeaderHex = headerHex,
      detectedFooterHex = footerHex,
      estimatedStride = analysis.detectedRecordStride ?: 0,
      averageEntropy = analysis.entropy,
      printableRatio = analysis.printableStringDensity,
      nullRatio = analysis.nullByteRatio,
      sampleSizeBytes = fileSize,
      sampleSha256 = sha256
    )
  }

  fun matchLearnedProfile(candidateBytes: ByteArray, profile: BinLearnedProfile): Double {
    if (candidateBytes.isEmpty()) return 0.0
    var score = 0.0

    // Header match
    val learnedHeader = HashUtils.bytesFromHex(profile.detectedHeaderHex)
    if (learnedHeader.isNotEmpty() && candidateBytes.size >= learnedHeader.size) {
      var headerMatch = true
      for (i in learnedHeader.indices) {
        if (candidateBytes[i] != learnedHeader[i]) {
          headerMatch = false
          break
        }
      }
      if (headerMatch) score += 40.0
    }

    // Entropy tolerance match (+- 1.0)
    val candidateEntropy = NativeForensicScanner.calculateEntropy(candidateBytes)
    val entropyDelta = abs(candidateEntropy - profile.averageEntropy)
    if (entropyDelta < 0.5) {
      score += 25.0
    } else if (entropyDelta < 1.0) {
      score += 15.0
    }

    // Stride match
    if (profile.estimatedStride > 0) {
      val candidateStride = detectStride(candidateBytes, candidateBytes.size)
      if (candidateStride == profile.estimatedStride) {
        score += 20.0
      }
    }

    // Size approximation (within 20%)
    if (profile.sampleSizeBytes > 0) {
      val ratio = candidateBytes.size.toDouble() / profile.sampleSizeBytes
      if (ratio in 0.8..1.2) {
        score += 15.0
      }
    }

    return minOf(100.0, score)
  }

  private fun detectStride(data: ByteArray, len: Int): Int? {
    val candidateStrides = listOf(16, 32, 64, 128, 256, 512, 1024)
    for (stride in candidateStrides) {
      if (len >= stride * 3) {
        var matches = 0
        var total = 0
        for (i in 0 until (len - stride) step stride) {
          // Compare 4-byte prefix of consecutive records
          for (k in 0 until 4) {
            if (i + stride + k < len) {
              total++
              if (data[i + k] == data[i + stride + k]) {
                matches++
              }
            }
          }
        }
        if (total > 0 && (matches.toDouble() / total) >= 0.65) {
          return stride
        }
      }
    }
    return null
  }

  private fun extractPrintableStrings(data: ByteArray, len: Int, minLength: Int, limit: Int): List<String> {
    val strings = mutableListOf<String>()
    val current = StringBuilder()
    for (i in 0 until len) {
      val b = data[i].toInt() and 0xFF
      if (b in 32..126) {
        current.append(b.toChar())
      } else {
        if (current.length >= minLength) {
          strings.add(current.toString())
          if (strings.size >= limit) return strings
        }
        current.clear()
      }
    }
    if (current.length >= minLength && strings.size < limit) {
      strings.add(current.toString())
    }
    return strings
  }
}
