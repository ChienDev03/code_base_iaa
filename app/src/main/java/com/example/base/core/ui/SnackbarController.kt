package com.example.base.core.ui
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton
/** App-scoped bounded message queue; UI resolves fallback resources. */
@Singleton
class SnackbarController @Inject constructor() {
    private val messages = Channel<String?>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    suspend fun show(message: String?) { messages.send(message) }
}
