package com.your.sourbase.features.example.data
import com.your.sourbase.core.base.AppRepository
import com.your.sourbase.core.config.AppConfig
import com.your.sourbase.core.network.ApiResult
import com.your.sourbase.core.time.TimeProvider
import com.your.sourbase.core.logging.AppLog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
/** Example entity independent of transport/UI. */
data class Example(val id: Int, val title: String?, val body: String?)
/** Snapshot records freshness without masking failed remote requests. */
data class ExampleSnapshot(val items: List<Example>, val loadedAt: Long)
/** Repository boundary used by presentation. */
interface ExampleRepository { suspend fun load(): ApiResult<ExampleSnapshot> }
@Serializable
internal data class ExampleDto(val id: Int, val title: String, val body: String)
internal interface ExampleApi { @GET("posts") suspend fun posts(): List<ExampleDto> }
/** Local source intentionally contains neutral generated examples. */
class ExampleLocalSource @Inject constructor() {
    fun items() = (1..5).map { Example(it, null, null) }
}
/** One shared HTTP connection pool, bounded requests and no body/token diagnostics. */
@Singleton
class DefaultExampleRepository @Inject constructor(
    private val local: ExampleLocalSource,
    private val time: TimeProvider,
) : AppRepository(), ExampleRepository {
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).build()
    }
    private val api by lazy {
        Retrofit.Builder().baseUrl("https://jsonplaceholder.typicode.com/").client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build().create(ExampleApi::class.java)
    }
    override suspend fun load(): ApiResult<ExampleSnapshot> {
        AppLog.d("example.load.start")
        val result = request {
            // Both children must succeed. Structured scope cancels the sibling on failure.
            coroutineScope {
                val items = async {
                    if (AppConfig.current.publicApi) api.posts().take(100).map { Example(it.id, it.title, it.body) }
                    else local.items()
                }
                val freshness = async { time.epochMillis() }
                ExampleSnapshot(items.await(), freshness.await())
            }
        }
        AppLog.d(if (result is ApiResult.Success) "example.load.success" else "example.load.failure")
        return result
    }
}
