package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.CollectionRecord
import com.example.ui.screens.MonthlyTotalCard
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
  fun collection_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        MonthlyTotalCard(
          record = CollectionRecord(
            id = 1,
            year = "2026",
            month = "September",
            particulars = "Maintenance Collections",
            flat1AAmount = 1000.0,
            flat1BAmount = 1000.0,
            flat2AAmount = 1000.0,
            flat2BAmount = 1000.0,
            flat3AAmount = 1000.0,
            flat3BAmount = 1000.0,
            totalAmount = 6000.0
          ),
          onEdit = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/collection_card.png")
  }
}
