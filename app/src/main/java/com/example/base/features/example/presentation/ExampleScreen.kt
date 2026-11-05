package com.example.base.features.example.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.base.R
import com.example.base.core.ui.*
import com.example.base.ui.theme.*
/** Lifecycle-aware list entry. */
@Composable
fun ExampleScreen(onOpen: (Int) -> Unit, selected: Int?, viewModel: ExampleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExampleContent(state, selected, viewModel::reload, onOpen)
}
/** Stateless list and component catalog; loading and refresh have separate visuals. */
@Composable
fun ExampleContent(state: ExampleState, selected: Int?, reload: () -> Unit, onOpen: (Int) -> Unit) {
    AppPullToRefreshBox(state.refreshing, reload) {
        LazyColumn(Modifier.fillMaxSize().imePadding().padding(AppSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space16), overscrollEffect = null) {
            item {
                Text(stringResource(R.string.example_title), style = LocalAppTypography.current.title)
                selected?.let { Text(stringResource(R.string.example_selected, it), style = LocalAppTypography.current.body) }
            }
            if (state.loading) item { AppLoadingSkeleton(stringResource(R.string.example_loading)) }
            if (state.failed) item {
                Text(state.message ?: stringResource(R.string.example_load_error), style = LocalAppTypography.current.body)
                AppButton(stringResource(R.string.action_retry), reload)
            }
            items(state.items, key = { it.id }, contentType = { "example" }) { item ->
                AppButton(item.title ?: stringResource(R.string.example_item, item.id), { onOpen(item.id) })
            }
            item { AppButton(stringResource(R.string.action_refresh), reload, enabled = !state.loading && !state.refreshing) }
            item {
                Text(stringResource(R.string.example_catalog), style = LocalAppTypography.current.title)
                ExampleCatalog()
            }
        }
    }
}
/** Detail data is repository-owned; route carries only a bounded ID. */
@Composable
fun ExampleDetail(id: Int, onBack: () -> Unit, onChoose: () -> Unit, viewModel: ExampleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.items.find { it.id == id }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState(), overscrollEffect = null).padding(AppSpacing.space16), verticalArrangement = Arrangement.spacedBy(AppSpacing.space16)) {
        AppButton(stringResource(R.string.action_back), onBack)
        when {
            state.loading -> AppLoadingSkeleton(stringResource(R.string.example_loading))
            state.failed -> { Text(state.message ?: stringResource(R.string.example_load_error)); AppButton(stringResource(R.string.action_retry), viewModel::reload) }
            item == null -> Text(stringResource(R.string.example_missing), style = LocalAppTypography.current.body)
            else -> {
                Text(item.title ?: stringResource(R.string.example_item, item.id), style = LocalAppTypography.current.title)
                Text(item.body ?: stringResource(R.string.example_local_body), style = LocalAppTypography.current.body)
                AppButton(stringResource(R.string.action_choose), onChoose)
            }
        }
    }
}
/** Small interactive state catalog; no external side effects. */
@Composable
fun ExampleCatalog() {
    var draft by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space16)) {
        AppNeutralIcon(stringResource(R.string.example_neutral_icon))
        AppNeutralIcon(null, tint = MaterialTheme.colorScheme.error)
        AppField(draft, { draft = it.take(80) }, stringResource(R.string.example_input_label))
        if (!validExampleText(draft)) Text(stringResource(R.string.example_validation_error), color = MaterialTheme.colorScheme.error, style = LocalAppTypography.current.body)
        AppButton(stringResource(R.string.action_choose), {}, enabled = false)
        AppButton(stringResource(R.string.example_show_dialog), { dialog = true })
    }
    if (dialog) AppDialog(stringResource(R.string.example_catalog), { dialog = false }) {
        AppButton(stringResource(R.string.example_close_dialog), { dialog = false })
    }
}
@Preview(fontScale = 2f) @Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable
private fun CatalogPreview() { AppTheme { ExampleContent(ExampleState(loading = false), null, {}, {}) } }
