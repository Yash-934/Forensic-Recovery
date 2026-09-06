package com.example.database

import com.example.core.HashUtils
import com.example.model.AuditLogEntry
import com.example.model.RecoveryCandidate
import com.example.model.ScanSession
import kotlinx.coroutines.flow.Flow

class ForensicRepository(private val scanDao: ScanDao) {

  val allSessions: Flow<List<ScanSession>> = scanDao.getAllSessions()
  val allCandidates: Flow<List<RecoveryCandidate>> = scanDao.getAllCandidates()
  val auditLogs: Flow<List<AuditLogEntry>> = scanDao.getAuditLogs()

  fun getCandidatesForSession(sessionId: String): Flow<List<RecoveryCandidate>> =
    scanDao.getCandidatesForSession(sessionId)

  suspend fun insertSession(session: ScanSession) = scanDao.insertSession(session)

  suspend fun updateSession(session: ScanSession) = scanDao.updateSession(session)

  suspend fun insertCandidates(candidates: List<RecoveryCandidate>) =
    scanDao.insertCandidates(candidates)

  suspend fun updateCandidate(candidate: RecoveryCandidate) =
    scanDao.updateCandidate(candidate)

  suspend fun getCandidateById(id: String): RecoveryCandidate? =
    scanDao.getCandidateById(id)

  suspend fun logAction(action: String, target: String, details: String, evidenceBytes: ByteArray? = null) {
    val hash = if (evidenceBytes != null) HashUtils.sha256(evidenceBytes) else "NONE"
    scanDao.insertAuditLog(
      AuditLogEntry(
        action = action,
        target = target,
        sha256Evidence = hash,
        details = details
      )
    )
  }
}
