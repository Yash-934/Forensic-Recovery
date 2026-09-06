package com.example.core

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

object HashUtils {

  fun sha256(data: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(data)
    return toHex(digest)
  }

  fun sha256(file: File): String {
    if (!file.exists() || !file.canRead()) return "UNREADABLE"
    val md = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    file.inputStream().use { stream ->
      var bytesRead: Int
      while (stream.read(buffer).also { bytesRead = it } != -1) {
        md.update(buffer, 0, bytesRead)
      }
    }
    return toHex(md.digest())
  }

  fun crc32(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
    val crc = CRC32()
    crc.update(data, offset, length)
    return crc.value
  }

  fun toHex(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
      sb.append(String.format("%02x", b.toInt() and 0xFF))
    }
    return sb.toString()
  }

  fun bytesFromHex(hex: String): ByteArray {
    val clean = hex.replace(" ", "").replace("0x", "").uppercase()
    val len = clean.length
    if (len % 2 != 0) return ByteArray(0)
    val result = ByteArray(len / 2)
    for (i in 0 until len step 2) {
      result[i / 2] = ((Character.digit(clean[i], 16) shl 4) +
          Character.digit(clean[i + 1], 16)).toByte()
    }
    return result
  }
}
