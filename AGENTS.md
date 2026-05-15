# AGENTS.md

## Project map
- Launcher entry point: `app/src/main/java/com/electro/todolist/ui/home/TasksActivity.kt`.
- `TasksActivity` is the UI hub: it wires `TasksAdapter`, `ItemTouchHelperCallback`, Activity Result launchers for import/export, the language popup, and bottom sheets.
- Main state lives in `TasksViewModel`; the Activity should mostly observe `tasks`, `currentListName`, `allLists`, and `snackbarEventFlow`.
- Data is split by concern: Room for tasks (`AppDatabase`, `TaskDao`), `SharedPreferences` for list metadata (`ListManager`), and SAF file I/O (`FileManager`).
- Secondary flows are separate activities: `TaskDetailsActivity` edits a task and can launch `FlowActivity`; `SettingsActivity` hosts `PreferenceFragmentCompat`.

## Data and persistence rules
- `Task` is both a Room `@Entity(tableName = "tasks")` and `@Serializable`; keep fields JSON- and Room-friendly.
- `SerialListObject` stores list id/title/position; list IDs are opaque strings, while titles are user-facing.
- `TaskDao` orders by `done ASC, position ASC`; drag/drop and top-insert behavior depend on that ordering.
- `TaskDiffCallback` intentionally ignores `position` to avoid double animations during reorder.
- `ListManager` persists the last opened list id and creates default lists (`list1`, `list2`, `list3`) on first launch.

## UI / interaction conventions
- Bottom-sheet actions are routed through `BottomFragmentActions` in `ui/fragments/ContextSettingsFragment.kt`.
- `TasksAdapter` is callback-driven: checkbox, swipe-left delete, swipe-right done, item click, and reorder all delegate back to the Activity/ViewModel.
- Keep user-facing strings and snackbar copy consistent with the existing French UI (`"Tâche supprimée"`, `"Liste créée"`, etc.).
- Logging uses `Timber`; prefer it over `println`/`Log` when tracing flows.
- Compose exists in the project, but the app is still primarily ViewBinding + XML; do not assume a full Compose migration.

## External integrations
- Firebase is configured via `app/google-services.json` and Gradle Firebase deps; `google-services` is applied in `app/build.gradle.kts`.
- Login code under `ui/login/` is still a placeholder flow (`LoginDataSource` returns a fake user); do not treat it as production auth.
- Other notable deps: `Room` + KSP, `DataStore`, `kotlinx.serialization`, `Lottie`, `SparkButton`, `Timber`, and `PronoteLib`.

## Build / test workflow
- Debug build: `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows).
- JVM tests: `./gradlew test`.
- Instrumented flows: `./gradlew connectedAndroidTest`.
- Use `./gradlew clean` if generated binding or Room artifacts get stale.
- When changing Gradle, manifests, signing, or dependencies, rebuild before editing UI logic further.

## Files worth checking first
- `app/build.gradle.kts` for SDK levels, signing, and dependency versions.
- `app/src/main/AndroidManifest.xml` for launch activities and permissions.
- `app/src/main/java/com/electro/todolist/ui/home/TasksViewModel.kt` and `.../data/repository/TasksRepository.kt` for state/data flow.
- `app/src/main/java/com/electro/todolist/ui/fragments/ContextSettingsFragment.kt` for main bottom-sheet action wiring.

## Drag & Drop and UI State Rules
This codebase relies on complex user interactions (e.g., Drag & Drop, Swipe) mixed with a reactive database (Room tracking `Flow`). To avoid race conditions and UI glitches, strictly observe the following rules:

1. **Avoid `ListAdapter` for Drag & Drop:**
   Do NOT use `ListAdapter` when you need real-time, gesture-based reordering. `ListAdapter` uses asynchronous `DiffUtil` calculations that will conflict with synchronous touch events. Use a standard `RecyclerView.Adapter` and manage the internal mutable list directly, relying on synchronous `calculateDiff` when the gesture completes.

2. **Temporary Single Source of Truth:**
   During a drag gesture (e.g., `isDragging == true`), the **UI must become the absolute single source of truth**. You must block and ignore any reactive updates coming from the database/ViewModel to prevent the list from resetting its state mid-drag.

3. **Dumb Position Tracking (No Complex Math):**
   Do not try to intelligently swap indices in the data layer during movement. Let the UI handle the visual reordering freely. Once the drop is completed, take the final visual list, group it by `done` status (to respect the DB's `ORDER BY done ASC, position ASC`), and flatly overwrite the `position` properties using `mapIndexed { index, task -> task.copy(position = index) }`.

4. **Refactor over Patching:**
   If you find yourself adding multiple boolean flags (`isUpdating`, `forceCommit`, etc.) to prevent asynchronous loops, stop. Step back and untangle the reactive flow rather than adding architectural band-aids.
