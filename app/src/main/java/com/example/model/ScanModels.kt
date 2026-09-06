package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanLevel(val title: String, val description: String) {
  FAST("FAST", "Metadata, MediaStore, accessible public directories, known extensions"),
  BALANCED("BALANCED", "Fast + Hidden artifacts, dot-files, SQLite WAL/journal analysis, thumbnails"),
  DEEP("DEEP", "Balanced + Deep signature carving, unallocated buffers, entropy profiling"),
  FORENSIC("FORENSIC", "Exhaustive multi-pass carving, fragment reconstruction, cryptographic hashing")
}

enum class ScanStatus {
  IDLE,
  SCANNING,
  PAUSED,
  COMPLETED,
  CANCELLED,
  FAILED
}

enum class ValidationState {
  VALIDATED,      // Passed header, structural checks, length consistency, and integrity
  PROBABLE,       // Valid header, sensible entropy and size, missing footer or checksum
  FRAGMENTARY,    // Fragmented or partial file with missing continuous blocks
  CORRUPTED,      // Header signature found but internal records fail structure test
  REJECTED        // High false positive likelihood (random entropy noise or zero-filled)
}

@Entity(tableName = "scan_sessions")
data class ScanSession(
  @PrimaryKey val id: String,
  val scanType: String,
  val scanLevel: ScanLevel,
  val targetPath: String,
  val startTime: Long,
  val endTime: Long? = null,
  val status: ScanStatus,
  val processedBytes: Long,
  val totalBytes: Long,
  val candidatesFound: Int,
  val validatedCount: Int,
  val filesystem: String,
  val deviceModel: String,
  val androidVersion: String,
  val rootActive: Boolean,
  val sourceHash: String = "",
  val scannerVersion: String = "1.0.0-PROD"
)

@Entity(tableName = "recovery_candidates")
data class RecoveryCandidate(
  @PrimaryKey val id: String,
  val sessionId: String,
  val originalPath: String?,
  val suggestedFilename: String,
  val extension: String,
  val mimeGuess: String,
  val startOffset: Long,
  val endOffset: Long,
  val size: Long,
  val fragmentCount: Int,
  val confidenceScore: Double,      // 0.0 to 100.0
  val confidenceBreakdown: String,  // Explanation of signals
  val validationState: ValidationState,
  val sha256: String,
  val crc32: Long,
  val entropy: Double,
  val isRecovered: Boolean = false,
  val recoveredPath: String? = null,
  val previewSnippetHex: String = "",
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntry(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val action: String,
  val target: String,
  val sha256Evidence: String,
  val operator: String = "Aegis Forensic Operator",
  val details: String
)

data class ScanProgress(
  val phase: String,
  val bytesProcessed: Long,
  val totalBytes: Long,
  val percentage: Float,
  val throughputBytesPerSec: Long,
  val candidatesFound: Int,
  val validCandidates: Int,
  val currentTarget: String
)

data class BinRecoveryConfig(
  val knownFilename: String = "",
  val targetFolderPath: String = "",
  val approximateSizeBytes: Long? = null,
  val sampleBinPath: String? = null,
  val knownHeaderHex: String = "",
  val knownFooterHex: String = "",
  val customHexPattern: String = "",
  val expectedRecordStride: Int? = null,
  val entropyMin: Double = 2.0,
  val entropyMax: Double = 7.9
)

data class BinLearnedProfile(
  val detectedHeaderHex: String,
  val detectedFooterHex: String,
  val estimatedStride: Int,
  val averageEntropy: Double,
  val printableRatio: Double,
  val nullRatio: Double,
  val sampleSizeBytes: Long,
  val sampleSha256: String
)
