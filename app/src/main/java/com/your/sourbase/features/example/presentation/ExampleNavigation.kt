package com.your.sourbase.features.example.presentation
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
/** Root list route. */
@Serializable data object ExampleList : NavKey
/** Small detail argument. Invalid IDs show an unavailable destination. */
@Serializable data class ExampleDetail(val id: Int) : NavKey
/** Polymorphic registrations for this feature's saved-state restoration. */
val ExampleRouteSerializers: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ExampleList::class, ExampleList.serializer())
        subclass(ExampleDetail::class, ExampleDetail.serializer())
    }
}
/** Owned entries for this feature. Back-stack mutation stays here, not in screens. */
fun EntryProviderScope<NavKey>.exampleEntries(
    stack: NavBackStack<NavKey>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    entry<ExampleList> {
        ExampleScreen(onOpen = { id ->
            if (id in 1..100 && stack.lastOrNull() == ExampleList) stack.add(ExampleDetail(id))
        }, selected = selected)
    }
    entry<ExampleDetail> { route ->
        ExampleDetail(route.id,
            onBack = { if (stack.lastOrNull() == route) stack.removeLastOrNull() },
            onChoose = {
                if (route.id in 1..100 && stack.lastOrNull() == route) {
                    onSelect(route.id)
                    stack.removeLastOrNull()
                }
            })
    }
}
