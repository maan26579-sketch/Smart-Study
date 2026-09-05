package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.FlashcardDao
import com.example.data.local.LectureSessionDao
import com.example.data.local.QuizDao
import com.example.data.model.Flashcard
import com.example.data.model.FormulaTerm
import com.example.data.model.LectureSession
import com.example.data.model.QuizQuestion
import com.example.data.remote.GeminiStudyService
import com.example.data.remote.GeneratedStudyPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SyncReport(
    val syncedCount: Int,
    val timestamp: Long,
    val devices: List<String>
)

class StudyRepository(
    private val sessionDao: LectureSessionDao,
    private val flashcardDao: FlashcardDao,
    private val quizDao: QuizDao,
    private val geminiService: GeminiStudyService = GeminiStudyService()
) {

    val allSessions: Flow<List<LectureSession>> = sessionDao.getAllSessions()

    fun getSession(id: Long): Flow<LectureSession?> = sessionDao.getSessionById(id)

    fun getFlashcards(sessionId: Long): Flow<List<Flashcard>> =
        flashcardDao.getFlashcardsForSession(sessionId)

    fun getQuiz(sessionId: Long): Flow<List<QuizQuestion>> =
        quizDao.getQuizForSession(sessionId)

    suspend fun processAndSaveMaterial(
        rawContent: String,
        sourceType: String,
        bitmap: Bitmap? = null,
        subjectHint: String? = null,
        audioPath: String? = null,
        audioDurationSeconds: Int = 0,
        imageUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val studyPackage = geminiService.analyzeStudyMaterial(
            textNotes = rawContent,
            bitmapImage = bitmap,
            userSubjectHint = subjectHint
        )

        val takeawaysJson = JSONArray().apply {
            studyPackage.keyTakeaways.forEach { put(it) }
        }.toString()

        val formulasJson = JSONArray().apply {
            studyPackage.formulasAndTerms.forEach { term ->
                put(JSONObject().apply {
                    put("term", term.term)
                    put("definition", term.definition)
                    term.formula?.let { put("formula", it) }
                })
            }
        }.toString()

        val session = LectureSession(
            title = studyPackage.title,
            subject = studyPackage.subject,
            sourceType = sourceType,
            rawContent = rawContent,
            audioFilePath = audioPath,
            audioDurationSeconds = audioDurationSeconds,
            imageUri = imageUri,
            summary = studyPackage.summary,
            keyTakeawaysJson = takeawaysJson,
            formulasAndTermsJson = formulasJson,
            isSynced = true,
            lastSyncedAt = System.currentTimeMillis()
        )

        val sessionId = sessionDao.insertSession(session)

        val flashcardEntities = studyPackage.flashcards.map {
            Flashcard(
                sessionId = sessionId,
                front = it.front,
                back = it.back,
                difficulty = it.difficulty
            )
        }
        flashcardDao.insertFlashcards(flashcardEntities)

        val quizEntities = studyPackage.quizQuestions.map {
            val optionsJson = JSONArray().apply {
                it.options.forEach { opt -> put(opt) }
            }.toString()

            QuizQuestion(
                sessionId = sessionId,
                question = it.question,
                optionsJson = optionsJson,
                correctIndex = it.correctIndex,
                explanation = it.explanation,
                userSelectedIndex = -1
            )
        }
        quizDao.insertQuizQuestions(quizEntities)

        sessionId
    }

    suspend fun updateFlashcardMastery(cardId: Long, isMastered: Boolean) =
        withContext(Dispatchers.IO) {
            flashcardDao.updateMastery(cardId, isMastered, System.currentTimeMillis())
        }

    suspend fun recordQuizAnswer(questionId: Long, selectedIndex: Int) =
        withContext(Dispatchers.IO) {
            quizDao.updateUserAnswer(questionId, selectedIndex)
        }

    suspend fun resetQuiz(sessionId: Long) = withContext(Dispatchers.IO) {
        quizDao.resetQuizAnswers(sessionId)
    }

    suspend fun toggleBookmark(session: LectureSession) = withContext(Dispatchers.IO) {
        sessionDao.updateBookmark(session.id, !session.isBookmarked)
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        flashcardDao.deleteFlashcardsForSession(sessionId)
        quizDao.deleteQuizForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun syncAllDevices(): SyncReport = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        sessionDao.markAllSynced(now)
        val sessions = sessionDao.getAllSessions().firstOrNull() ?: emptyList()
        SyncReport(
            syncedCount = sessions.size,
            timestamp = now,
            devices = listOf(
                "Pixel Tablet (Student Desk)",
                "Web Companion (Chrome Studio)",
                "Local On-Device Vault (Offline Ready)"
            )
        )
    }

    suspend fun exportAllDecksJson(): String = withContext(Dispatchers.IO) {
        val sessions = sessionDao.getAllSessions().firstOrNull() ?: emptyList()
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val sessionsArray = JSONArray()
        for (s in sessions) {
            val sObj = JSONObject()
            sObj.put("title", s.title)
            sObj.put("subject", s.subject)
            sObj.put("sourceType", s.sourceType)
            sObj.put("summary", s.summary)
            sObj.put("rawContent", s.rawContent)
            sObj.put("keyTakeaways", JSONArray(s.keyTakeawaysJson))
            sObj.put("formulasAndTerms", JSONArray(s.formulasAndTermsJson))

            val cards = flashcardDao.getFlashcardsOnce(s.id)
            val cardsArray = JSONArray()
            for (c in cards) {
                cardsArray.put(JSONObject().apply {
                    put("front", c.front)
                    put("back", c.back)
                    put("difficulty", c.difficulty)
                    put("isMastered", c.isMastered)
                })
            }
            sObj.put("flashcards", cardsArray)

            val quiz = quizDao.getQuizOnce(s.id)
            val quizArray = JSONArray()
            for (q in quiz) {
                quizArray.put(JSONObject().apply {
                    put("question", q.question)
                    put("options", JSONArray(q.optionsJson))
                    put("correctIndex", q.correctIndex)
                    put("explanation", q.explanation)
                })
            }
            sObj.put("quiz", quizArray)
            sessionsArray.put(sObj)
        }
        root.put("sessions", sessionsArray)
        root.toString(2)
    }

    suspend fun prepopulateStarterDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = sessionDao.getAllSessions().firstOrNull()
        if (!existing.isNullOrEmpty()) return@withContext

        // Prepopulate 2 realistic University Lecture & Whiteboard Sets
        // Sample 1: Audio Lecture - Machine Learning: Gradient Descent & Loss Landscapes
        val sample1Notes = """
            Prof. Martinez: 'Welcome back to CS 229. Today we delve into Optimization Landscapes and Stochastic Gradient Descent.
            Recall our objective function J of theta, which measures the empirical risk over our training batch.
            When we take the partial derivative of J with respect to parameter theta_j, we are determining the direction of steepest ascent.
            To minimize error, our update rule subtracts alpha times the gradient: theta equals theta minus alpha times nabla J.
            Here alpha is our learning rate or step size. If alpha is too large, the optimizer oscillates wildly or diverges.
            If alpha is too small, convergence takes millions of iterations or gets stuck in saddle points.
            In deep architectures, we encounter pathological curvature: ravines where the surface curves much more steeply in one dimension than another.
            Standard SGD oscillates across the slopes. Momentum solves this by adding an exponentially decaying moving average of past gradients, dampening oscillations and accelerating through flat plateaus.'
        """.trimIndent()

        val sample1Takeaways = JSONArray().apply {
            put("Gradient descent updates parameters in the direction opposite to the gradient of the empirical loss: theta := theta - alpha * nabla J(theta).")
            put("The learning rate alpha controls step size; excessively high rates induce divergence while low rates result in slow convergence or entrapment.")
            put("Pathological curvature causes standard SGD to oscillate across steep ravines; Momentum dampens oscillations by accumulating past directional velocity.")
            put("Mini-batch gradient descent balances computational throughput with stochastic regularization, smoothing loss surface traversal.")
        }.toString()

        val sample1Formulas = JSONArray().apply {
            put(JSONObject().apply {
                put("term", "Standard Parameter Update Rule")
                put("definition", "Iterative parameter adjustment moving anti-parallel to the loss gradient.")
                put("formula", "theta_{t+1} = theta_t - alpha * nabla J(theta_t)")
            })
            put(JSONObject().apply {
                put("term", "Momentum Velocity Accumulator")
                put("definition", "Exponential moving average dampening orthogonal oscillations while accelerating consistent descent.")
                put("formula", "v_t = beta * v_{t-1} + (1 - beta) * nabla J(theta_t)")
            })
            put(JSONObject().apply {
                put("term", "Pathological Curvature")
                put("definition", "An ill-conditioned Hessian matrix with high condition number creating deep, narrow loss ravines.")
                put("formula", "kappa = lambda_{max} / lambda_{min} >> 1")
            })
        }.toString()

        val session1 = LectureSession(
            title = "Machine Learning: Optimization & Gradient Descent",
            subject = "Computer Science",
            sourceType = "AUDIO_LECTURE",
            rawContent = sample1Notes,
            audioFilePath = null,
            audioDurationSeconds = 245,
            summary = "This lecture establishes the mathematical foundation of optimization in machine learning. It explores gradient descent mechanics, learning rate sensitivity, and how momentum counteracts the ill-conditioned curvature found in deep learning loss surfaces. Students examine standard SGD, mini-batch trade-offs, and velocity vector formulations.",
            keyTakeawaysJson = sample1Takeaways,
            formulasAndTermsJson = sample1Formulas,
            isSynced = true,
            isBookmarked = true
        )

        val id1 = sessionDao.insertSession(session1)

        val cards1 = listOf(
            Flashcard(
                sessionId = id1,
                front = "What is the update rule for basic Gradient Descent?",
                back = "theta := theta - alpha * nabla J(theta), where alpha is the learning rate and nabla J is the loss gradient.",
                difficulty = "Easy"
            ),
            Flashcard(
                sessionId = id1,
                front = "Why does standard SGD struggle with pathological curvature?",
                back = "Because gradient vectors oscillate perpendicular to the ravine rather than making rapid progress along the gentle descent axis.",
                difficulty = "Medium"
            ),
            Flashcard(
                sessionId = id1,
                front = "How does the Momentum parameter beta improve convergence?",
                back = "By carrying forward a fraction beta of previous velocity, canceling out alternating oscillations and accelerating consistent directional momentum.",
                difficulty = "Medium"
            ),
            Flashcard(
                sessionId = id1,
                front = "What occurs if the learning rate alpha is set excessively high?",
                back = "The optimization step overshoots the minimum, leading to catastrophic numerical divergence or endless unstable oscillations.",
                difficulty = "Easy"
            ),
            Flashcard(
                sessionId = id1,
                front = "What is the condition number of a Hessian matrix?",
                back = "The ratio of its maximum to minimum eigenvalue (lambda_max / lambda_min), quantifying curvature asymmetry.",
                difficulty = "Hard"
            )
        )
        flashcardDao.insertFlashcards(cards1)

        val quiz1 = listOf(
            QuizQuestion(
                sessionId = id1,
                question = "In the parameter update equation theta := theta - alpha * nabla J, what does alpha represent?",
                optionsJson = JSONArray(listOf("Learning rate (step size)", "Batch regularization factor", "Momentum damping coefficient", "Loss variance")).toString(),
                correctIndex = 0,
                explanation = "Alpha is the learning rate scalar controlling the magnitude of each update step along the negative gradient."
            ),
            QuizQuestion(
                sessionId = id1,
                question = "What primary challenge does Momentum directly mitigate in gradient descent?",
                optionsJson = JSONArray(listOf("Vanishing network weights", "High-frequency oscillations across steep loss ravines", "Underfitting the validation dataset", "Zero gradients at global optima")).toString(),
                correctIndex = 1,
                explanation = "Momentum aggregates past gradient history, canceling opposing oscillations while compounding progress along the trough."
            ),
            QuizQuestion(
                sessionId = id1,
                question = "Why is Mini-batch SGD preferred over Full-batch Gradient Descent in modern deep learning?",
                optionsJson = JSONArray(listOf("It eliminates all hyperparameter tuning", "It offers GPU parallelism while introducing beneficial gradient noise to escape saddle points", "It guarantees analytical zero error on every iteration", "It requires zero RAM")).toString(),
                correctIndex = 1,
                explanation = "Mini-batch SGD provides both computational efficiency on vectorized hardware and mild stochastic noise that aids exploration."
            ),
            QuizQuestion(
                sessionId = id1,
                question = "If an optimizer oscillates violently between opposite walls of a valley, which parameter adjustment is most urgent?",
                optionsJson = JSONArray(listOf("Increase the learning rate", "Reduce the learning rate or introduce momentum damping", "Delete the training set", "Increase batch size to infinity")).toString(),
                correctIndex = 1,
                explanation = "Oscillation across walls indicates excessive step sizes relative to local curvature; reducing alpha or applying momentum stabilizes trajectory."
            )
        )
        quizDao.insertQuizQuestions(quiz1)

        // Sample 2: Whiteboard Scan - Organic Chemistry: Carbonyl Electrophiles & Nucleophilic Addition
        val sample2Notes = """
            [Whiteboard Scan: Chem 320 - Hall 4B]
            C=O Carbonyl Group Polarity: Oxygen is highly electronegative (delta-), leaving carbonyl carbon electropositive (delta+).
            Nucleophilic attack occurs at the carbonyl carbon at the Bürgi-Dunitz angle (~107 degrees).
            Intermediate: Tetrahedral alkoxide intermediate formed.
            Grignard Reagent: R-MgX acts as a powerful carbanion equivalent (R:-).
            Addition to Formaldehyde -> Primary alcohol
            Addition to Aldehyde -> Secondary alcohol
            Addition to Ketone -> Tertiary alcohol
            Quenching with aqueous hydronium (H3O+) protonates the tetrahedral intermediate alkoxide to yield the final neutral alcohol.
            Caution: Anhydrous conditions strictly required due to extreme basicity of Grignard reagents (water immediately protonates R-MgX to hydrocarbon).
        """.trimIndent()

        val sample2Takeaways = JSONArray().apply {
            put("The carbonyl carbon is an electrophilic center due to significant dipole moment toward the electronegative oxygen.")
            put("Nucleophiles attack the sp2 carbonyl carbon at the ~107° Bürgi-Dunitz trajectory, yielding a tetrahedral sp3 intermediate.")
            put("Grignard reagents (R-MgX) generate primary alcohols from formaldehyde, secondary alcohols from aldehydes, and tertiary alcohols from ketones.")
            put("Reactions require strictly anhydrous ethereal solvents (e.g. diethyl ether, THF) to prevent premature acid-base quenching by water.")
        }.toString()

        val sample2Formulas = JSONArray().apply {
            put(JSONObject().apply {
                put("term", "Bürgi-Dunitz Angle")
                put("definition", "The obtuse angle (~107°) of approach maximizing orbital overlap between nucleophile HOMO and carbonyl pi* LUMO.")
                put("formula", "theta approx 107 deg")
            })
            put(JSONObject().apply {
                put("term", "Grignard Formation")
                put("definition", "Oxidative insertion of metallic magnesium into an alkyl or aryl carbon-halogen bond.")
                put("formula", "R-X + Mg (ether) -> R-MgX")
            })
            put(JSONObject().apply {
                put("term", "Tertiary Alcohol Synthesis")
                put("definition", "Nucleophilic addition of a Grignard reagent to a ketone followed by hydronium workup.")
                put("formula", "R'-CO-R'' + R-MgX -> [Intermediate] --(H3O+)--> R'R''(R)C-OH")
            })
        }.toString()

        val session2 = LectureSession(
            title = "Organic Chemistry: Carbonyl Additions & Grignards",
            subject = "Organic Chemistry",
            sourceType = "WHITEBOARD_SCAN",
            rawContent = sample2Notes,
            audioFilePath = null,
            audioDurationSeconds = 0,
            summary = "Extracted from lecture whiteboard notes on carbonyl electrophilicity and organometallic additions. The session analyzes the dipole moment of carbonyl bonds, the Bürgi-Dunitz orbital overlap trajectory, and the complete synthetic pathways of Grignard reagents into formaldehyde, aldehydes, and ketones followed by acidic workup.",
            keyTakeawaysJson = sample2Takeaways,
            formulasAndTermsJson = sample2Formulas,
            isSynced = true,
            isBookmarked = false
        )

        val id2 = sessionDao.insertSession(session2)

        val cards2 = listOf(
            Flashcard(
                sessionId = id2,
                front = "What is the Bürgi-Dunitz angle and why is it approximately 107 degrees?",
                back = "The angle of nucleophilic attack onto a carbonyl carbon; ~107° maximizes overlap with the pi* antibonding orbital while minimizing electrostatic repulsion with oxygen lone pairs.",
                difficulty = "Hard"
            ),
            Flashcard(
                sessionId = id2,
                front = "What alcohol product is formed when a Grignard reagent reacts with a ketone?",
                back = "A tertiary (3°) alcohol after acidic aqueous workup.",
                difficulty = "Easy"
            ),
            Flashcard(
                sessionId = id2,
                front = "Why must Grignard reactions be performed under strictly anhydrous conditions?",
                back = "Because organomagnesium reagents are extremely strong bases; any water or protic solvent will immediately protonate the carbanion to an alkane, destroying the reagent.",
                difficulty = "Medium"
            ),
            Flashcard(
                sessionId = id2,
                front = "What hybridization change occurs at the carbonyl carbon during nucleophilic attack?",
                back = "From planar sp2 hybridization to tetrahedral sp3 hybridization in the alkoxide intermediate.",
                difficulty = "Medium"
            )
        )
        flashcardDao.insertFlashcards(cards2)

        val quiz2 = listOf(
            QuizQuestion(
                sessionId = id2,
                question = "Reaction of phenylmagnesium bromide (PhMgBr) with propanal followed by H3O+ workup produces:",
                optionsJson = JSONArray(listOf("1-phenylpropan-1-ol (Secondary alcohol)", "2-phenylpropan-2-ol (Tertiary alcohol)", "Phenol and propane gas", "Benzoic acid")).toString(),
                correctIndex = 0,
                explanation = "Propanal is an aldehyde; addition of a carbanion to an aldehyde produces a secondary alcohol with one phenyl and one ethyl substituent on the carbinol carbon."
            ),
            QuizQuestion(
                sessionId = id2,
                question = "What orbital interaction drives nucleophilic addition into carbonyls?",
                optionsJson = JSONArray(listOf("Nucleophile HOMO attacks the carbonyl pi* (pi-star) LUMO", "Nucleophile LUMO interacts with the carbon 1s core", "Carbonyl sigma* accepts electrons from solvent", "Direct overlap of oxygen lone pairs with magnesium d-orbitals")).toString(),
                correctIndex = 0,
                explanation = "The highest occupied molecular orbital (HOMO) of the nucleophile donates electron density into the lowest unoccupied molecular orbital (pi* LUMO) of the carbonyl group."
            ),
            QuizQuestion(
                sessionId = id2,
                question = "What happens if acetone is treated with methylmagnesium iodide in wet (aqueous) solvent?",
                optionsJson = JSONArray(listOf("Tertiary butyl alcohol forms in 99% yield", "Methane gas is released and the Grignard is destroyed", "A stable hemiketal crystals form", "Acetone polymerizes instantly")).toString(),
                correctIndex = 1,
                explanation = "Grignard reagents are strong bases (pKa of conjugate alkane ~50). Water (pKa ~15.7) will instantaneously protonate CH3-MgI to produce methane gas (CH4) and basic magnesium salts."
            )
        )
        quizDao.insertQuizQuestions(quiz2)
    }
}
