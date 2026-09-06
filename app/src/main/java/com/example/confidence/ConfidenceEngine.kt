package com.example.confidence

import com.example.model.ValidationState

data class ConfidenceAssessment(
  val totalScore: Double,          // 0.0 to 100.0
  val validationState: ValidationState,
  val signalsSummary: String,
  val isFalsePositiveRisk: Boolean
)

object ConfidenceEngine {

  fun evaluate(
    hasValidHeader: Boolean,
    hasValidFooter: Boolean,
    hasStructuralContinuity: Boolean,
    hasFilesystemMetadataMatch: Boolean,
    hasChecksumValidation: Boolean,
    hasExpectedSizeMatch: Boolean,
    entropy: Double,
    nullRatio: Double,
    isFragmented: Boolean,
    missingFragmentPercent: Double = 0.0
  ): ConfidenceAssessment {
    var score = 0.0
    val signals = mutableListOf<String>()

    // Header Signal (up to 30 pts)
    if (hasValidHeader) {
      score += 30.0
      signals.add("+ Valid signature/header verified (+30%)")
    } else {
      signals.add("- Missing or unverified header signature")
    }

    // Footer Signal (up to 20 pts)
    if (hasValidFooter) {
      score += 20.0
      signals.add("+ Complete footer/trailer boundary detected (+20%)")
    }

    // Structural Consistency (up to 20 pts)
    if (hasStructuralContinuity) {
      score += 20.0
      signals.add("+ Internal structure & record alignment consistent (+20%)")
    }

    // Filesystem Metadata / Directory Remnant (up to 15 pts)
    if (hasFilesystemMetadataMatch) {
      score += 15.0
      signals.add("+ File system metadata / directory entry matched (+15%)")
    }

    // Checksum verification (up to 10 pts)
    if (hasChecksumValidation) {
      score += 10.0
      signals.add("+ Embedded/calculated checksum integrity valid (+10%)")
    }

    // Expected size match (up to 5 pts)
    if (hasExpectedSizeMatch) {
      score += 5.0
      signals.add("+ Size matches expected allocation boundaries (+5%)")
    }

    // Anti-false positive penalties
    var falsePositiveRisk = false

    // Zero-fill / blank area filter
    if (nullRatio > 0.95) {
      score = minOf(score, 10.0)
      falsePositiveRisk = true
      signals.add("! WARNING: Region contains >95% null bytes (sparse or zeroed storage)")
    }

    // Flat entropy filter (e.g. constant byte pattern like 0xFF 0xFF...)
    if (entropy < 0.5 && nullRatio < 0.90) {
      score = minOf(score, 15.0)
      falsePositiveRisk = true
      signals.add("! WARNING: Very low entropy (<0.5) indicating repetitive byte padding")
    }

    // Fragment penalty
    if (isFragmented && missingFragmentPercent > 0.0) {
      val penalty = minOf(35.0, missingFragmentPercent * 0.7)
      score = maxOf(5.0, score - penalty)
      signals.add(String.format("- %.1f%% region missing or unallocated (-%.1f%%)", missingFragmentPercent, penalty))
    }

    val finalScore = minOf(99.0, maxOf(0.0, score)) // Cap at 99.0 unless physically certified

    val validationState = when {
      finalScore >= 85.0 && !falsePositiveRisk && !isFragmented -> ValidationState.VALIDATED
      finalScore >= 60.0 && !falsePositiveRisk -> ValidationState.PROBABLE
      isFragmented && finalScore >= 40.0 -> ValidationState.FRAGMENTARY
      falsePositiveRisk || finalScore < 30.0 -> ValidationState.REJECTED
      else -> ValidationState.CORRUPTED
    }

    return ConfidenceAssessment(
      totalScore = finalScore,
      validationState = validationState,
      signalsSummary = signals.joinToString("\n"),
      isFalsePositiveRisk = falsePositiveRisk
    )
  }
}
