package com.your.sourbase
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.your.sourbase.features.example.presentation.ExampleContent
import com.your.sourbase.features.example.presentation.ExampleState
import com.your.sourbase.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
/** Render-only regression coverage; execution requires an authorized device run. */
class ExampleContentTest {
    @get:Rule val compose = createComposeRule()
    @Test fun initialFailureKeepsRetryAvailable() {
        compose.setContent { AppTheme { ExampleContent(ExampleState(loading = false, failed = true), null, {}, {}) } }
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
}
