package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.AuditLogEntry
import com.example.model.RecoveryCandidate
import com.example.model.ScanSession

@Database(
  entities = [
    ScanSession::class,
    RecoveryCandidate::class,
    AuditLogEntry::class
  ],
  version = 1,
  exportSchema = false
)
abstract class ForensicDatabase : RoomDatabase() {

  abstract fun scanDao(): ScanDao

  companion object {
    @Volatile
    private var INSTANCE: ForensicDatabase? = null

    fun getInstance(context: Context): ForensicDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          ForensicDatabase::class.java,
          "aegis_forensics.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
