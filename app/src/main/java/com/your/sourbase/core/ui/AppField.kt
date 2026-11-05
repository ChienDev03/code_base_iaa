package com.your.sourbase.core.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.your.sourbase.R
import com.your.sourbase.ui.theme.*
private val FieldMinHeight = 48.dp
private val FieldBorder = 1.dp
private val NeutralIconSize = 24.dp
/** Reusable input; draft remains caller-owned and disabled state applies to every editing path. */
@Composable
fun AppField(value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space08)) {
        Text(label, style = LocalAppTypography.current.body)
        BasicTextField(value, onValueChange, enabled = enabled,
            textStyle = LocalAppTypography.current.body.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().heightIn(min = FieldMinHeight)
                .border(FieldBorder, MaterialTheme.colorScheme.outline, AppShapes.medium).padding(AppSpacing.space16))
    }
}
/** App-owned neutral vector; caller controls tint and semantic description. */
@Composable
fun AppNeutralIcon(description: String?, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(painterResource(R.drawable.ic_example), description, tint = tint, modifier = Modifier.size(NeutralIconSize))
}
/** Simple local validation; remote rules remain at the feature repository. */
fun validExampleText(value: String): Boolean = value.trim().length in 1..80
/** Caller-owned visibility; outside tap and system Back share the same dismiss action. */
@Composable
fun AppDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AppShapes.medium) {
            Column(Modifier.padding(AppSpacing.space24), verticalArrangement = Arrangement.spacedBy(AppSpacing.space16)) {
                Text(title, style = LocalAppTypography.current.title)
                content()
            }
        }
    }
}
