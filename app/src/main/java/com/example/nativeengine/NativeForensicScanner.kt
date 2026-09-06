package com.example.nativeengine

import com.example.core.HashUtils
import java.util.zip.CRC32
import kotlin.math.ln

object NativeForensicScanner {

  private var isNativeAvailable = false

  init {
    try {
      System.loadLibrary("aegis_forensics")
      isNativeAvailable = true
    } catch (_: UnsatisfiedLinkError) {
      isNativeAvailable = false
    } catch (_: Exception) {
      isNativeAvailable = false
    }
  }

  fun isAccelerated(): Boolean = isNativeAvailable

  fun calculateEntropy(data: ByteArray, length: Int = data.size): Double {
    if (length <= 0) return 0.0
    if (isNativeAvailable) {
      try {
        return nativeCalculateEntropy(data, length)
      } catch (_: Exception) {}
    }
    // High-performance JVM Shannon entropy
    val counts = IntArray(256)
    val actualLen = minOf(data.size, length)
    for (i in 0 until actualLen) {
      counts[data[i].toInt() and 0xFF]++
    }
    var entropy = 0.0
    val total = actualLen.toDouble()
    val log2Const = ln(2.0)
    for (c in counts) {
      if (c > 0) {
        val p = c / total
        entropy -= p * (ln(p) / log2Const)
      }
    }
    return entropy
  }

  fun calculateCrc32(data: ByteArray, length: Int = data.size): Long {
    if (isNativeAvailable) {
      try {
        return nativeCalculateCrc32(data, length).toLong() and 0xFFFFFFFFL
      } catch (_: Exception) {}
    }
    return HashUtils.crc32(data, 0, minOf(data.size, length))
  }

  fun findPattern(buffer: ByteArray, bufferLen: Int, pattern: ByteArray): Long {
    if (pattern.isEmpty() || bufferLen < pattern.size) return -1L
    if (isNativeAvailable) {
      try {
        return nativeFindPattern(buffer, bufferLen, pattern, pattern.size)
      } catch (_: Exception) {}
    }
    // Boyer-Moore / fast scan fallback
    val first = pattern[0]
    val maxScan = bufferLen - pattern.size
    for (i in 0..maxScan) {
      if (buffer[i] == first) {
        var match = true
        for (j in 1 until pattern.size) {
          if (buffer[i + j] != pattern[j]) {
            match = false
            break
          }
        }
        if (match) return i.toLong()
      }
    }
    return -1L
  }

  // JNI Native Declarations
  private external fun nativeCalculateEntropy(data: ByteArray, len: Int): Double
  private external fun nativeCalculateCrc32(data: ByteArray, len: Int): Int
  private external fun nativeFindPattern(buffer: ByteArray, bufLen: Int, pattern: ByteArray, patLen: Int): Long
}
