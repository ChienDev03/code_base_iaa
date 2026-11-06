# Local feature templates

Use the actual ExampleViewModel, ExampleScreen and ExampleNavigation for UI/navigation patterns.
Do not copy nonexistent marker types, tracker services or feedback APIs from former products.

## Repository

```kotlin
interface FeatureRepository {
    suspend fun load(): DataResult<FeatureSnapshot>
}

@Singleton
class RoomFeatureRepository @Inject constructor(
    private val database: MoneyDatabase,
) : AppRepository(), FeatureRepository {
    override suspend fun load(): DataResult<FeatureSnapshot> = local {
        // Read the needed DAO query and map to a feature snapshot.
    }
}
```

Use validated writes, integer money and Room transactions. Keep SDK calls out of database code.
Register @Binds in core/di/RepositoryModule.

## ViewModel outcome

```kotlin
when (val result = repository.load()) {
    is DataResult.Success -> updateState { it.copy(items = result.data.items) }
    DataResult.Failure -> snackbar.show(null) // generic fallback; use approved feature resources in UI
}
```

Wrap in viewModelScope, preserve cancellation, clear loading/in-flight guards in finally.
Keep first-load and refresh state separate and preserve displayed content during refresh.
AppViewModel<S,E> has no marker constraints; postEvent is suspend.
Do not emit arbitrary event names or log stored values.
