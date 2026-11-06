package com.your.sourbase.features.example.data

import com.your.sourbase.core.base.AppRepository
import com.your.sourbase.core.data.DataResult
import com.your.sourbase.core.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Neutral UI demonstration item, separate from persisted financial data. */
data class Example(val id: Int, val title: String?, val body: String?)
/** Local snapshot with an injected timestamp. */
data class ExampleSnapshot(val items: List<Example>, val loadedAt: Long)
/** Repository boundary used by the starter presentation. */
interface ExampleRepository { suspend fun load(): DataResult<ExampleSnapshot> }
/** Local-only demonstration; never creates financial records. */
@Singleton
class DefaultExampleRepository @Inject constructor(
    private val time: TimeProvider,
) : AppRepository(), ExampleRepository {
    override suspend fun load(): DataResult<ExampleSnapshot> = local {
        ExampleSnapshot((1..5).map { Example(it, null, null) }, time.epochMillis())
    }
}
