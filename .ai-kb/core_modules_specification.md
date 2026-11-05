# Android Core Architecture Specification: Connectivity, Time Validation & Error Handling

## Repository adaptation notes

Imported user-provided reference: source language, implementation snippets, schemas and detailed
checklists are retained. Packages use `com.example.app`; feature entry points use `Example` names.
Snippets are reference code, not installed or compiled application code. Repository instructions
and the adaptation notes below take precedence when implementing them. Dependency versions are
retained source examples, not newly verified version recommendations. Tests, SDK installation,
remote configuration changes, publication and uploads are not performed by importing this file.

- Apply [design tokens and shared UI](design-system.md): use app typography, spacing, shapes,
  debounced/in-flight actions and lifecycle-aware state collection instead of copying raw UI styling.
- Preserve cancellation in every catch path. Inject clocks/dispatchers; do not classify TLS or
  arbitrary exception-message text as proof of clock drift. Never bypass TLS validation.
- Unknown/unavailable server time is distinct from confirmed drift. Auto-time settings and a
  reasonable year are hints, not proof of agreement with trusted time. Prefer an injected trusted
  server-time source and monotonic anchor; bound polling to the active lifecycle and one owner.
- The sample's network set and `checkNow()` snapshot need one serialized reconciliation policy:
  initial state, network switching and delayed loss callbacks must not publish stale offline state.
- Error precedence is joined nonblank `details[].message` → backend `message` → feature fallback
  → generic fallback. The sample's first-detail-only resolver and `localizedMessage` fallback do
  not meet that rule; never display exception text or raw bodies.
- Recovery retries remain opt-in for idempotent reads, bounded and cancellable. Do not replay
  ambiguous mutations. Smart reload must distinguish a failed/uninitialized load from legitimate
  empty success and must not duplicate an active load merely because `isLoading` is true.
- `TimeObserver` is referenced but not fully defined in this source; establish its state/await API
  when implementing it. Prefer injected recovery interfaces over mutable global registrations.
- Offline/time states use one shell-owned fullscreen blocker with centered content, not dialogs.
  Consume system/predictive Back and gate app pop callbacks; preserve the underlying stack.

Related contracts: [App update](features/app_update_specification.md),
[Rating and review](features/app_rating_review_specification.md).

---

Tài liệu đặc tả kỹ thuật này dành cho Coding Agent/Developer để triển khai chuẩn hoá 3 hệ thống nền tảng trong ứng dụng Android (Jetpack Compose + Clean Architecture / MVI + Kotlin Coroutines & Flow + Hilt):

1. **Network Connectivity Monitoring & Offline UI** (Lắng nghe mạng Realtime + Fullscreen Offline Screen)
2. **Device Clock Drift & Incorrect Time Validation** (Kiểm tra lệch giờ thiết bị so với Server + Fullscreen Blocking Screen)
3. **Enterprise Error Handling Architecture** (`ApiResult`, Backend Error Mapping, `AppRepository`, ViewModel & UI Presentation)

---

## 1. Network Connectivity Monitoring (`core/network`)

### Mục tiêu
- Sử dụng `ConnectivityManager.NetworkCallback` để phát hiện thay đổi mạng Realtime.
- Cung cấp `StateFlow<Boolean>` cho Compose UI và các Repository/ViewModel.
- Có hàm kiểm tra đồng bộ tức thì (`checkNow()`) và hàm suspend chờ có mạng (`awaitOnline()`).
- Hiển thị màn hình toàn phần với nội dung căn giữa, chặn Back và thao tác điều hướng khi mất mạng và điều hướng mở Settings mạng.
- `MainActivity` gọi `checkNow()` ở `onResume` và khi cửa sổ lấy lại focus (đóng Quick Settings), để không phụ thuộc hoàn toàn vào callback có thể bị OEM freezer làm trễ/rớt.

### 1.1 Interface: `core/network/ConnectivityObserver.kt`
```kotlin
package com.example.app.core.network

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    /** StateFlow thông báo trạng thái mạng realtime */
    val isOnline: StateFlow<Boolean>

    /** Kiểm tra ngay lập tức trạng thái mạng hiện tại (synchronous) */
    fun checkNow(): Boolean

    /** Suspend function chờ cho đến khi có mạng trở lại */
    suspend fun awaitOnline()
}
```

### 1.2 Triển khai: `core/network/ConnectivityMonitor.kt`
```kotlin
package com.example.app.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityMonitor @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : ConnectivityObserver {

    private val availableNetworks: MutableSet<Network> =
        Collections.synchronizedSet(mutableSetOf())

    private val _isOnline = MutableStateFlow(hasInternetConnection())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            updateNetworkCapability(network, capabilities)
        }

        override fun onLost(network: Network) {
            availableNetworks.remove(network)
            _isOnline.value = availableNetworks.isNotEmpty()
        }

        override fun onUnavailable() {
            availableNetworks.clear()
            _isOnline.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateNetworkCapability(network, networkCapabilities)
        }
    }

    init {
        AppRecoveryCoordinator.connectivityObserver = this
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }.onFailure {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }
    }

    override fun checkNow(): Boolean {
        val online = hasInternetConnection()
        _isOnline.value = online
        return online
    }

    override suspend fun awaitOnline() {
        if (checkNow()) return
        isOnline.filter { it }.first()
    }

    private fun updateNetworkCapability(network: Network, capabilities: NetworkCapabilities?) {
        val isValidated = capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (isValidated) {
            availableNetworks.add(network)
        } else {
            availableNetworks.remove(network)
        }
        _isOnline.value = availableNetworks.isNotEmpty()
    }

    private fun hasInternetConnection(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
```

### 1.3 Resource Strings: `res/values/strings.xml`
```xml
<resources>
    <!-- Offline Dialog -->
    <string name="offline_dialog_title">No Internet Connection</string>
    <string name="offline_dialog_message">It looks like you’ve lost connection. Please check your network settings and try again</string>
    <string name="offline_dialog_try_again">Try again</string>
    <string name="open_settings">Open Settings</string>
</resources>
```

### 1.4 Fullscreen offline screen: `core/ui/components/OfflineScreen.kt`

Offline and incorrect-time states use an opaque, full-window screen, not a `Dialog`, bottom sheet
or dismissible card. Center the icon/title/message/settings action group horizontally and vertically
inside the safe content area. Use app tokens and keep the action reachable on compact screens.
There is no close, toolbar Back, outside-dismiss or swipe-dismiss action.

```kotlin
package com.example.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app.R

/** Displays the blocking offline state through the shell's fullscreen status container. */
@Composable
fun OfflineScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FullscreenStatusScreen(
        title = stringResource(R.string.offline_dialog_title),
        message = stringResource(R.string.offline_dialog_message),
        actionLabel = stringResource(R.string.open_settings),
        onAction = onOpenSettings,
        modifier = modifier,
    )
}
```

The resource keys above remain compatible with the imported reference; their `dialog` prefix does
not mean the presentation still uses a dialog. New resources belong in the shared/core string file.

### 1.5 Shared centered container and Back ownership

Reference file: `core/ui/components/FullscreenStatusScreen.kt`. Imports use the example package;
map `AppButton` and theme imports to their actual locations in the consuming app.

```kotlin
package com.example.app.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.app.core.ui.AppButton
import com.example.app.ui.theme.AppSpacing
import com.example.app.ui.theme.LocalAppTypography
import kotlinx.coroutines.flow.collect

private val StatusContentMaxWidth = 400.dp

/** Consumes Back without dismissing the active status or changing the navigation stack. */
@Composable
fun BlockingStatusBackGuard() {
    BackHandler(enabled = true) { /* Remain on the blocking screen. */ }
    PredictiveBackHandler(enabled = true) { progress ->
        progress.collect { /* Consume gesture progress without popping or previewing a route. */ }
    }
}

/** Fills the app window with a centered, non-dismissible status and a settings action. */
@Composable
fun FullscreenStatusScreen(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(AppSpacing.space24),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = StatusContentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState(), overscrollEffect = null),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space16),
            ) {
                Text(
                    text = title,
                    style = LocalAppTypography.current.title,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = LocalAppTypography.current.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                AppButton(label = actionLabel, onClick = onAction)
            }
        }
    }
}
```

Use the supported Activity Compose version's predictive-back API/opt-in if required. Register the
Back guard at the active shell after navigation handlers so it owns dispatch while blocked. Consume
both a committed and cancelled gesture without changing app state; propagate coroutine cancellation.
App toolbar Back, custom edge-swipe and navigation command handlers must also check the same
blocking state before any `pop`/`navigateBack` call. System Back interception alone cannot guard
an app callback that directly mutates the stack.

The full-window surface covers the app content; system bars use the app's approved edge-to-edge
policy. Android Home, Recents and power controls are outside this app-navigation Back contract.
Do not attempt to trap those system actions. Settings is an explicit allowed action.

---

## 2. Device Clock Drift & Incorrect Time Validation (`core/util`)

### Mục tiêu
- Ngăn chặn lỗi SSL Handshake, JWT Token Invalid, countdown mismatch khi người dùng chỉnh giờ điện thoại thủ công bị lệch quá 5 phút.
- Gửi HEAD request siêu nhẹ lấy header `Date` từ Server.
- Lắng nghe broadcast `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_DATE_CHANGED`.
- Composable tự động re-check on Resume và Polling tự tắt popup khi người dùng chỉnh lại giờ đúng.

### 2.1 Utility & Composable: `core/util/TimeChecker.kt`
```kotlin
package com.example.app.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

@Composable
fun rememberDeviceTimeCorrect(): State<Boolean?> {
    val context = LocalContext.current
    val isDeviceTimeCorrect = remember { mutableStateOf<Boolean?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val checkTime: () -> Unit = {
        coroutineScope.launch {
            isDeviceTimeCorrect.value = context.isDeviceTimeCorrect()
        }
    }

    LaunchedEffect(Unit) {
        checkTime()
    }

    LaunchedEffect(isDeviceTimeCorrect.value) {
        if (isDeviceTimeCorrect.value == false) {
            while (true) {
                delay(2000)
                val currentStatus = context.isDeviceTimeCorrect()
                if (currentStatus != isDeviceTimeCorrect.value) {
                    isDeviceTimeCorrect.value = currentStatus
                    if (currentStatus) break
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkTime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                checkTime()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return isDeviceTimeCorrect
}

suspend fun Context.isDeviceTimeCorrect(serverUrl: String = "https://google.com"): Boolean = withContext(Dispatchers.IO) {
    val autoTime = try {
        Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1
    } catch (e: Exception) { true }

    val autoTimeZone = try {
        Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 0) == 1
    } catch (e: Exception) { true }

    val networkTime = getNetworkHeaderTime(serverUrl)
    if (networkTime != null) {
        val diff = abs(System.currentTimeMillis() - networkTime)
        return@withContext diff <= 5 * 60 * 1000L // Độ lệch cho phép: tối đa 5 phút
    }

    val calendar = java.util.Calendar.getInstance()
    val isReasonableYear = calendar.get(java.util.Calendar.YEAR) >= 2025
    return@withContext autoTime && autoTimeZone && isReasonableYear
}

private fun getNetworkHeaderTime(targetUrl: String): Long? {
    return try {
        val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 3000
            readTimeout = 3000
            instanceFollowRedirects = false
        }
        connection.connect()
        val serverDate = connection.date
        connection.disconnect()
        if (serverDate > 0) serverDate else null
    } catch (e: Exception) {
        null
    }
}
```

### 2.2 Resource Strings: `res/values/strings.xml`
```xml
<resources>
    <!-- Incorrect Device Time Dialog -->
    <string name="error_incorrect_device_time_title">Incorrect device time</string>
    <string name="error_incorrect_device_time_subtitle">Your device time does not match our system time. Please update it to continue using all app features</string>
    <string name="open_settings">Open Settings</string>
</resources>
```

### 2.3 Fullscreen incorrect-time screen: `core/ui/components/IncorrectTimeScreen.kt`

```kotlin
package com.example.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.app.R

/** Displays confirmed device-time drift in the shared fullscreen status container. */
@Composable
fun IncorrectTimeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FullscreenStatusScreen(
        title = stringResource(R.string.error_incorrect_device_time_title),
        message = stringResource(R.string.error_incorrect_device_time_subtitle),
        actionLabel = stringResource(R.string.open_settings),
        onAction = onOpenSettings,
        modifier = modifier,
    )
}
```

Center content exactly as on the offline screen. The shell owns Back blocking and the safe settings
launcher; use `Settings.ACTION_DATE_SETTINGS` with a guarded general-settings fallback. A failed
launch retains the blocking screen and uses the shared error-feedback policy.

Only confirmed drift shows this screen. An unknown time result is not drift. Returning from
settings does not itself clear the state: recheck time and remove the blocker only after confirmed
recovery. Polling is bounded to the active owner/lifecycle and stops when no longer needed.

---

## 3. Enterprise Error Handling Architecture (`core/network` & `core/base`)

### Mục tiêu
- Chuẩn hoá kết quả API trả về bằng `ApiResult<T>` (`Success`, `Error`, `Loading`).
- Parse chi tiết body lỗi từ Backend (`ServerErrorResponse`).
- Phân loại rõ `ApiErrorType` (Validation 400/422, Unauthorized 401, Forbidden 403, NotFound 404, Server 5xx, Network, Timeout...).
- Không nuốt `CancellationException` để Coroutine huỷ đúng theo vòng đời Compose/ViewModel.
- Tầng Presentation hiển thị lỗi linh hoạt (Inline Error có nút Retry hoặc Toast/Snackbar thông báo).

### 3.1 Type & Result Wrapper: `core/network/ApiResult.kt` & `ApiErrorType.kt`
```kotlin
package com.example.app.core.network

enum class ApiErrorType {
    Network,
    Timeout,
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
    Validation,
    TooManyRequests,
    Server,
    Serialization,
    Unknown
}

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    
    data class Error(
        val type: ApiErrorType,
        val message: String,
        val cause: Throwable? = null,
        val statusCode: Int? = null,
        val errorType: String? = null
    ) : ApiResult<Nothing>

    data object Loading : ApiResult<Nothing>

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    fun onSuccess(action: (T) -> Unit): ApiResult<T> = apply {
        if (this is Success) action(data)
    }

    fun onError(action: (Error) -> Unit): ApiResult<T> = apply {
        if (this is Error) action(this)
    }

    fun getOrNull(): T? = (this as? Success)?.data
}
```

### 3.2 Backend Error DTO: `core/network/ServerErrorResponse.kt`
```kotlin
package com.example.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerErrorResponse(
    @SerialName("message") val message: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("errorType") val errorType: String? = null,
    @SerialName("details") val details: List<ServerErrorDetail>? = null
) {
    fun resolvedMessage(): String {
        val detailMsg = details?.firstOrNull { it.message.isNotBlank() }?.message
        return detailMsg ?: message ?: error ?: "Đã có lỗi xảy ra, vui lòng thử lại"
    }
}

@Serializable
data class ServerErrorDetail(
    @SerialName("property") val property: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("message") val message: String = ""
)
```

### 3.3 Centralized Error Messages: `core/network/ApiErrorMessages.kt`
Tất cả các câu thông báo lỗi cho người dùng được gom tập trung vào một file để dễ dàng quản lý, chỉnh sửa hoặc localize.

```kotlin
package com.example.app.core.network

/**
 * Nơi tập trung toàn bộ message lỗi cho người dùng của tầng Network.
 */
object ApiErrorMessages {
    /** Thiết bị mất kết nối mạng */
    const val NO_INTERNET =
        "You're currently offline. Please check your internet connection!"

    /** Hết thời gian chờ kết nối */
    const val CONNECTION_TIMEOUT =
        "The connection to server took too long. Please check your connection and try again!"

    /** Kết nối nhưng không gửi được dữ liệu lên server */
    const val SEND_TIMEOUT =
        "Unable to send data to server. Please check your connection and try again!"

    /** Server xử lý quá lâu không phản hồi */
    const val RECEIVE_TIMEOUT =
        "The request is taking longer than expected. Please check your connection and try again!"

    /** Lỗi chứng chỉ SSL / HTTPS bị chặn */
    const val BAD_CERTIFICATE =
        "Secure connection failed. Please check your connection and try again!"

    /** Lỗi chung phía server không xác định */
    const val BAD_RESPONSE =
        "An error occurred while processing the request. Please try again later!"

    /** Request bị huỷ */
    const val REQUEST_CANCELED =
        "The request was canceled. Please try again later!"

    /** Không thể kết nối tới server (sai host, server down) */
    const val CONNECTION_ERROR =
        "Unable to connect to server. Please check your connection and try again!"

    /** Lỗi không xác định */
    const val UNKNOWN_ERROR =
        "Oops! Something went wrong. Please try again later!"

    /** Lỗi parse JSON sai shape/type */
    const val MAPPING_TYPE_ERROR =
        "We encountered an issue while processing the data. Please try again later!"

    /** HTTP 5xx - Lỗi hệ thống máy chủ */
    const val SERVER_ERROR =
        "The system is currently experiencing an issue. Please try again later!"

    /** Dữ liệu từ server trả về sai nghiệp vụ */
    const val INVALID_DATA_SERVER =
        "The server returned invalid data. Please try again later!"

    /** HTTP 429 - Quá nhiều request */
    const val TOO_MANY_REQUESTS =
        "You are operating too fast. Please try again later!"

    /** HTTP 400 / 422 - Sai định dạng đầu vào */
    const val VALIDATION_ERROR =
        "Invalid data. Please check your information and try again!"

    /** HTTP 404 - Không tìm thấy */
    const val NOT_FOUND =
        "We couldn't find what you're looking for. Please try again!"

    /** HTTP 409 - Dữ liệu bị trùng / xung đột */
    const val CONFLICT =
        "Data already exists or is in conflict. Please try again!"

    /** HTTP 403 - Không có quyền */
    const val FORBIDDEN =
        "You do not have permission to perform this action."

    /** HTTP 401 - Hết phiên / sai token */
    const val UNAUTHORIZED =
        "We couldn't verify your session. Please restart the app and try again!"

    /** Lỗi đọc/ghi Cache local Room */
    const val CACHE_ERROR =
        "We couldn't load your data. Please restart the app and try again!"
}
```

### 3.4 Mapper chuyển Exception sang `ApiResult.Error`: `core/network/ApiErrorMapper.kt`
```kotlin
package com.example.app.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

object ApiErrorMapper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun map(throwable: Throwable): ApiResult.Error {
        // Bắt buộc rethrow CancellationException để Coroutine huỷ đúng lifecycle
        if (throwable is CancellationException) throw throwable
        if (throwable.cause is CancellationException) throw (throwable.cause as CancellationException)
        if (throwable is IOException && throwable.message?.contains("canceled", ignoreCase = true) == true) {
            throw kotlinx.coroutines.CancellationException("Request was canceled", throwable)
        }
        
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val errorBody = throwable.response()?.errorBody()?.string()
                val serverError = errorBody?.let {
                    runCatching { json.decodeFromString<ServerErrorResponse>(it) }.getOrNull()
                }

                ApiResult.Error(
                    type = httpCodeToErrorType(code),
                    message = serverError?.resolvedMessage() ?: httpCodeToMessage(code),
                    cause = throwable,
                    statusCode = code,
                    errorType = serverError?.errorType
                )
            }
            is SocketTimeoutException -> ApiResult.Error(
                type = ApiErrorType.Timeout,
                message = ApiErrorMessages.CONNECTION_TIMEOUT,
                cause = throwable
            )
            is UnknownHostException -> ApiResult.Error(
                type = ApiErrorType.Network,
                message = ApiErrorMessages.NO_INTERNET,
                cause = throwable
            )
            is IOException -> ApiResult.Error(
                type = ApiErrorType.Network,
                message = ApiErrorMessages.CONNECTION_ERROR,
                cause = throwable
            )
            is SerializationException -> ApiResult.Error(
                type = ApiErrorType.Serialization,
                message = ApiErrorMessages.MAPPING_TYPE_ERROR,
                cause = throwable
            )
            else -> ApiResult.Error(
                type = ApiErrorType.Unknown,
                message = throwable.localizedMessage ?: ApiErrorMessages.UNKNOWN_ERROR,
                cause = throwable
            )
        }
    }

    fun httpCodeToErrorType(code: Int): ApiErrorType = when (code) {
        400, 422 -> ApiErrorType.Validation
        401 -> ApiErrorType.Unauthorized
        403 -> ApiErrorType.Forbidden
        404 -> ApiErrorType.NotFound
        408 -> ApiErrorType.Timeout
        409 -> ApiErrorType.Conflict
        429 -> ApiErrorType.TooManyRequests
        in 500..599 -> ApiErrorType.Server
        else -> ApiErrorType.Unknown
    }

    private fun httpCodeToMessage(code: Int): String = when (code) {
        400, 422 -> ApiErrorMessages.VALIDATION_ERROR
        401 -> ApiErrorMessages.UNAUTHORIZED
        403 -> ApiErrorMessages.FORBIDDEN
        404 -> ApiErrorMessages.NOT_FOUND
        408 -> ApiErrorMessages.CONNECTION_TIMEOUT
        409 -> ApiErrorMessages.CONFLICT
        429 -> ApiErrorMessages.TOO_MANY_REQUESTS
        in 500..599 -> ApiErrorMessages.SERVER_ERROR
        else -> ApiErrorMessages.UNKNOWN_ERROR
    }
}
```

### 3.5 Global Recovery Coordinator & Base Repository

#### 3.5.1 `core/network/AppRecoveryCoordinator.kt`
Tập trung điều phối trạng thái phục hồi của Mạng và Đồng hồ thiết bị. Cho phép các hàm gọi API tự động suspend và retry khi mạng phục hồi hoặc khi giờ thiết bị được chỉnh đúng.

```kotlin
package com.example.app.core.network

import com.example.app.core.time.TimeObserver
import kotlinx.coroutines.delay

object AppRecoveryCoordinator {
    @Volatile
    var connectivityObserver: ConnectivityObserver? = null

    @Volatile
    var timeObserver: TimeObserver? = null

    fun isOffline(): Boolean = connectivityObserver?.isOnline?.value == false
    fun isTimeDrifted(): Boolean = timeObserver?.isTimeCorrect?.value == false

    suspend fun awaitRecoveryIfUnhealthy() {
        if (isOffline()) {
            connectivityObserver?.awaitOnline()
            delay(300L)
        }
        if (isTimeDrifted()) {
            timeObserver?.awaitValidTime()
            delay(300L)
        }
    }

    suspend fun awaitRecovery() {
        connectivityObserver?.awaitOnline()
        timeObserver?.awaitValidTime()
        delay(500L)
    }
}
```

#### 3.5.2 `core/base/AppRepository.kt`
Tích hợp cơ chế kiểm soát phục hồi mạng/giờ (`autoRetryOnRecovery = false` theo mặc định để tránh retry mù quáng các mutation không idempotent như POST/DELETE/restore; opt-in `autoRetryOnRecovery = true` cho các API đọc idempotent) và cung cấp các hàm overload hỗ trợ positional lambda trailing:

```kotlin
package com.example.app.core.base

import com.example.app.core.network.ApiErrorMapper
import com.example.app.core.network.ApiErrorType
import com.example.app.core.network.ApiResult
import com.example.app.core.network.AppRecoveryCoordinator
import java.io.IOException
import javax.net.ssl.SSLException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

abstract class AppRepository {

    protected suspend fun <T> safeApiCall(
        block: suspend () -> T,
    ): ApiResult<T> = safeApiCall(autoRetryOnRecovery = false, maxRetries = 2, block = block)

    protected suspend fun <T> safeApiCall(
        autoRetryOnRecovery: Boolean = false,
        maxRetries: Int = 2,
        block: suspend () -> T,
    ): ApiResult<T> {
        var attempts = 0
        while (true) {
            attempts++
            currentCoroutineContext().ensureActive()
            try {
                if (autoRetryOnRecovery) {
                    AppRecoveryCoordinator.awaitRecoveryIfUnhealthy()
                }
                currentCoroutineContext().ensureActive()
                val data = block()
                return ApiResult.Success(data)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                currentCoroutineContext().ensureActive()

                val isNetworkIssue = isNetworkFailure(t) || AppRecoveryCoordinator.isOffline()
                val isTimeIssue = isTimeFailure(t) || AppRecoveryCoordinator.isTimeDrifted()

                if (autoRetryOnRecovery && (isNetworkIssue || isTimeIssue) && attempts <= maxRetries) {
                    AppRecoveryCoordinator.awaitRecovery()
                    continue
                }

                return ApiErrorMapper.map(t)
            }
        }
    }

    protected fun <T> safeApiFlow(
        block: suspend () -> T,
    ): Flow<ApiResult<T>> = safeApiFlow(autoRetryOnRecovery = false, block = block)

    protected fun <T> safeApiFlow(
        autoRetryOnRecovery: Boolean = false,
        block: suspend () -> T,
    ): Flow<ApiResult<T>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall(autoRetryOnRecovery = autoRetryOnRecovery, block = block))
    }

    protected fun <T> cacheThenNetwork(
        cache: Flow<T>,
        networkCall: suspend () -> T,
    ): Flow<ApiResult<T>> = cacheThenNetwork(cache = cache, autoRetryOnRecovery = false, networkCall = networkCall)

    protected fun <T> cacheThenNetwork(
        cache: Flow<T>,
        autoRetryOnRecovery: Boolean = false,
        networkCall: suspend () -> T,
    ): Flow<ApiResult<T>> = flow {
        cache.firstOrNull()?.let { cached -> emit(ApiResult.Success(cached)) }
        emit(safeApiCall(autoRetryOnRecovery = autoRetryOnRecovery, block = networkCall))
    }

    private fun isNetworkFailure(t: Throwable): Boolean {
        if (t is IOException) return true
        val errorResult = ApiErrorMapper.map(t)
        return errorResult.type == ApiErrorType.Network || errorResult.type == ApiErrorType.Timeout
    }

    private fun isTimeFailure(t: Throwable): Boolean {
        if (t is SSLException) return true
        val msg = t.message?.lowercase().orEmpty()
        return msg.contains("time") || msg.contains("date") || msg.contains("cert")
    }
}
```

#### 3.5.3 Tự động nạp lại dữ liệu (Smart Auto-Reload) khi có kết nối Internet trở lại
Khi thiết bị khởi động offline hoặc gặp sự cố mất mạng khiến lần load dữ liệu ban đầu/đang tải bị gián đoạn (UI ghi nhận `error`, `isLoading == true` hoặc rỗng), việc kết nối mạng phục hồi (`isOnline == true`) sẽ tự động nạp lại dữ liệu:
- Các feature ViewModel tại các màn hình đích chính (các tab điều hướng chính / main bottom navigation destinations) inject `ConnectivityObserver` và quan sát luồng `connectivityObserver.isOnline`.
- **Cơ chế nạp có điều kiện (Smart Recovery):** CHỈ tự động kích hoạt nạp lại dữ liệu của màn hình/tab đang hiển thị khi `isOnline == true` VÀ trạng thái hiện tại thoả mãn điều kiện lỗi/nghẽn: `state.error != null`, đang tải dở (`isLoading == true`), hoặc dữ liệu màn hình đang rỗng/chưa tải.
- **Tránh spam request:** Nếu dữ liệu đã tải thành công trước đó (`error == null` và đã có nội dung), việc mạng chập chờn hay đổi mạng (Wi-Fi $\leftrightarrow$ 4G) sẽ KHÔNG tự reload để tránh giật lag UI và tiết kiệm băng thông. Người dùng có thể dùng Pull-to-refresh nếu muốn chủ động làm mới.
- Nhờ vậy, ngay khi màn hình offline được gỡ sau khi xác nhận phục hồi sau sự cố nghẽn mạng/offline, màn hình đang hiển thị tự động đồng bộ dữ liệu mới nhất mà người dùng không cần thao tác thêm.

---

## 4. Hilt Dependency Injection Setup (`core/di`)

```kotlin
// core/di/CoreNetworkModule.kt
package com.example.app.core.di

import android.content.Context
import android.net.ConnectivityManager
import com.example.app.core.network.ConnectivityMonitor
import com.example.app.core.network.ConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreNetworkModule {

    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        connectivityManager: ConnectivityManager
    ): ConnectivityObserver = ConnectivityMonitor(connectivityManager)
}
```

---

## 5. Shell integration and navigation gate

One shell owner selects a single blocking state. Priority is **offline first**, then confirmed
incorrect time, then normal content; this avoids stacking screens and lets network recovery happen
before an online time recheck. On network recovery, a still-confirmed drift state keeps access
blocked until time validation succeeds. No navigation route is pushed or popped for either status.

```kotlin
// Example shell-owned state, not a new navigation destination.
enum class CoreBlockingState { None, Offline, IncorrectTime }

fun resolveCoreBlockingState(
    isOnline: Boolean,
    isTimeCorrect: Boolean?,
): CoreBlockingState = when {
    !isOnline -> CoreBlockingState.Offline
    isTimeCorrect == false -> CoreBlockingState.IncorrectTime
    else -> CoreBlockingState.None
}
```

```kotlin
// Inside the existing app shell; imports omitted for the reference fragment.
val isOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle()
val isTimeCorrect by timeObserver.isTimeCorrect.collectAsStateWithLifecycle()
val blockingState = resolveCoreBlockingState(isOnline, isTimeCorrect)

// Keep the existing navigation state/saveable-state/entry owners above this content gate.
// Park normal destination UI while blocked; do not clear or recreate its back stack.
when (blockingState) {
    CoreBlockingState.None -> AppNavHost()
    CoreBlockingState.Offline -> OfflineScreen(onOpenSettings = openNetworkSettings)
    CoreBlockingState.IncorrectTime -> IncorrectTimeScreen(onOpenSettings = openDateSettings)
}

// Registered after the navigation content; only present while a blocker owns the shell.
if (blockingState != CoreBlockingState.None) {
    BlockingStatusBackGuard()
}
```

`timeObserver` is an injected shared owner of trusted-time state. Its complete implementation is
outside the pasted reference; do not add another polling loop inside each status screen. Retain
`checkNow()` on Activity resume/window-focus regain and trigger the shared time recheck on resume
and relevant time broadcasts. Dismiss the old editor's IME on entering a blocker.

All app navigation entry points must use the same gate. Example policy before processing a toolbar
Back or a custom pop request:

```kotlin
if (blockingState == CoreBlockingState.None) {
    navigationOwner.navigateBack() // Example existing shell navigation callback.
}
```

Do not queue a Back press for replay after recovery. Do not navigate to a previous route to remove
the blocker. Settings launch is allowed, but returning without fixing the condition remains blocked.
Retain the underlying navigation/saveable state and per-entry ViewModel ownership while parked;
confirm the shell implementation supports this before lifting a Navigation host in/out of composition.
Suspend competing update/review/ad overlays while a core blocker is visible.

### Verification cases

- Offline/time content fills the app window; its content group is centered with no dialog scrim/card.
- Compact viewport/IME/insets keep the settings action accessible without enabling navigation escape.
- Toolbar Back, device Back, three-button Back and predictive Back (commit/cancel/repeat) do not pop.
- A custom edge-swipe or direct app `navigateBack` request is rejected while blocked.
- Repeated settings taps are guarded; returning without repair retains the same blocking state.
- Offline plus incorrect time shows only offline; recovery then time validation selects one state.
- Recovery restores the same destination and draft without a synthetic pop or replayed Back.
- Rotation/background/recreation retains the correct blocker and does not duplicate observers.

These are documentation cases only; no runtime/device/Maestro checks were executed. Automated
runtime tests require explicit confirmation under repository instructions.

---

## 6. Hướng dẫn sử dụng trong Feature ViewModel & UI

```kotlin
// Example Feature ViewModel
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ExampleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    private val _event = Channel<ExampleUiEvent>(Channel.BUFFERED)
    val event: Flow<ExampleUiEvent> = _event.receiveAsFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, inlineError = null) }
            
            when (val result = repository.fetchData()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, data = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    
                    if (result.type == ApiErrorType.Unauthorized) {
                        _event.send(ExampleUiEvent.NavigateToLogin)
                    } else if (_uiState.value.data == null) {
                        // Hiển thị Inline Error nếu chưa có data (Full screen error with retry)
                        _uiState.update { it.copy(inlineError = result.message) }
                    } else {
                        // Đã có data hiển thị, chỉ bắn Toast/Snackbar báo lỗi refresh
                        _event.send(ExampleUiEvent.ShowToast(result.message))
                    }
                }
                is ApiResult.Loading -> Unit
            }
        }
    }
}
```
