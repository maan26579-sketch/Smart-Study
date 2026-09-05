package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Flashcard
import com.example.data.model.LectureSession
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureSessionDao {
    @Query("SELECT * FROM lecture_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<LectureSession>>

    @Query("SELECT * FROM lecture_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<LectureSession?>

    @Query("SELECT * FROM lecture_sessions WHERE id = :id")
    suspend fun getSessionByIdOnce(id: Long): LectureSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LectureSession): Long

    @Update
    suspend fun updateSession(session: LectureSession)

    @Query("DELETE FROM lecture_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("UPDATE lecture_sessions SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, isBookmarked: Boolean)

    @Query("UPDATE lecture_sessions SET isSynced = :isSynced, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, isSynced: Boolean, lastSyncedAt: Long)

    @Query("UPDATE lecture_sessions SET isSynced = 1, lastSyncedAt = :timestamp")
    suspend fun markAllSynced(timestamp: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getFlashcardsForSession(sessionId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE sessionId = :sessionId")
    suspend fun getFlashcardsOnce(sessionId: Long): List<Flashcard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<Flashcard>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: Flashcard): Long

    @Update
    suspend fun updateFlashcard(card: Flashcard)

    @Query("UPDATE flashcards SET isMastered = :isMastered, lastReviewedAt = :timestamp WHERE id = :id")
    suspend fun updateMastery(id: Long, isMastered: Boolean, timestamp: Long)

    @Query("DELETE FROM flashcards WHERE sessionId = :sessionId")
    suspend fun deleteFlashcardsForSession(sessionId: Long)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getQuizForSession(sessionId: Long): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM quiz_questions WHERE sessionId = :sessionId")
    suspend fun getQuizOnce(sessionId: Long): List<QuizQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)

    @Query("UPDATE quiz_questions SET userSelectedIndex = :selectedIndex WHERE id = :id")
    suspend fun updateUserAnswer(id: Long, selectedIndex: Int)

    @Query("UPDATE quiz_questions SET userSelectedIndex = -1 WHERE sessionId = :sessionId")
    suspend fun resetQuizAnswers(sessionId: Long)

    @Query("DELETE FROM quiz_questions WHERE sessionId = :sessionId")
    suspend fun deleteQuizForSession(sessionId: Long)
}
