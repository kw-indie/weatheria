App Description
---------------
Fetch weather info for any location from OpenWeatherMap api.
Automatically detects your location, but you can also enter a name or coordinates manually.
Automatically updates weather info, but you can disable this feature and refresh manually.
When offline, you still get approximated info using the last update.
Beautiful backgrounds and animations to indicate local time of day for each location.
Supports Android APIs 21 and above.

Used Libraries
--------------
* [Foundation][0] - Components for core system capabilities, Kotlin extensions and support for
  multidex and automated testing.
  * [AppCompat][1] - Degrade gracefully on older versions of Android.
  * [Android KTX][2] - Write more concise, idiomatic Kotlin code.
* [Architecture][3] - A collection of libraries that help you design robust, testable, and
  maintainable apps. Start with classes for managing your UI component lifecycle and handling data
  persistence.
  * [Data Binding][4] - Declaratively bind observable data to UI elements.
  * [Lifecycle][5] - Create a UI that automatically responds to lifecycle events.
  * [LiveData][6] - Build data objects that notify views when the underlying database changes.
  * [Navigation][7] - Handle everything needed for in-app navigation.
  * [Room][8] - Access your app's SQLite database with in-app objects and compile-time checks.
  * [ViewModel][9] - Store UI-related data that isn't destroyed on app rotations. Easily schedule
     asynchronous tasks for optimal execution.
  * [WorkManager][10] - Manage your Android background jobs.
* [UI][11] - Details on why and how to use UI Components in your apps - together or separate
  * [Animations & Transitions][12] - Move widgets and transition between screens.
  * [Fragment][13] - A basic unit of composable UI.
  * [Layout][14] - Lay out widgets using different algorithms.
* Third party and miscellaneous libraries
  * [Glide][15] for image loading
  * [Hilt][16] for [dependency injection][17]
  * [Kotlin Coroutines][18] for managing background threads with simplified code and reducing needs for callbacks
  * [intuit-ssp][19] for scalable font sizes
  * [intuit-sdp][20] for scalable density pixels

[0]: https://developer.android.com/jetpack/components
[1]: https://developer.android.com/topic/libraries/support-library/packages#v7-appcompat
[2]: https://developer.android.com/kotlin/ktx
[3]: https://developer.android.com/jetpack/arch/
[4]: https://developer.android.com/topic/libraries/data-binding/
[5]: https://developer.android.com/topic/libraries/architecture/lifecycle
[6]: https://developer.android.com/topic/libraries/architecture/livedata
[7]: https://developer.android.com/topic/libraries/architecture/navigation/
[8]: https://developer.android.com/topic/libraries/architecture/room
[9]: https://developer.android.com/topic/libraries/architecture/viewmodel
[10]: https://developer.android.com/topic/libraries/architecture/workmanager
[11]: https://developer.android.com/guide/topics/ui
[12]: https://developer.android.com/training/animation/
[13]: https://developer.android.com/guide/components/fragments
[14]: https://developer.android.com/guide/topics/ui/declaring-layout
[15]: https://bumptech.github.io/glide/
[16]: https://developer.android.com/training/dependency-injection/hilt-android
[17]: https://developer.android.com/training/dependency-injection
[18]: https://kotlinlang.org/docs/reference/coroutines-overview.html
[19]: https://github.com/intuit/ssp
[20]: https://github.com/intuit/sdp