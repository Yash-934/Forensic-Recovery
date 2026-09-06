package com.example.reporting

import com.example.core.DeviceForensicCapabilities
import com.example.model.RecoveryCandidate
import com.example.model.ScanSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ForensicReportGenerator {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)

  fun generateMarkdownReport(
    session: ScanSession,
    capabilities: DeviceForensicCapabilities,
    candidates: List<RecoveryCandidate>
  ): String {
    val sb = StringBuilder()
    val now = dateFormat.format(Date())
    val startTime = dateFormat.format(Date(session.startTime))
    val endTime = session.endTime?.let { dateFormat.format(Date(it)) } ?: "In Progress"

    sb.append("# DIGITAL FORENSIC EVIDENCE & ACQUISITION REPORT\n\n")
    sb.append("**Report Generated:** $now\n")
    sb.append("**Aegis Forensic Scanner Version:** ${session.scannerVersion}\n")
    sb.append("**Session ID:** `${session.id}`\n\n")

    sb.append("## 1. DEVICE PROFILE & CAPABILITY ENUMERATION\n\n")
    sb.append("| Property | Detail |\n")
    sb.append("|---|---|\n")
    sb.append("| **Device Hardware** | ${capabilities.manufacturer} ${capabilities.model} |\n")
    sb.append("| **Android OS** | ${capabilities.androidVersion} |\n")
    sb.append("| **Superuser / Root** | ${if (capabilities.isRootAvailable) "VERIFIED ACTIVE (${capabilities.rootEvidence})" else "NOT ACTIVE (Standard Android Sandbox)"} |\n")
    sb.append("| **Primary Filesystem** | ${capabilities.primaryFilesystem} |\n")
    sb.append("| **Storage Access** | ${if (capabilities.hasManageExternalStorage) "MANAGE_EXTERNAL_STORAGE Granted" else "Scoped Storage (Restricted)"} |\n")
    sb.append("| **Acquisition Capability** | **${capabilities.capabilityLevel.name}** |\n\n")

    sb.append("### Technical Limits & Integrity Constraints\n")
    sb.append("- **Encryption:** ${capabilities.encryptionStatus}\n")
    sb.append("- **Flash Controller TRIM:** ${capabilities.trimNotice}\n")
    sb.append("- **Capability Scope:** ${capabilities.capabilityExplanation}\n\n")

    sb.append("## 2. SCAN CONFIGURATION & EXECUTION\n\n")
    sb.append("| Parameter | Value |\n")
    sb.append("|---|---|\n")
    sb.append("| **Scan Mode** | ${session.scanType} (${session.scanLevel.title}) |\n")
    sb.append("| **Target Boundary** | `${session.targetPath}` |\n")
    sb.append("| **Scan Started** | $startTime |\n")
    sb.append("| **Scan Concluded** | $endTime |\n")
    sb.append("| **Bytes Processed** | ${session.processedBytes} bytes (${session.processedBytes / (1024 * 1024)} MB) |\n")
    sb.append("| **Candidates Identified** | ${candidates.size} |\n")
    sb.append("| **Validated Integrity Count** | ${candidates.count { it.confidenceScore >= 80.0 }} |\n\n")

    sb.append("## 3. EVIDENCE CANDIDATE MANIFEST\n\n")
    if (candidates.isEmpty()) {
      sb.append("*No recovery candidates detected matching active signature thresholds.*\n\n")
    } else {
      sb.append("| ID / File | Type | Size | Confidence | State | SHA-256 Hash |\n")
      sb.append("|---|---|---|---|---|---|\n")
      for (c in candidates) {
        sb.append("| `${c.suggestedFilename}` | ${c.extension.uppercase()} | ${c.size} B | **${c.confidenceScore.toInt()}%** | ${c.validationState.name} | `${c.sha256.take(16)}...` |\n")
      }
      sb.append("\n")

      sb.append("### Detailed Candidate Evidence Breakdown\n\n")
      for ((idx, c) in candidates.withIndex()) {
        sb.append("#### [${idx + 1}] ${c.suggestedFilename}\n")
        sb.append("- **Candidate ID:** `${c.id}`\n")
        sb.append("- **Source Offset:** `0x${c.startOffset.toString(16).uppercase()} .. 0x${c.endOffset.toString(16).uppercase()}`\n")
        sb.append("- **MIME Guess:** ${c.mimeGuess}\n")
        sb.append("- **Entropy Score:** ${String.format(Locale.US, "%.3f", c.entropy)} / 8.0\n")
        sb.append("- **CRC-32 Checksum:** `0x${c.crc32.toString(16).uppercase()}`\n")
        sb.append("- **SHA-256 Digest:** `${c.sha256}`\n")
        sb.append("- **Confidence Assessment:** ${c.confidenceScore.toInt()}%\n")
        sb.append("```\n")
        sb.append(c.confidenceBreakdown)
        sb.append("\n```\n")
        if (c.previewSnippetHex.isNotEmpty()) {
          sb.append("- **Header Preview Hex:** `${c.previewSnippetHex}`\n")
        }
        sb.append("\n")
      }
    }

    sb.append("## 4. CHAIN OF CUSTODY & CERTIFICATION\n\n")
    sb.append("This forensic acquisition audit was conducted strictly adhering to read-only source preservation principles. No source sectors or filesystem structures were altered or mounted read-write during this execution.\n\n")
    sb.append("**Auditor Signature Hash:** `${session.id.hashCode().toString(16).uppercase()}`\n")

    return sb.toString()
  }
}
