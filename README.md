# myTasks - ToDoList · Gestionnaire de tâches Android

> Application Android de gestion de tâches avec support multi-listes, glisser-déposer, gestes de balayage, import/export de fichiers et authentification Firebase.

📖 [English version below](#english-version)

---

## Fonctionnalités

- **Gestion des tâches** — Créer, modifier, compléter et supprimer des tâches. Réorganisation par glisser-déposer avec persistance de l'ordre. Suppression groupée des tâches complétées.
- **Multi-listes** — Travaillez avec plusieurs listes de tâches. Les listes supportent la création, le renommage, la suppression et la mémorisation de la dernière liste ouverte.
- **Import / Export** — Exportez vos tâches vers un fichier et importez depuis un fichier externe via le Storage Access Framework (SAF) d'Android.
- **Gestes et actions rapides** — Balayage pour supprimer avec annulation (Snackbar + undo), défilement fluide et réorganisation par glisser-déposer.
- **Authentification Firebase** — Connexion via Google, email ou mode anonyme grâce à FirebaseUI, avec hooks d'onboarding personnalisés.
- **Thèmes et paramètres** — `ThemeManager` et `SettingsActivity` pour les préférences utilisateur. DataStore pour les paramètres légers.

---

## Architecture

L'application suit le pattern **MVVM** avec une séparation claire des responsabilités :

```
ui/
├── home/         → TasksActivity, TasksViewModel, TasksAdapter, ListsAdapter
├── details/      → TaskDetailsActivity (édition d'une tâche)
├── login/        → LoginViewModel, LoginResult, LoggedInUserView
data/
├── repository/   → TasksRepository, ListRepository
├── manager/      → ListManager, FileManager
├── model/        → Task, SerialListObject
AuthActivity.kt   → Authentification Firebase
ThemeManager.kt   → Gestion des thèmes
SettingsActivity  → Préférences utilisateur
```

- **Activities & Fragments** : `TasksActivity` orchestre les fragments (`AddTaskFragment`, `ChangeListFragment`, bottom sheets). `TaskDetailsActivity` gère l'édition d'une tâche individuelle.
- **ViewModels** : `TasksViewModel` coordonne le chargement, le drag-and-drop, l'import/export. `LoginViewModel` gère l'état d'authentification Firebase.
- **Repositories & Managers** : `TasksRepository` encapsule le CRUD et la persistance des positions. `ListRepository` s'appuie sur `ListManager` pour les métadonnées. `FileManager` fournit les helpers d'import/export.
- **Modèles de données** : Les tâches sont sérialisées en JSON via Kotlinx Serialization et stockées en `SharedPreferences` par liste.
- **Composants UI** : Adapters `RecyclerView` (`TasksAdapter`, `ListsAdapter`). Jetpack Compose utilisé de manière sélective dans `TasksCompose.kt`.

---

## Persistance des données

| Données | Mécanisme |
|---|---|
| Tâches par liste | `SharedPreferences` (JSON / Kotlinx Serialization) |
| Métadonnées des listes | `SharedPreferences` partagé (`allLists`) |
| Dernière liste ouverte | `SharedPreferences` |
| Paramètres utilisateur | Jetpack `DataStore` |
| Import / Export | SAF — `ActivityResultContracts` |

---

## Prérequis et installation

- **Android Studio** Jellyfish ou version ultérieure
- **JDK 17**
- **Android SDK 35**
- Appareil ou émulateur Android **6.0 (API 23)** minimum

### Étapes

```bash
# 1. Cloner le dépôt
git clone https://github.com/votre-utilisateur/todolist.git
cd todolist

# 2. Placer votre google-services.json dans app/
# (un fichier est déjà présent — vérifiez que la config Firebase correspond à votre projet)

# 3. Compiler
./gradlew assembleDebug

# 4. Installer sur un appareil/émulateur
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Tests

```bash
# Tests unitaires
./gradlew test

# Tests instrumentés (appareil ou émulateur requis)
./gradlew connectedAndroidTest
```

---

## UX — Points notables

- Le **balayage pour supprimer** affiche une Snackbar ancrée au FAB avec une option d'annulation avant la suppression définitive.
- L'**état vide** est affiché automatiquement lorsqu'une liste ne contient aucune tâche.
- Le **glisser-déposer** est géré par `ItemTouchHelperCallback`, les nouvelles positions étant persistées via le ViewModel.

---

## Pistes d'amélioration (court terme)

- 🔐 **Sécuriser les artifacts de signature** — Les chemins et mots de passe du keystore sont en dur dans `build.gradle.kts` ; les déplacer vers des variables d'environnement ou des propriétés Gradle.
- ⚙️ **Aligner la toolchain Compose** — Le compileur Compose (1.8.1) ne correspond pas à la version du plugin Kotlin (2.1.20) et au plugin Compose (2.2.0-RC) ; migrer vers un BOM Compose stable et cohérent.
- 🏗️ **Centraliser la gestion des paramètres** — Les helpers DataStore vivent dans `TasksActivity` ; les refactoriser dans un `SettingsViewModel` dédié.
- 🗄️ **Remplacer SharedPreferences par Room** — Améliorerait la sécurité du schéma, la flexibilité des requêtes et les migrations.
- 🔑 **Moderniser le flux d'authentification** — `AuthActivity` utilise `startActivityForResult` (déprécié) ; migrer vers les `Activity Result APIs`.

---

---

## English version

> <a name="english-version"></a>

# ToDoList · Android Task Manager

> An Android task management app featuring multi-list support, drag-and-drop reordering, swipe gestures, file import/export, and Firebase-backed authentication.

---

## Features

- **Task management** — Create, edit, complete, and delete tasks. Drag-and-drop reordering with persistence. Bulk removal of completed items.
- **Multi-list support** — Work with multiple task lists. Supports creation, renaming, deletion, and remembers the last opened list.
- **File import/export** — Export tasks to a document URI and import from an external file using Android's Storage Access Framework (SAF).
- **Gestures & quick actions** — Swipe-to-delete with undo (Snackbar), smooth scrolling, and drag-to-reorder.
- **Firebase authentication** — Google, email, and optional anonymous sign-in via FirebaseUI, with custom layouts and onboarding hooks.
- **Theme & settings** — `ThemeManager` and `SettingsActivity` for user preferences. `DataStore` for lightweight settings.

---

## Architecture

The app follows the **MVVM** pattern with a clear separation of concerns:

```
ui/
├── home/         → TasksActivity, TasksViewModel, TasksAdapter, ListsAdapter
├── details/      → TaskDetailsActivity (per-task editing)
├── login/        → LoginViewModel, LoginResult, LoggedInUserView
data/
├── repository/   → TasksRepository, ListRepository
├── manager/      → ListManager, FileManager
├── model/        → Task, SerialListObject
AuthActivity.kt   → Firebase authentication
ThemeManager.kt   → Theme management
SettingsActivity  → User preferences
```

- **Activities & Fragments**: `TasksActivity` orchestrates fragments (`AddTaskFragment`, `ChangeListFragment`, bottom sheets). `TaskDetailsActivity` handles per-task editing.
- **ViewModels**: `TasksViewModel` coordinates task loading, drag/drop persistence, and import/export. `LoginViewModel` manages Firebase auth state.
- **Repositories & Managers**: `TasksRepository` wraps task CRUD and position persistence. `ListRepository` uses `ListManager` for list metadata. `FileManager` provides import/export helpers.
- **Data models**: Tasks are JSON-serialized via Kotlinx Serialization and stored in per-list `SharedPreferences`.
- **UI components**: `RecyclerView` adapters (`TasksAdapter`, `ListsAdapter`). Jetpack Compose used selectively in `TasksCompose.kt`.

---

## Data Persistence

| Data | Mechanism |
|---|---|
| Tasks per list | `SharedPreferences` (JSON / Kotlinx Serialization) |
| List metadata | Shared `SharedPreferences` (`allLists`) |
| Last opened list | `SharedPreferences` |
| User settings | Jetpack `DataStore` |
| Import / Export | SAF — `ActivityResultContracts` |

---

## Requirements & Setup

- **Android Studio** Jellyfish or later
- **JDK 17**
- **Android SDK 35**
- Device or emulator running **Android 6.0 (API 23)** or newer

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/your-username/todolist.git
cd todolist

# 2. Place your google-services.json inside app/
# (a file is already present — make sure the Firebase config matches your project)

# 3. Build
./gradlew assembleDebug

# 4. Install on a device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (device or emulator required)
./gradlew connectedAndroidTest
```

---

## UX Highlights

- **Swipe-to-delete** shows a Snackbar anchored to the FAB with an undo option before final deletion.
- **Empty state** is toggled automatically when a list has no tasks.
- **Drag-and-drop** ordering is handled by `ItemTouchHelperCallback`, with positions persisted through the ViewModel.

---

## Short-Term Improvement Ideas

- 🔐 **Secure signing artifacts** — Keystore paths and passwords are hardcoded in `build.gradle.kts`; move them to environment variables or Gradle properties.
- ⚙️ **Align Compose toolchain** — The Compose compiler (1.8.1) doesn't match the Kotlin plugin (2.1.20) and Compose plugin (2.2.0-RC); upgrade to a consistent, stable Compose BOM.
- 🏗️ **Centralize settings management** — DataStore helpers live inside `TasksActivity`; refactor into a dedicated `SettingsViewModel`.
- 🗄️ **Replace SharedPreferences with Room** — Would improve schema safety, query flexibility, and migration paths.
- 🔑 **Modernize auth flow** — `AuthActivity` uses the deprecated `startActivityForResult`; migrate to Activity Result APIs.
