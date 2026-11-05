package com.your.sourbase.navigation
import com.your.sourbase.features.example.presentation.ExampleRouteSerializers
import kotlinx.serialization.modules.SerializersModule
/**
 * Composes every feature's polymorphic `NavKey` registrations so saved-state restoration works.
 * Add a new feature by chaining `+ <feature>RouteSerializers`; each feature keeps its own routes
 * and subclass bindings in its own `*Navigation.kt`.
 */
val AppRouteSerializersModule: SerializersModule = ExampleRouteSerializers
