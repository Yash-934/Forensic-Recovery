package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AuditLogEntry
import com.example.model.RecoveryCandidate
import com.example.model.ScanSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

  // Sessions
  @Query("SELECT * FROM scan_sessions ORDER BY startTime DESC")
  fun getAllSessions(): Flow<List<ScanSession>>

  @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
  suspend fun getSessionById(sessionId: String): ScanSession?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: ScanSession)

  @Update
  suspend fun updateSession(session: ScanSession)

  // Candidates
  @Query("SELECT * FROM recovery_candidates WHERE sessionId = :sessionId ORDER BY confidenceScore DESC")
  fun getCandidatesForSession(sessionId: String): Flow<List<RecoveryCandidate>>

  @Query("SELECT * FROM recovery_candidates ORDER BY timestamp DESC")
  fun getAllCandidates(): Flow<List<RecoveryCandidate>>

  @Query("SELECT * FROM recovery_candidates WHERE id = :candidateId")
  suspend fun getCandidateById(candidateId: String): RecoveryCandidate?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCandidate(candidate: RecoveryCandidate)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCandidates(candidates: List<RecoveryCandidate>)

  @Update
  suspend fun updateCandidate(candidate: RecoveryCandidate)

  // Audit Logs
  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
  fun getAuditLogs(): Flow<List<AuditLogEntry>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAuditLog(log: AuditLogEntry)
}
