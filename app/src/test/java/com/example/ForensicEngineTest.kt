package com.example

import com.example.binanalysis.BinForensicAnalyzer
import com.example.carver.FileSignature
import com.example.carver.SignatureRegistry
import com.example.confidence.ConfidenceEngine
import com.example.core.HashUtils
import com.example.model.ValidationState
import com.example.nativeengine.NativeForensicScanner
import com.example.reconstruction.FragmentReconstructor
import com.example.reconstruction.FragmentSlice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForensicEngineTest {

  @Test
  fun testHashCalculations() {
    val sample = "FORENSIC_EVIDENCE_PAYLOAD_TEST".toByteArray(Charsets.UTF_8)
    val sha256 = HashUtils.sha256(sample)
    assertEquals(64, sha256.length)

    val crc = HashUtils.crc32(sample)
    assertTrue(crc != 0L)
  }

  @Test
  fun testFormatFileSize() {
    assertEquals("0 B", HashUtils.formatFileSize(0))
    assertEquals("512 B", HashUtils.formatFileSize(512))
    assertEquals("1.0 KB", HashUtils.formatFileSize(1024))
    assertEquals("15.5 KB", HashUtils.formatFileSize((15.5 * 1024).toLong()))
    assertEquals("1.00 MB", HashUtils.formatFileSize(1024 * 1024))
    assertEquals("4.25 MB", HashUtils.formatFileSize((4.25 * 1024 * 1024).toLong()))
    assertEquals("1.50 GB", HashUtils.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
  }

  @Test
  fun testEntropyCalculation() {
    // Flat repeated byte has 0 entropy
    val zeros = ByteArray(1024) { 0 }
    val zeroEntropy = NativeForensicScanner.calculateEntropy(zeros)
    assertEquals(0.0, zeroEntropy, 0.001)

    // Evenly distributed byte sequence approaches 8.0
    val distributed = ByteArray(256) { it.toByte() }
    val maxEntropy = NativeForensicScanner.calculateEntropy(distributed)
    assertTrue(maxEntropy > 7.9)
  }

  @Test
  fun testSignatureMatching() {
    val jpegHeader = HashUtils.bytesFromHex("FF D8 FF E0 00 10 4A 46 49 46")
    val match = SignatureRegistry.findMatchingHeader(jpegHeader, 0)
    assertNotNull(match)
    assertEquals("jpg", match?.extension)
  }

  @Test
  fun testConfidenceAssessment() {
    // Valid file with signature, checksum and structure
    val validAssessment = ConfidenceEngine.evaluate(
      hasValidHeader = true,
      hasValidFooter = true,
      hasStructuralContinuity = true,
      hasFilesystemMetadataMatch = true,
      hasChecksumValidation = true,
      hasExpectedSizeMatch = true,
      entropy = 6.2,
      nullRatio = 0.05,
      isFragmented = false
    )
    assertTrue(validAssessment.totalScore >= 85.0)
    assertEquals(ValidationState.VALIDATED, validAssessment.validationState)
    assertFalse(validAssessment.isFalsePositiveRisk)

    // Zero-filled block should be rejected as false positive
    val zeroBlockAssessment = ConfidenceEngine.evaluate(
      hasValidHeader = false,
      hasValidFooter = false,
      hasStructuralContinuity = false,
      hasFilesystemMetadataMatch = false,
      hasChecksumValidation = false,
      hasExpectedSizeMatch = false,
      entropy = 0.0,
      nullRatio = 0.99,
      isFragmented = false
    )
    assertTrue(zeroBlockAssessment.isFalsePositiveRisk)
    assertEquals(ValidationState.REJECTED, zeroBlockAssessment.validationState)
  }

  @Test
  fun testFragmentReconstructor() {
    val chunk1 = "CHUNK_PART_1_EVIDENCE_RECORD".toByteArray(Charsets.UTF_8)
    val chunk2 = "CHUNK_PART_2_EVIDENCE_RECORD".toByteArray(Charsets.UTF_8)

    val slice1 = FragmentSlice(
      offset = 0,
      length = chunk1.size,
      entropy = NativeForensicScanner.calculateEntropy(chunk1),
      crc32 = HashUtils.crc32(chunk1),
      data = chunk1
    )

    val slice2 = FragmentSlice(
      offset = chunk1.size.toLong() + 1024, // 1024 byte missing unallocated gap
      length = chunk2.size,
      entropy = NativeForensicScanner.calculateEntropy(chunk2),
      crc32 = HashUtils.crc32(chunk2),
      data = chunk2
    )

    val result = FragmentReconstructor.reconstructFragments(listOf(slice1, slice2))
    assertFalse(result.isContiguous)
    assertEquals(1024L, result.missingByteGaps)
    assertEquals((chunk1.size + chunk2.size).toLong(), result.totalRecoveredBytes)
    assertTrue(result.reconstructionMap.contains("GAP"))
  }

  @Test
  fun testBinAnalyzer() {
    val structuredData = ByteArray(512) { (it % 32).toByte() }
    val analysis = BinForensicAnalyzer.analyzeBuffer(structuredData)
    assertNotNull(analysis.potentialHeaderHex)
    assertTrue(analysis.inferredAlignment > 0)
  }
}
