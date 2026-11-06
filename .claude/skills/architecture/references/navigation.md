# Navigation 3

Feature routes implement serializable NavKey and register in their feature serializer module.
AppRouteSerializersModule combines these modules for saved-state restoration.
Each feature provides an entries function; AppNavGraph owns the back stack and one snackbar host.
Use hiltViewModel with per-entry ViewModelStore ownership and lifecycle-aware effect collection.
Routes carry small identifiers, not records, credentials or mutable database objects.
The starter contains example destinations only; no authentication routes exist.
Analytics is not emitted automatically. New navigation and screen events need a product contract.
