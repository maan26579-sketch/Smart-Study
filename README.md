# Smart Study 🎓

> **AI-Powered Lecture & Study Companion for Android**  
> Record live lectures, scan handwritten whiteboard notes, and instantly convert them into structured summaries, key formulas, interactive flashcards, and self-assessment quizzes with full offline vault storage.

---

## ✨ Features

- 🎙️ **Live Lecture Audio Recording & Playback**
  - High-fidelity microphone capture with real-time waveform visualization.
  - Built-in audio playback engine supporting custom playback speeds (0.75x, 1.0x, 1.25x, 1.5x, 2.0x) and 10-second skip controls.
- 📸 **Whiteboard & Note Scanner**
  - Photo Picker integration to capture or upload classroom whiteboards, diagrams, and notebook pages.
  - Pre-loaded academic samples (Physics, Organic Chemistry, Economics, Molecular Biology) for instant demonstration.
- 🧠 **AI Synthesis (Gemini 2.5 Flash)**
  - Generates comprehensive executive summaries with bullet points and key formulas.
  - Creates active-recall flashcards categorized by difficulty (Easy, Medium, Hard).
  - Produces multiple-choice quizzes with explanations for each question.
- 🗂️ **Interactive Study Decks**
  - **Flashcard Mode**: Flip-to-reveal answers, track mastered cards vs. items still needing review.
  - **Quiz Mode**: Timed or untimed multiple-choice test runner with immediate rationales and final score breakdown.
  - **Key Concepts & Formulas**: Organized reference cards for quick exam revision.
- 💾 **Offline-First Vault & Data Portability**
  - Built on Android Room Database for complete offline functionality.
  - Device-to-device simulation sync and one-click JSON deck export/backup.
  - Bookmark favorite lecture sessions for quick filtering.
- 🎨 **Modern Bold Typography UI**
  - Material 3 design system with vibrant purple/lavender accenting, high-contrast typography, and accessible 48dp+ touch targets.

---

## 🏗️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & `StateFlow`
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) via Kotlin Symbol Processing (KSP)
- **AI Integration**: Google Gemini 2.5 Flash via Server-Side API / Secrets Gradle Plugin
- **Audio Engine**: Android `MediaRecorder` & `MediaPlayer` with waveform scrubber
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`gradle/libs.versions.toml`)

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer
- **Android SDK**: `minSdk 24`, `targetSdk 36`, `compileSdk 36`
- **JDK**: Java 11 or Java 17
- **Gemini API Key**: Get a key from [Google AI Studio](https://aistudio.google.com/)

### Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/smart-study.git
   cd smart-study
   ```

2. **Configure API Keys**:
   Copy `.env.example` to `.env` in the root project directory:
   ```bash
   cp .env.example .env
   ```
   Open `.env` and add your Gemini API Key:
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
   *(The Secrets Gradle plugin will automatically inject this into `BuildConfig.GEMINI_API_KEY` at compile time without checking it into version control.)*

3. **Open & Build with Android Studio**:
   - Launch Android Studio and choose **Open Project**.
   - Select the cloned directory and let Gradle sync.
   - Run the app on an Android Emulator or physical device (API 24+).

4. **Build via Command Line**:
   ```bash
   # Assemble debug APK
   gradle :app:assembleDebug

   # Run local unit tests
   gradle :app:testDebugUnitTest
   ```

---

## 📱 Permissions

The application requests only necessary permissions:
- `android.permission.INTERNET`: For communicating with the Gemini API and remote sync.
- `android.permission.RECORD_AUDIO`: For recording live lecture audio.
- Photo Picker: Uses zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) for whiteboard image selection.

---

## 📂 Project Structure

```
app/src/main/
├── java/com/example/
│   ├── audio/              # Audio recorder and playback controllers
│   ├── data/
│   │   ├── api/            # Gemini AI service and response parsers
│   │   ├── database/       # Room entities, DAOs, and database configuration
│   │   ├── model/          # StudySession, Flashcard, QuizQuestion models
│   │   └── repository/     # StudyRepository coordinating Room and AI logic
│   ├── ui/
│   │   ├── components/     # AudioPlayerBar, FlashcardView, QuizRunnerView, SyncDialog
│   │   ├── screens/        # HomeScreen, SessionDetailScreen, RecordingDialog, ScanWhiteboardDialog
│   │   ├── theme/          # Material 3 Color, Type, Shape & Bold Typography theme
│   │   └── viewmodel/      # StudyViewModel managing UI states
│   └── MainActivity.kt     # Single-activity Compose container
└── res/                    # Vector drawables, launcher icons, strings
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
