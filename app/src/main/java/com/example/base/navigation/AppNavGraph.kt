package com.example.base.navigation
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.example.base.R
import com.example.base.core.ui.SnackbarController
import com.example.base.features.example.presentation.ExampleScreen
import com.example.base.features.example.presentation.ExampleDetail as DetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
/** App-scoped messaging access, separate from screen ownership. */
@HiltViewModel
class ShellViewModel @Inject constructor(val snackbar: SnackbarController) : ViewModel()
/** Saved routes, per-entry stores and durable result state. No external deep links are registered. */
@Composable
fun AppNavGraph(shell: ShellViewModel = hiltViewModel()) {
    val configuration = remember { SavedStateConfiguration { serializersModule = AppRouteSerializersModule } }
    val stack = rememberNavBackStack(configuration, ExampleList)
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val host = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val fallback = stringResource(R.string.example_load_error)
    LaunchedEffect(shell, lifecycle, fallback) {
        shell.snackbar.events.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { host.showSnackbar(it ?: fallback) }
    }
    Scaffold(snackbarHost = { SnackbarHost(host) }) { padding ->
        NavDisplay(
            modifier = Modifier.padding(padding), backStack = stack,
            onBack = { if (stack.size > 1) stack.removeLastOrNull() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
            entryProvider = entryProvider {
                entry<ExampleList> {
                    ExampleScreen(onOpen = { id ->
                        if (id in 1..100 && stack.lastOrNull() == ExampleList) stack.add(ExampleDetail(id))
                    }, selected = selected)
                }
                entry<ExampleDetail> { route ->
                    DetailScreen(route.id,
                        onBack = { if (stack.lastOrNull() == route) stack.removeLastOrNull() },
                        onChoose = {
                            if (route.id in 1..100 && stack.lastOrNull() == route) {
                                selected = route.id
                                stack.removeLastOrNull()
                            }
                        })
                }
            },
        )
    }
}
