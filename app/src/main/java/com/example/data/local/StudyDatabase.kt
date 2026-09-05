package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Flashcard
import com.example.data.model.LectureSession
import com.example.data.model.QuizQuestion

@Database(
    entities = [LectureSession::class, Flashcard::class, QuizQuestion::class],
    version = 1,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun lectureSessionDao(): LectureSessionDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "smart_study_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
