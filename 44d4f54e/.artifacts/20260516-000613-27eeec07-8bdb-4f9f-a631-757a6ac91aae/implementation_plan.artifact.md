# Hilt Integration Verification and Refactoring

This plan aims to verify the recent Hilt dependency injection setup and further improve the architecture by making `ListManager` and `FileManager` injectable, and cleaning up deprecated factory patterns.

## Proposed Changes

### Build and Verification
- Run a full build to ensure the current Hilt setup is valid and the project compiles.

---

### Dependency Injection Refactoring

#### [ListManager.kt](file:///C:/Users/R5F/Documents/GitHub/ToDoList/app/src/main/java/com/electro/todolist/data/manager/ListManager.kt)
- Annotate the constructor with `@Inject` and use `@ApplicationContext`.
```kotlin
class ListManager @Inject constructor(@ApplicationContext private val context: Context) { ... }
```

#### [FileManager.kt](file:///C:/Users/R5F/Documents/GitHub/ToDoList/app/src/main/java/com/electro/todolist/data/manager/FileManager.kt)
- Annotate the constructor with `@Inject` and use `@ApplicationContext`.
```kotlin
class FileManager @Inject constructor(@ApplicationContext private val context: Context) { ... }
```

#### [TasksRepository.kt](file:///C:/Users/R5F/Documents/GitHub/ToDoList/app/src/main/java/com/electro/todolist/data/repository/TasksRepository.kt)
- Update the constructor to inject `ListManager` and `FileManager`.
```kotlin
open class TasksRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listManager: ListManager,
    private val fileManager: FileManager
) { ... }
```

#### [DataModule.kt](file:///C:/Users/R5F/Documents/GitHub/ToDoList/app/src/main/java/com/electro/todolist/di/DataModule.kt)
- Remove `provideTasksRepository` if constructor injection is sufficient, or update it to be more idiomatic. (Hilt can automatically provide `TasksRepository` if its constructor and all its dependencies are injectable).

---

### Cleanup

#### [DELETE] [TasksViewModelFactory.kt](file:///C:/Users/R5F/Documents/GitHub/ToDoList/app/src/main/java/com/electro/todolist/ui/home/TasksViewModelFactory.kt)
- Remove the deprecated factory as it's no longer used with Hilt.

---

## Verification Plan

### Automated Tests
- `.\gradlew.bat assembleDebug`: Ensure the project compiles successfully.
- `.\gradlew.bat test`: Run existing unit tests to ensure no regressions.

### Manual Verification
- Deploy the app to a device/emulator.
- Verify that tasks are loaded correctly (proves `TasksRepository` and `TaskDao` injection).
- Verify that lists can be created/renamed (proves `ListManager` injection).
- Verify that export/import works (proves `FileManager` injection).
