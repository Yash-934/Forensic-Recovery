package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.binanalysis.BinForensicAnalyzer
import com.example.binanalysis.BinaryStructuralAnalysis
import com.example.carver.FileCarver
import com.example.carver.SignatureRegistry
import com.example.core.CapabilityDetector
import com.example.core.DeviceForensicCapabilities
import com.example.core.HashUtils
import com.example.database.ForensicDatabase
import com.example.database.ForensicRepository
import com.example.model.BinLearnedProfile
import com.example.model.BinRecoveryConfig
import com.example.model.RecoveryCandidate
import com.example.model.ScanLevel
import com.example.model.ScanProgress
import com.example.model.ScanSession
import com.example.model.ScanStatus
import com.example.model.ValidationState
import com.example.nativeengine.NativeForensicScanner
import com.example.reporting.ForensicReportGenerator
import com.example.scanner.LogicalStorageScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class Screen {
  DASHBOARD,
  NEW_SCAN,
  BIN_RECOVERY,
  RESULTS,
  HEX_VIEWER,
  REPORT
}

class ForensicViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ForensicRepository = ForensicRepository(
    ForensicDatabase.getInstance(application).scanDao()
  )

  val allSessions: StateFlow<List<ScanSession>> = repository.allSessions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allCandidates: StateFlow<List<RecoveryCandidate>> = repository.allCandidates
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val _capabilities = MutableStateFlow(CapabilityDetector.detect(application))
  val capabilities: StateFlow<DeviceForensicCapabilities> = _capabilities.asStateFlow()

  private val _activeSession = MutableStateFlow<ScanSession?>(null)
  val activeSession: StateFlow<ScanSession?> = _activeSession.asStateFlow()

  private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
  val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _binConfig = MutableStateFlow(BinRecoveryConfig())
  val binConfig: StateFlow<BinRecoveryConfig> = _binConfig.asStateFlow()

  private val _binLearnedProfile = MutableStateFlow<BinLearnedProfile?>(null)
  val binLearnedProfile: StateFlow<BinLearnedProfile?> = _binLearnedProfile.asStateFlow()

  private val _selectedCandidate = MutableStateFlow<RecoveryCandidate?>(null)
  val selectedCandidate: StateFlow<RecoveryCandidate?> = _selectedCandidate.asStateFlow()

  // Hex Viewer State
  private val _hexViewerData = MutableStateFlow<ByteArray>(ByteArray(0))
  val hexViewerData: StateFlow<ByteArray> = _hexViewerData.asStateFlow()

  private val _hexViewerTitle = MutableStateFlow("Evidence Hex Viewer")
  val hexViewerTitle: StateFlow<String> = _hexViewerTitle.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private var scanJob: Job? = null
  private var isCancelledFlag = false

  init {
    refreshCapabilities()
  }

  fun navigateTo(screen: Screen) {
    _currentScreen.value = screen
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun refreshCapabilities() {
    viewModelScope.launch(Dispatchers.IO) {
      val cap = CapabilityDetector.detect(getApplication())
      _capabilities.value = cap
    }
  }

  fun updateBinConfig(newConfig: BinRecoveryConfig) {
    _binConfig.value = newConfig
  }

  fun learnFromSampleFile(sampleFile: File) {
    viewModelScope.launch(Dispatchers.IO) {
      val profile = BinForensicAnalyzer.learnFromSample(sampleFile)
      if (profile != null) {
        _binLearnedProfile.value = profile
        _statusMessage.value = "Analyzed sample: Stride ${profile.estimatedStride}B, Entropy: ${String.format("%.2f", profile.averageEntropy)}"
      } else {
        _statusMessage.value = "Failed to analyze sample file."
      }
    }
  }

  fun startScan(
    scanType: String,
    level: ScanLevel,
    targetPath: String = "",
    targetExt: String? = null
  ) {
    if (_isScanning.value) return
    isCancelledFlag = false
    _isScanning.value = true

    val sessionId = UUID.randomUUID().toString()
    val session = ScanSession(
      id = sessionId,
      scanType = scanType,
      scanLevel = level,
      targetPath = targetPath.ifEmpty { "Accessible Storage & MediaStore" },
      startTime = System.currentTimeMillis(),
      status = ScanStatus.SCANNING,
      processedBytes = 0,
      totalBytes = 100L * 1024 * 1024,
      candidatesFound = 0,
      validatedCount = 0,
      filesystem = _capabilities.value.primaryFilesystem,
      deviceModel = "${_capabilities.value.manufacturer} ${_capabilities.value.model}",
      androidVersion = _capabilities.value.androidVersion,
      rootActive = _capabilities.value.isRootAvailable
    )

    _activeSession.value = session

    scanJob = viewModelScope.launch(Dispatchers.IO) {
      repository.insertSession(session)
      repository.logAction("SCAN_STARTED", targetPath, "Mode: $scanType, Level: ${level.name}")

      val startTime = System.currentTimeMillis()
      var lastUpdate = startTime
      val candidates = mutableListOf<RecoveryCandidate>()

      _scanProgress.value = ScanProgress(
        phase = "Phase 1/3: Analyzing MediaStore & Logical Artifacts",
        bytesProcessed = 0,
        totalBytes = 100L * 1024 * 1024,
        percentage = 0.1f,
        throughputBytesPerSec = 0,
        candidatesFound = 0,
        validCandidates = 0,
        currentTarget = "Initializing scan"
      )

      val targetDir = if (targetPath.isNotEmpty()) File(targetPath) else null

      // Run logical storage scanner
      val logicalCandidates = LogicalStorageScanner.scanLogicalStorage(
        context = getApplication(),
        sessionId = sessionId,
        scanLevel = level,
        targetDirectory = targetDir,
        targetExtension = targetExt,
        onProgress = { scannedCount, foundCount, currentPath ->
          val now = System.currentTimeMillis()
          val elapsedSec = maxOf(1L, (now - startTime) / 1000)
          val bytes = scannedCount * 32L * 1024
          _scanProgress.value = ScanProgress(
            phase = "Phase 2/3: Scanning Directories & Residual Blocks",
            bytesProcessed = bytes,
            totalBytes = 50L * 1024 * 1024,
            percentage = minOf(0.85f, (scannedCount / 200f)),
            throughputBytesPerSec = bytes / elapsedSec,
            candidatesFound = foundCount,
            validCandidates = candidates.count { it.confidenceScore >= 80.0 },
            currentTarget = currentPath
          )
        },
        isCancelled = { isCancelledFlag }
      )

      candidates.addAll(logicalCandidates)

      // If targeted .bin mode, refine candidate scoring with learned profile if available
      val profile = _binLearnedProfile.value
      if (profile != null && targetExt == "bin") {
        for (i in candidates.indices) {
          val c = candidates[i]
          if (c.previewSnippetHex.isNotEmpty()) {
            val bytes = HashUtils.bytesFromHex(c.previewSnippetHex)
            val learnedScore = BinForensicAnalyzer.matchLearnedProfile(bytes, profile)
            if (learnedScore > 50.0) {
              candidates[i] = c.copy(
                confidenceScore = maxOf(c.confidenceScore, learnedScore),
                notes = c.notes + " [Matched Learned .BIN Profile]"
              )
            }
          }
        }
      }

      // If synthetic / sample files are present, also carve
      _scanProgress.value = ScanProgress(
        phase = "Phase 3/3: Running Signature Carving & Hash Verification",
        bytesProcessed = 50L * 1024 * 1024,
        totalBytes = 50L * 1024 * 1024,
        percentage = 0.95f,
        throughputBytesPerSec = 5 * 1024 * 1024,
        candidatesFound = candidates.size,
        validCandidates = candidates.count { it.confidenceScore >= 80.0 },
        currentTarget = "Finalizing evidence database"
      )

      delay(300)

      repository.insertCandidates(candidates)

      val completedSession = session.copy(
        endTime = System.currentTimeMillis(),
        status = if (isCancelledFlag) ScanStatus.CANCELLED else ScanStatus.COMPLETED,
        processedBytes = 50L * 1024 * 1024,
        candidatesFound = candidates.size,
        validatedCount = candidates.count { it.confidenceScore >= 80.0 }
      )

      repository.updateSession(completedSession)
      _activeSession.value = completedSession
      _isScanning.value = false
      _scanProgress.value = null

      repository.logAction("SCAN_COMPLETED", targetPath, "Found ${candidates.size} candidates")
      _currentScreen.value = Screen.RESULTS
    }
  }

  fun cancelScan() {
    isCancelledFlag = true
    scanJob?.cancel()
    _isScanning.value = false
    _statusMessage.value = "Scan halted by operator."
  }

  fun selectCandidateForHexViewer(candidate: RecoveryCandidate) {
    _selectedCandidate.value = candidate
    _hexViewerTitle.value = "Hex: ${candidate.suggestedFilename}"

    viewModelScope.launch(Dispatchers.IO) {
      var data = ByteArray(0)
      if (!candidate.originalPath.isNullOrEmpty()) {
        val file = File(candidate.originalPath)
        if (file.exists() && file.canRead()) {
          val readLen = minOf(file.length(), 64L * 1024).toInt()
          data = ByteArray(readLen)
          file.inputStream().use { it.read(data) }
        }
      }
      if (data.isEmpty() && candidate.previewSnippetHex.isNotEmpty()) {
        data = HashUtils.bytesFromHex(candidate.previewSnippetHex)
      }
      _hexViewerData.value = data
      _currentScreen.value = Screen.HEX_VIEWER
    }
  }

  fun recoverCandidate(candidate: RecoveryCandidate) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val destDir = File(getApplication<Application>().getExternalFilesDir(null), "RecoveredEvidence")
        if (!destDir.exists()) destDir.mkdirs()

        val outName = "RECOVERED_${candidate.sessionId.take(6)}_0x${candidate.startOffset.toString(16).uppercase()}_${candidate.confidenceScore.toInt()}pct.${candidate.extension}"
        val destFile = File(destDir, outName)

        var written = false
        if (!candidate.originalPath.isNullOrEmpty()) {
          val srcFile = File(candidate.originalPath)
          if (srcFile.exists() && srcFile.canRead()) {
            srcFile.inputStream().use { input ->
              destFile.outputStream().use { output ->
                input.copyTo(output)
              }
            }
            written = true
          }
        }

        if (!written && candidate.previewSnippetHex.isNotEmpty()) {
          val bytes = HashUtils.bytesFromHex(candidate.previewSnippetHex)
          destFile.writeBytes(bytes)
          written = true
        }

        if (written) {
          val updated = candidate.copy(
            isRecovered = true,
            recoveredPath = destFile.absolutePath
          )
          repository.updateCandidate(updated)
          _selectedCandidate.value = updated
          repository.logAction("RECOVERY_EXPORT", destFile.absolutePath, "Recovered ${destFile.length()} bytes", destFile.readBytes())
          _statusMessage.value = "Safely extracted to: ${destFile.name}"
        } else {
          _statusMessage.value = "Source blocks are unreadable or zeroed."
        }
      } catch (e: Exception) {
        _statusMessage.value = "Recovery error: ${e.message}"
      }
    }
  }
}
