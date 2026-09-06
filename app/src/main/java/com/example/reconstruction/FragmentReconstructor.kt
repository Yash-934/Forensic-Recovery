package com.example.reconstruction

import com.example.core.HashUtils
import com.example.nativeengine.NativeForensicScanner
import kotlin.math.abs

data class FragmentSlice(
  val offset: Long,
  val length: Int,
  val entropy: Double,
  val crc32: Long,
  val data: ByteArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FragmentSlice) return false
    return offset == other.offset && length == other.length && crc32 == other.crc32
  }

  override fun hashCode(): Int {
    var result = offset.hashCode()
    result = 31 * result + length
    result = 31 * result + crc32.hashCode()
    return result
  }
}

data class ReconstructionResult(
  val totalRecoveredBytes: Long,
  val missingByteGaps: Long,
  val fragmentCount: Int,
  val continuityScore: Double,
  val combinedBytes: ByteArray,
  val reconstructionMap: String,
  val isContiguous: Boolean
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ReconstructionResult) return false
    return totalRecoveredBytes == other.totalRecoveredBytes &&
        missingByteGaps == other.missingByteGaps &&
        combinedBytes.contentEquals(other.combinedBytes)
  }

  override fun hashCode(): Int {
    var result = totalRecoveredBytes.hashCode()
    result = 31 * result + missingByteGaps.hashCode()
    result = 31 * result + combinedBytes.contentHashCode()
    return result
  }
}

object FragmentReconstructor {

  fun analyzeContinuity(sliceA: FragmentSlice, sliceB: FragmentSlice): Double {
    // 1. Distance check
    val expectedNextOffset = sliceA.offset + sliceA.length
    val offsetDiff = sliceB.offset - expectedNextOffset

    var score = 50.0 // Base score

    if (offsetDiff == 0L) {
      score += 30.0 // Contiguous in storage
    } else if (offsetDiff in 1..4096) {
      score += 10.0 // Close block
    } else {
      score -= 15.0 // Non-contiguous jump
    }

    // 2. Entropy similarity
    val entDiff = abs(sliceA.entropy - sliceB.entropy)
    if (entDiff < 0.4) {
      score += 20.0
    } else if (entDiff < 1.0) {
      score += 10.0
    } else {
      score -= 10.0
    }

    return minOf(100.0, maxOf(0.0, score))
  }

  fun reconstructFragments(slices: List<FragmentSlice>): ReconstructionResult {
    if (slices.isEmpty()) {
      return ReconstructionResult(
        totalRecoveredBytes = 0,
        missingByteGaps = 0,
        fragmentCount = 0,
        continuityScore = 0.0,
        combinedBytes = ByteArray(0),
        reconstructionMap = "No fragments provided.",
        isContiguous = true
      )
    }

    val sorted = slices.sortedBy { it.offset }
    var totalBytes = 0L
    var missingGaps = 0L
    val mapBuilder = StringBuilder()
    var isContiguous = true
    var totalContinuity = 0.0

    // Measure total size required
    for (i in sorted.indices) {
      val slice = sorted[i]
      totalBytes += slice.length
      mapBuilder.append(
        String.format(
          "[Fragment %d: Offset 0x%X..0x%X (%d bytes, Entropy: %.2f)]\n",
          i + 1, slice.offset, slice.offset + slice.length, slice.length, slice.entropy
        )
      )

      if (i < sorted.size - 1) {
        val next = sorted[i + 1]
        val expected = slice.offset + slice.length
        if (next.offset > expected) {
          val gap = next.offset - expected
          missingGaps += gap
          isContiguous = false
          mapBuilder.append(String.format("  --> [GAP: %d missing unallocated bytes (NOT invented)]\n", gap))
        }
        totalContinuity += analyzeContinuity(slice, next)
      }
    }

    val avgContinuity = if (sorted.size > 1) totalContinuity / (sorted.size - 1) else 100.0

    // Assemble bytes safely without inventing data for missing gaps
    val combinedSize = sorted.sumOf { it.length }
    val combined = ByteArray(combinedSize)
    var writePos = 0
    for (s in sorted) {
      System.arraycopy(s.data, 0, combined, writePos, s.length)
      writePos += s.length
    }

    return ReconstructionResult(
      totalRecoveredBytes = totalBytes,
      missingByteGaps = missingGaps,
      fragmentCount = sorted.size,
      continuityScore = avgContinuity,
      combinedBytes = combined,
      reconstructionMap = mapBuilder.toString(),
      isContiguous = isContiguous
    )
  }
}
