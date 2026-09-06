package com.example.carver

import com.example.binanalysis.BinForensicAnalyzer
import com.example.confidence.ConfidenceEngine
import com.example.core.HashUtils
import com.example.model.RecoveryCandidate
import com.example.model.ValidationState
import com.example.nativeengine.NativeForensicScanner
import java.io.File
import java.io.InputStream
import java.util.UUID

object FileCarver {

  const val CHUNK_SIZE = 128 * 1024 // 128 KB buffer
  const val OVERLAP_SIZE = 4 * 1024 // 4 KB overlap for signatures across buffer boundaries

  fun carveFromStream(
    inputStream: InputStream,
    sessionId: String,
    sourceDescriptor: String,
    maxScanBytes: Long = 50L * 1024 * 1024,
    onProgress: (bytesScanned: Long, candidatesFound: Int) -> Unit
  ): List<RecoveryCandidate> {
    val candidates = mutableListOf<RecoveryCandidate>()
    val buffer = ByteArray(CHUNK_SIZE)
    var overlap = ByteArray(0)
    var totalBytesScanned = 0L

    while (totalBytesScanned < maxScanBytes) {
      val readLen = inputStream.read(buffer)
      if (readLen <= 0) break

      // Combine overlap from previous block with new block
      val workingBlock = if (overlap.isNotEmpty()) {
        val combined = ByteArray(overlap.size + readLen)
        System.arraycopy(overlap, 0, combined, 0, overlap.size)
        System.arraycopy(buffer, 0, combined, overlap.size, readLen)
        combined
      } else {
        buffer.copyOf(readLen)
      }

      val blockStartOffset = totalBytesScanned - overlap.size

      // Scan for signatures
      var searchOffset = 0
      while (searchOffset < workingBlock.size - 8) {
        val match = SignatureRegistry.findMatchingHeader(workingBlock, searchOffset)
        if (match != null) {
          val absoluteStart = blockStartOffset + searchOffset
          val remainingInBlock = workingBlock.size - searchOffset

          // Determine candidate length
          var candidateLength = minOf(remainingInBlock.toLong(), match.maxExpectedSize)
          var footerFound = false

          if (match.footer != null) {
            val footerIdx = NativeForensicScanner.findPattern(
              workingBlock.copyOfRange(searchOffset, workingBlock.size),
              remainingInBlock,
              match.footer
            )
            if (footerIdx != -1L) {
              candidateLength = footerIdx + match.footer.size
              footerFound = true
            }
          }

          // Extract candidate byte snippet
          val snippetLen = minOf(candidateLength.toInt(), remainingInBlock)
          val candidateSlice = workingBlock.copyOfRange(searchOffset, searchOffset + snippetLen)

          val entropy = NativeForensicScanner.calculateEntropy(candidateSlice)
          val crc32 = NativeForensicScanner.calculateCrc32(candidateSlice)
          val sha256 = HashUtils.sha256(candidateSlice)

          var nullCount = 0
          for (b in candidateSlice) {
            if (b.toInt() == 0) nullCount++
          }
          val nullRatio = if (candidateSlice.isNotEmpty()) nullCount.toDouble() / candidateSlice.size else 1.0

          // Evaluate confidence
          val evaluation = ConfidenceEngine.evaluate(
            hasValidHeader = true,
            hasValidFooter = footerFound,
            hasStructuralContinuity = true,
            hasFilesystemMetadataMatch = false,
            hasChecksumValidation = crc32 != 0L,
            hasExpectedSizeMatch = candidateLength > 64,
            entropy = entropy,
            nullRatio = nullRatio,
            isFragmented = false
          )

          // Anti-false positive rejection
          if (!evaluation.isFalsePositiveRisk && evaluation.totalScore >= 35.0) {
            val snippetHex = HashUtils.toHex(candidateSlice.copyOf(minOf(candidateSlice.size, 32)))

            val candidate = RecoveryCandidate(
              id = UUID.randomUUID().toString(),
              sessionId = sessionId,
              originalPath = "$sourceDescriptor:0x${absoluteStart.toString(16).uppercase()}",
              suggestedFilename = "RECOVERED_${sessionId.take(6)}_0x${absoluteStart.toString(16).uppercase()}.${match.extension}",
              extension = match.extension,
              mimeGuess = match.mimeType,
              startOffset = absoluteStart,
              endOffset = absoluteStart + candidateLength,
              size = candidateLength,
              fragmentCount = 1,
              confidenceScore = evaluation.totalScore,
              confidenceBreakdown = evaluation.signalsSummary,
              validationState = evaluation.validationState,
              sha256 = sha256,
              crc32 = crc32,
              entropy = entropy,
              previewSnippetHex = snippetHex,
              notes = "Carved signature: ${match.name}"
            )
            candidates.add(candidate)
          }

          // Advance past signature
          searchOffset += maxOf(1, match.header.size)
        } else {
          searchOffset++
        }
      }

      totalBytesScanned += readLen
      onProgress(totalBytesScanned, candidates.size)

      // Save overlap for next buffer
      overlap = if (readLen >= OVERLAP_SIZE) {
        buffer.copyOfRange(readLen - OVERLAP_SIZE, readLen)
      } else {
        buffer.copyOf(readLen)
      }
    }

    return candidates
  }
}
