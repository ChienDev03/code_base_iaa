package com.example.base.navigation
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
/** Root list route. */
@Serializable data object ExampleList : NavKey
/** Small detail argument. Invalid IDs show an unavailable destination. */
@Serializable data class ExampleDetail(val id: Int) : NavKey
/** All saved back-stack route serializers are registered explicitly. */
val AppRouteSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ExampleList::class, ExampleList.serializer())
        subclass(ExampleDetail::class, ExampleDetail.serializer())
    }
}
