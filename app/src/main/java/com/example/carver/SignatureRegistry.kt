package com.example.carver

import com.example.core.HashUtils

data class FileSignature(
  val name: String,
  val extension: String,
  val mimeType: String,
  val header: ByteArray,
  val footer: ByteArray? = null,
  val minEntropy: Double = 1.0,
  val maxEntropy: Double = 8.0,
  val maxExpectedSize: Long = 100L * 1024 * 1024,
  val isContainer: Boolean = false
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FileSignature) return false
    return name == other.name && extension == other.extension && header.contentEquals(other.header)
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + extension.hashCode()
    result = 31 * result + header.contentHashCode()
    return result
  }
}

object SignatureRegistry {

  private val signatures = mutableListOf<FileSignature>()

  init {
    loadStandardSignatures()
  }

  private fun loadStandardSignatures() {
    signatures.clear()

    // JPEG: FF D8 FF ... FF D9
    signatures.add(
      FileSignature(
        name = "JPEG Image",
        extension = "jpg",
        mimeType = "image/jpeg",
        header = HashUtils.bytesFromHex("FF D8 FF"),
        footer = HashUtils.bytesFromHex("FF D9"),
        minEntropy = 6.5,
        maxExpectedSize = 30L * 1024 * 1024
      )
    )

    // PNG: 89 50 4E 47 0D 0A 1A 0A ... 49 45 4E 44 AE 42 60 82
    signatures.add(
      FileSignature(
        name = "PNG Image",
        extension = "png",
        mimeType = "image/png",
        header = HashUtils.bytesFromHex("89 50 4E 47 0D 0A 1A 0A"),
        footer = HashUtils.bytesFromHex("49 45 4E 44 AE 42 60 82"),
        minEntropy = 5.0,
        maxExpectedSize = 50L * 1024 * 1024
      )
    )

    // GIF89a / GIF87a
    signatures.add(
      FileSignature(
        name = "GIF Image",
        extension = "gif",
        mimeType = "image/gif",
        header = HashUtils.bytesFromHex("47 49 46 38 39 61"), // GIF89a
        footer = HashUtils.bytesFromHex("00 3B"),
        minEntropy = 4.0,
        maxExpectedSize = 25L * 1024 * 1024
      )
    )

    // WEBP: RIFF .... WEBP
    signatures.add(
      FileSignature(
        name = "WebP Image",
        extension = "webp",
        mimeType = "image/webp",
        header = HashUtils.bytesFromHex("52 49 46 46"), // "RIFF"
        minEntropy = 6.0,
        maxExpectedSize = 25L * 1024 * 1024
      )
    )

    // PDF: %PDF- (25 50 44 46 2D) ... %%EOF (25 25 45 4F 46)
    signatures.add(
      FileSignature(
        name = "PDF Document",
        extension = "pdf",
        mimeType = "application/pdf",
        header = HashUtils.bytesFromHex("25 50 44 46 2D"),
        footer = HashUtils.bytesFromHex("25 25 45 4F 46"),
        minEntropy = 3.5,
        maxExpectedSize = 100L * 1024 * 1024
      )
    )

    // ZIP / Office XML (DOCX, XLSX, APK, JAR): 50 4B 03 04
    signatures.add(
      FileSignature(
        name = "ZIP / Office Archive",
        extension = "zip",
        mimeType = "application/zip",
        header = HashUtils.bytesFromHex("50 4B 03 04"),
        footer = HashUtils.bytesFromHex("50 4B 05 06"), // End of central directory record
        minEntropy = 6.5,
        maxExpectedSize = 200L * 1024 * 1024,
        isContainer = true
      )
    )

    // SQLite Database: "SQLite format 3\000" (53 51 4C 69 74 65 20 66 6F 72 6D 61 74 20 33 00)
    signatures.add(
      FileSignature(
        name = "SQLite 3 Database",
        extension = "db",
        mimeType = "application/x-sqlite3",
        header = HashUtils.bytesFromHex("53 51 4C 69 74 65 20 66 6F 72 6D 61 74 20 33 00"),
        minEntropy = 2.0,
        maxEntropy = 7.5,
        maxExpectedSize = 500L * 1024 * 1024
      )
    )

    // MP4 / MOV: ....ftyp
    signatures.add(
      FileSignature(
        name = "MP4 / ISO Video",
        extension = "mp4",
        mimeType = "video/mp4",
        header = HashUtils.bytesFromHex("66 74 79 70"), // ftyp
        minEntropy = 6.5,
        maxExpectedSize = 500L * 1024 * 1024
      )
    )

    // WAV Audio: RIFF .... WAVE
    signatures.add(
      FileSignature(
        name = "WAV Audio",
        extension = "wav",
        mimeType = "audio/wav",
        header = HashUtils.bytesFromHex("52 49 46 46"),
        minEntropy = 3.0,
        maxExpectedSize = 100L * 1024 * 1024
      )
    )

    // MP3 Audio: ID3 header
    signatures.add(
      FileSignature(
        name = "MP3 Audio",
        extension = "mp3",
        mimeType = "audio/mpeg",
        header = HashUtils.bytesFromHex("49 44 33"), // "ID3"
        minEntropy = 6.0,
        maxExpectedSize = 50L * 1024 * 1024
      )
    )

    // Generic Binary Envelope / Firmware / Custom .BIN pattern
    signatures.add(
      FileSignature(
        name = "Binary Blob",
        extension = "bin",
        mimeType = "application/octet-stream",
        header = HashUtils.bytesFromHex("7F 45 4C 46"), // ELF header (common for compiled bin)
        minEntropy = 3.0,
        maxExpectedSize = 100L * 1024 * 1024
      )
    )
  }

  fun getAllSignatures(): List<FileSignature> = signatures.toList()

  fun registerCustomSignature(signature: FileSignature) {
    // Remove if already exists with same name
    signatures.removeAll { it.name == signature.name }
    signatures.add(0, signature) // Prioritize custom signature
  }

  fun findMatchingHeader(buffer: ByteArray, offset: Int = 0): FileSignature? {
    val remaining = buffer.size - offset
    for (sig in signatures) {
      if (remaining >= sig.header.size) {
        var match = true
        for (i in sig.header.indices) {
          if (buffer[offset + i] != sig.header[i]) {
            match = false
            break
          }
        }
        if (match) return sig
      }
    }
    return null
  }
}
