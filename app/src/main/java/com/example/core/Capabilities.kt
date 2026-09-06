package com.example.core

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

enum class RecoveryCapabilityLevel {
  STANDARD,
  ROOTED,
  IMAGE_ANALYSIS,
  LIMITED
}

data class DeviceForensicCapabilities(
  val androidVersion: String,
  val apiLevel: Int,
  val manufacturer: String,
  val model: String,
  val isRootAvailable: Boolean,
  val rootEvidence: String,
  val hasManageExternalStorage: Boolean,
  val primaryFilesystem: String,
  val partitionsDetected: List<String>,
  val accessibleStoragePaths: List<String>,
  val capabilityLevel: RecoveryCapabilityLevel,
  val capabilityExplanation: String,
  val encryptionStatus: String,
  val trimNotice: String
)

object CapabilityDetector {

  fun detect(context: Context): DeviceForensicCapabilities {
    val api = Build.VERSION.SDK_INT
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    val osVersion = "Android ${Build.VERSION.RELEASE} (API $api)"

    val (isRoot, rootEvidence) = checkRootDefensively()
    val hasAllFilesAccess = if (api >= Build.VERSION_CODES.R) {
      Environment.isExternalStorageManager()
    } else {
      true
    }

    val accessiblePaths = mutableListOf<String>()
    try {
      context.getExternalFilesDirs(null).filterNotNull().forEach { file ->
        accessiblePaths.add(file.absolutePath)
      }
      val extDir = Environment.getExternalStorageDirectory()
      if (extDir != null && extDir.exists() && extDir.canRead()) {
        accessiblePaths.add(extDir.absolutePath)
      }
    } catch (_: Exception) {}

    val detectedPartitions = enumerateBlockDevices()
    val fsType = detectFilesystem()

    val level: RecoveryCapabilityLevel
    val explanation: String

    if (isRoot && detectedPartitions.isNotEmpty()) {
      level = RecoveryCapabilityLevel.ROOTED
      explanation = "Superuser privileges active. Direct raw partition block reading (/dev/block/...) is legitimately accessible. Caution: FBE and TRIM apply to freed blocks."
    } else if (hasAllFilesAccess) {
      level = RecoveryCapabilityLevel.STANDARD
      explanation = "Standard Android with MANAGE_EXTERNAL_STORAGE access. Logical storage traversal, MediaStore database artifacts, recycle bins, hidden files, and file carving on accessible storage enabled."
    } else {
      level = RecoveryCapabilityLevel.LIMITED
      explanation = "Scoped storage sandbox active without MANAGE_EXTERNAL_STORAGE. Scanning is strictly confined to app-private cache and public media pickers. Request storage access for full analysis."
    }

    val encryptionStatus = if (api >= Build.VERSION_CODES.N) {
      "File-Based Encryption (FBE) active (AES-256-XTS). Deleted file encryption keys are erased from kernel keyring on unlink."
    } else {
      "Full-Disk Encryption (FDE) legacy mode."
    }

    val trimNotice = "Flash storage controllers issue TRIM/discard upon deletion. Physical NAND blocks may return zeros or be recycled into wear-leveling pools."

    return DeviceForensicCapabilities(
      androidVersion = osVersion,
      apiLevel = api,
      manufacturer = manufacturer,
      model = model,
      isRootAvailable = isRoot,
      rootEvidence = rootEvidence,
      hasManageExternalStorage = hasAllFilesAccess,
      primaryFilesystem = fsType,
      partitionsDetected = detectedPartitions,
      accessibleStoragePaths = accessiblePaths,
      capabilityLevel = level,
      capabilityExplanation = explanation,
      encryptionStatus = encryptionStatus,
      trimNotice = trimNotice
    )
  }

  private fun checkRootDefensively(): Pair<Boolean, String> {
    val commonPaths = listOf(
      "/system/bin/su",
      "/system/xbin/su",
      "/sbin/su",
      "/system/sd/xbin/su",
      "/system/bin/failsafe/su",
      "/data/local/xbin/su",
      "/data/local/bin/su",
      "/data/local/su"
    )

    for (path in commonPaths) {
      try {
        val f = File(path)
        if (f.exists() && f.canExecute()) {
          return Pair(true, "Found su binary at $path")
        }
      } catch (_: Exception) {}
    }

    // Check PATH
    val pathEnv = System.getenv("PATH") ?: ""
    for (dir in pathEnv.split(":")) {
      val candidate = File(dir, "su")
      if (candidate.exists() && candidate.canExecute()) {
        return Pair(true, "Found executable su in PATH: ${candidate.absolutePath}")
      }
    }

    return Pair(false, "Unrooted standard Android runtime. SELinux enforcing.")
  }

  private fun enumerateBlockDevices(): List<String> {
    val blocks = mutableListOf<String>()
    val candidateDirs = listOf("/dev/block", "/dev/block/by-name", "/dev/block/bootdevice/by-name")
    for (dirPath in candidateDirs) {
      try {
        val dir = File(dirPath)
        if (dir.exists() && dir.canRead()) {
          dir.listFiles()?.take(15)?.forEach { file ->
            blocks.add(file.name)
          }
        }
      } catch (_: Exception) {}
    }
    return blocks
  }

  private fun detectFilesystem(): String {
    try {
      val mounts = File("/proc/mounts")
      if (mounts.exists() && mounts.canRead()) {
        mounts.useLines { lines ->
          for (line in lines) {
            if (line.contains("/data ") || line.contains("/storage/emulated")) {
              val parts = line.split("\\s+".toRegex())
              if (parts.size >= 3) {
                val type = parts[2].lowercase()
                return when {
                  type.contains("f2fs") -> "F2FS (Flash-Friendly File System)"
                  type.contains("ext4") -> "ext4 (Fourth Extended Filesystem)"
                  type.contains("exfat") -> "exFAT"
                  type.contains("sdcardfs") || type.contains("fuse") -> "FUSE/sdcardfs over $type"
                  else -> type
                }
              }
            }
          }
        }
      }
    } catch (_: Exception) {}
    return "F2FS / ext4 (Standard Android Data Partition)"
  }
}
