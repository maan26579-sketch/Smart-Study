package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "lecture_sessions")
data class LectureSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceType: String, // AUDIO_LECTURE, WHITEBOARD_SCAN, TEXTBOOK_NOTES
    val rawContent: String = "",
    val audioFilePath: String? = null,
    val audioDurationSeconds: Int = 0,
    val imageUri: String? = null,
    val summary: String = "",
    val keyTakeawaysJson: String = "[]",
    val formulasAndTermsJson: String = "[]",
    val isSynced: Boolean = true,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
) {
    fun getKeyTakeaways(): List<String> {
        return try {
            val jsonArray = JSONArray(keyTakeawaysJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            if (keyTakeawaysJson.isNotBlank()) keyTakeawaysJson.lines().filter { it.isNotBlank() }
            else emptyList()
        }
    }

    fun getFormulasAndTerms(): List<FormulaTerm> {
        return try {
            val jsonArray = JSONArray(formulasAndTermsJson)
            val list = mutableListOf<FormulaTerm>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FormulaTerm(
                        term = obj.optString("term", ""),
                        definition = obj.optString("definition", ""),
                        formula = if (obj.has("formula") && !obj.isNull("formula")) obj.getString("formula") else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class FormulaTerm(
    val term: String,
    val definition: String,
    val formula: String? = null
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = LectureSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val front: String,
    val back: String,
    val difficulty: String = "Medium",
    val isMastered: Boolean = false,
    val lastReviewedAt: Long? = null
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = LectureSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val question: String,
    val optionsJson: String,
    val correctIndex: Int,
    val explanation: String = "",
    val userSelectedIndex: Int = -1
) {
    fun getOptions(): List<String> {
        return try {
            val jsonArray = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
