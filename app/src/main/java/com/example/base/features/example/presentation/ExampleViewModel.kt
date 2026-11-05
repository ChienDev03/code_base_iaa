package com.example.base.features.example.presentation
import androidx.lifecycle.viewModelScope
import com.example.base.core.base.AppViewModel
import com.example.base.core.network.ApiResult
import com.example.base.core.ui.SnackbarController
import com.example.base.features.example.data.Example
import com.example.base.features.example.data.ExampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
/** Immutable first-load and refresh state. */
data class ExampleState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<Example> = emptyList(),
    val failed: Boolean = false,
    val message: String? = null,
)
/** Per-entry business owner; initial request runs once per ViewModel instance. */
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ExampleRepository,
    private val snackbar: SnackbarController,
) : AppViewModel<ExampleState, Nothing>(ExampleState()) {
    private val inFlight = AtomicBoolean(false)
    init { reload() }
    fun reload() {
        if (!inFlight.compareAndSet(false, true)) return
        viewModelScope.launch {
            val hasData = state.value.items.isNotEmpty()
            updateState { it.copy(loading = !hasData, refreshing = hasData, failed = false) }
            try {
                when (val result = repository.load()) {
                    is ApiResult.Success -> updateState { it.copy(items = result.data.items, failed = false, message = null) }
                    is ApiResult.Failure -> {
                        if (hasData) snackbar.show(result.error.message)
                        else updateState { it.copy(failed = true, message = result.error.message) }
                    }
                }
            } finally {
                updateState { it.copy(loading = false, refreshing = false) }
                inFlight.set(false)
            }
        }
    }
}
