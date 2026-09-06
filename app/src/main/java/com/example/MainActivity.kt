package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.screens.BinRecoveryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HexViewerScreen
import com.example.ui.screens.NewScanScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.ForensicNavyDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ForensicViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

  private val viewModel: ForensicViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Request MANAGE_EXTERNAL_STORAGE on Android 11+ if not granted, so logical scanner can inspect full directories
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
          data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
      } catch (_: Exception) {
        try {
          val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
          startActivity(fallback)
        } catch (_: Exception) {}
      }
    }

    setContent {
      MyApplicationTheme {
        ForensicApp(viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshCapabilities()
  }
}

@Composable
fun ForensicApp(viewModel: ForensicViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val capabilities by viewModel.capabilities.collectAsState()
  val activeSession by viewModel.activeSession.collectAsState()
  val scanProgress by viewModel.scanProgress.collectAsState()
  val isScanning by viewModel.isScanning.collectAsState()
  val candidates by viewModel.allCandidates.collectAsState()
  val selectedCandidate by viewModel.selectedCandidate.collectAsState()
  val hexData by viewModel.hexViewerData.collectAsState()
  val hexTitle by viewModel.hexViewerTitle.collectAsState()
  val binConfig by viewModel.binConfig.collectAsState()
  val binProfile by viewModel.binLearnedProfile.collectAsState()
  val statusMessage by viewModel.statusMessage.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(statusMessage) {
    statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = ForensicNavyDark,
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ForensicNavyDark)
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        Screen.DASHBOARD -> DashboardScreen(
          capabilities = capabilities,
          scanProgress = scanProgress,
          isScanning = isScanning,
          candidates = candidates,
          onNavigate = { viewModel.navigateTo(it) },
          onCancelScan = { viewModel.cancelScan() },
          onSelectCandidate = { candidate ->
            viewModel.selectCandidateForHexViewer(candidate)
          }
        )

        Screen.NEW_SCAN -> NewScanScreen(
          onBack = { viewModel.navigateTo(Screen.DASHBOARD) },
          onStartScan = { scanType, level, targetPath, targetExt ->
            viewModel.startScan(scanType, level, targetPath, targetExt)
          }
        )

        Screen.BIN_RECOVERY -> BinRecoveryScreen(
          binConfig = binConfig,
          learnedProfile = binProfile,
          onBack = { viewModel.navigateTo(Screen.DASHBOARD) },
          onUpdateConfig = { viewModel.updateBinConfig(it) },
          onLearnFromSample = { sampleFile -> viewModel.learnFromSampleFile(sampleFile) },
          onStartTargetedScan = { cfg ->
            viewModel.startScan(
              scanType = "TARGETED_BIN",
              level = com.example.model.ScanLevel.DEEP,
              targetPath = cfg.targetFolderPath,
              targetExt = "bin"
            )
          }
        )

        Screen.RESULTS -> ResultsScreen(
          candidates = candidates,
          selectedCandidate = selectedCandidate,
          onBack = { viewModel.navigateTo(Screen.DASHBOARD) },
          onSelectCandidate = { candidate ->
            viewModel.selectCandidateForHexViewer(candidate)
          },
          onOpenHexViewer = { candidate ->
            viewModel.selectCandidateForHexViewer(candidate)
          },
          onRecoverCandidate = { candidate ->
            viewModel.recoverCandidate(candidate)
          }
        )

        Screen.HEX_VIEWER -> HexViewerScreen(
          title = hexTitle,
          data = hexData,
          onBack = { viewModel.navigateTo(Screen.RESULTS) }
        )

        Screen.REPORT -> ReportScreen(
          activeSession = activeSession,
          capabilities = capabilities,
          candidates = candidates,
          onBack = { viewModel.navigateTo(Screen.DASHBOARD) }
        )
      }
    }
  }
}

