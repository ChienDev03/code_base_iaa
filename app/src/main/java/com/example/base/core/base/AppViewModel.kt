package com.example.base.core.base
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
/** UDF owner with buffered, lifecycle-collected one-shot effects. */
abstract class AppViewModel<S, E>(initial: S) : ViewModel() {
    private val mutableState = MutableStateFlow(initial)
    val state = mutableState.asStateFlow()
    private val channel = Channel<E>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    protected fun updateState(transform: (S) -> S) = mutableState.update(transform)
    protected suspend fun postEvent(event: E) = channel.send(event)
}
