package com.your.sourbase.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/** Injected dispatchers for blocking local IO and CPU work; Room manages suspend queries. */
class AppDispatchers @Inject constructor() {
    val io: CoroutineDispatcher = Dispatchers.IO
    val default: CoroutineDispatcher = Dispatchers.Default
}
