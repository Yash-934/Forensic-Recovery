package com.example.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.binanalysis.BinForensicAnalyzer
import com.example.carver.SignatureRegistry
import com.example.confidence.ConfidenceEngine
import com.example.core.HashUtils
import com.example.model.RecoveryCandidate
import com.example.model.ScanLevel
import com.example.model.ValidationState
import com.example.nativeengine.NativeForensicScanner
import java.io.File
import java.util.UUID

object LogicalStorageScanner {

  fun scanLogicalStorage(
    context: Context,
    sessionId: String,
    scanLevel: ScanLevel,
    targetDirectory: File?,
    targetExtension: String? = null,
    onProgress: (scannedCount: Int, foundCount: Int, currentPath: String) -> Unit,
    isCancelled: () -> Boolean
  ): List<RecoveryCandidate> {
    val candidates = mutableListOf<RecoveryCandidate>()
    var scannedFiles = 0

    // 1. Scan MediaStore deleted / trashed / orphan artifacts (Android 11+ supports IS_TRASHED)
    if (scanLevel != ScanLevel.FAST || targetDirectory == null) {
      scanMediaStoreArtifacts(context, sessionId, candidates)
    }

    // 2. Determine target roots
    val rootsToScan = mutableListOf<File>()
    if (targetDirectory != null && targetDirectory.exists()) {
      rootsToScan.add(targetDirectory)
    } else {
      // Add default accessible locations
      try {
        val ext = Environment.getExternalStorageDirectory()
        if (ext != null && ext.exists()) rootsToScan.add(ext)
      } catch (_: Exception) {}

      context.getExternalFilesDirs(null).filterNotNull().forEach {
        if (it.exists()) rootsToScan.add(it)
      }
    }

    // 3. Recursive directory walker
    val visitedPaths = mutableSetOf<String>()

    for (root in rootsToScan) {
      if (isCancelled()) break
      val queue = ArrayDeque<File>()
      queue.add(root)

      while (queue.isNotEmpty() && !isCancelled()) {
        val current = queue.removeFirst()
        if (!visitedPaths.add(current.absolutePath)) continue

        val children = current.listFiles() ?: continue
        for (child in children) {
          if (isCancelled()) break
          if (child.isDirectory) {
            // Keep queue bounded
            if (queue.size < 500) {
              queue.add(child)
            }
          } else {
            scannedFiles++
            if (scannedFiles % 25 == 0) {
              onProgress(scannedFiles, candidates.size, child.name)
            }
            inspectFileArtifact(child, sessionId, targetExtension, candidates, scanLevel)
          }
        }
      }
    }

    return candidates
  }

  private fun inspectFileArtifact(
    file: File,
    sessionId: String,
    targetExtension: String?,
    outCandidates: MutableList<RecoveryCandidate>,
    scanLevel: ScanLevel
  ) {
    if (!file.canRead() || file.length() == 0L) return

    val name = file.name
    val isHidden = name.startsWith(".")
    val isTrashed = name.contains("trashed", ignoreCase = true) || name.contains("recycle", ignoreCase = true)
    val isBin = name.endsWith(".bin", ignoreCase = true) || (targetExtension != null && name.endsWith(".$targetExtension", ignoreCase = true))
    val isDbArtifact = name.endsWith("-wal") || name.endsWith("-journal") || name.endsWith(".db")

    val matchesFilter = isBin || isHidden || isTrashed || isDbArtifact || targetExtension == null || scanLevel == ScanLevel.FORENSIC

    if (!matchesFilter) return

    try {
      val readLen = minOf(file.length(), 64L * 1024).toInt()
      val headerBytes = ByteArray(readLen)
      file.inputStream().use { it.read(headerBytes) }

      val sig = SignatureRegistry.findMatchingHeader(headerBytes, 0)
      val ext = sig?.extension ?: if (isBin) "bin" else file.extension.ifEmpty { "bin" }
      val mime = sig?.mimeType ?: if (isBin) "application/octet-stream" else "application/unknown"

      val entropy = NativeForensicScanner.calculateEntropy(headerBytes)
      val crc32 = HashUtils.crc32(headerBytes)
      val sha256 = HashUtils.sha256(file)

      var nulls = 0
      for (b in headerBytes) {
        if (b.toInt() == 0) nulls++
      }
      val nullRatio = if (headerBytes.isNotEmpty()) nulls.toDouble() / headerBytes.size else 0.0

      val isStructureValid = if (isBin) {
        val binAnalysis = BinForensicAnalyzer.analyzeBuffer(headerBytes)
        binAnalysis.detectedRecordStride != null || entropy in 2.0..7.9
      } else {
        sig != null
      }

      val evaluation = ConfidenceEngine.evaluate(
        hasValidHeader = sig != null || (isBin && entropy > 2.0),
        hasValidFooter = false,
        hasStructuralContinuity = isStructureValid,
        hasFilesystemMetadataMatch = true,
        hasChecksumValidation = crc32 != 0L,
        hasExpectedSizeMatch = file.length() > 0,
        entropy = entropy,
        nullRatio = nullRatio,
        isFragmented = false
      )

      val snippetHex = HashUtils.toHex(headerBytes.copyOf(minOf(headerBytes.size, 32)))

      val notes = buildString {
        if (isHidden) append("[Hidden dot-file] ")
        if (isTrashed) append("[Recycle/Trash Remnant] ")
        if (isDbArtifact) append("[SQLite Journal/WAL artifact] ")
        if (isBin) append("[Target .BIN candidate] ")
      }

      val candidate = RecoveryCandidate(
        id = UUID.randomUUID().toString(),
        sessionId = sessionId,
        originalPath = file.absolutePath,
        suggestedFilename = file.name,
        extension = ext,
        mimeGuess = mime,
        startOffset = 0,
        endOffset = file.length(),
        size = file.length(),
        fragmentCount = 1,
        confidenceScore = evaluation.totalScore,
        confidenceBreakdown = evaluation.signalsSummary,
        validationState = evaluation.validationState,
        sha256 = sha256,
        crc32 = crc32,
        entropy = entropy,
        previewSnippetHex = snippetHex,
        notes = notes
      )

      outCandidates.add(candidate)
    } catch (_: Exception) {}
  }

  private fun scanMediaStoreArtifacts(
    context: Context,
    sessionId: String,
    outCandidates: MutableList<RecoveryCandidate>
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        val cr: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
          MediaStore.MediaColumns._ID,
          MediaStore.MediaColumns.DISPLAY_NAME,
          MediaStore.MediaColumns.DATA,
          MediaStore.MediaColumns.SIZE,
          MediaStore.MediaColumns.MIME_TYPE,
          MediaStore.MediaColumns.IS_TRASHED
        )
        val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 1"

        cr.query(uri, projection, selection, null, null)?.use { cursor ->
          val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
          val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
          val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
          val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

          while (cursor.moveToNext()) {
            val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "trashed_file" else "trashed_file"
            val path = if (dataIdx != -1) cursor.getString(dataIdx) else null
            val size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L
            val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "application/octet-stream" else "application/octet-stream"

            val candidate = RecoveryCandidate(
              id = UUID.randomUUID().toString(),
              sessionId = sessionId,
              originalPath = path,
              suggestedFilename = name,
              extension = name.substringAfterLast('.', "bin"),
              mimeGuess = mime,
              startOffset = 0,
              endOffset = size,
              size = size,
              fragmentCount = 1,
              confidenceScore = 85.0,
              confidenceBreakdown = "+ MediaStore marked item in system recycle bin (+85% confidence)",
              validationState = ValidationState.PROBABLE,
              sha256 = "MEDIASTORE_TRASHED",
              crc32 = 0L,
              entropy = 5.0,
              previewSnippetHex = "",
              notes = "Recoverable from Android MediaStore Recycle Bin"
            )
            outCandidates.add(candidate)
          }
        }
      } catch (_: Exception) {}
    }
  }
}
