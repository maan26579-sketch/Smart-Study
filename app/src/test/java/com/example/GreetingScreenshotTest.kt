package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun study_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.StudySessionCard(
          session = com.example.data.model.LectureSession(
            title = "Machine Learning: Optimization & Gradient Descent",
            subject = "Computer Science",
            sourceType = "AUDIO_LECTURE",
            rawContent = "Lecture notes",
            summary = "Summary of optimization landscapes and gradient descent update rules.",
            keyTakeawaysJson = "[]",
            formulasAndTermsJson = "[]",
            isSynced = true,
            isBookmarked = true
          ),
          onClick = {},
          onToggleBookmark = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/study_card.png")
  }
}
