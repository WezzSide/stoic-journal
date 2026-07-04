## Task 7: Set up app module, theme, and navigation

**Files:**
- Create: `app/src/main/java/com/stoicjournal/StoicJournalApplication.kt`
- Create: `app/src/main/java/com/stoicjournal/MainActivity.kt`
- Create: `app/src/main/java/com/stoicjournal/navigation/Screen.kt`
- Create: `app/src/main/java/com/stoicjournal/navigation/StoicJournalNavHost.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Color.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Type.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Theme.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Shape.kt`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/build.gradle.kts`

**Interfaces:**
- Consumes: All feature modules and core modules.
- Produces: Application entry point, navigation graph, and global theme.

- [ ] **Step 1: Write `app/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.application")
}

android {
    namespace = "com.stoicjournal"

    defaultConfig {
        applicationId = "com.stoicjournal"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:notifications"))
    implementation(project(":feature:today"))
    implementation(project(":feature:journal"))
    implementation(project(":feature:library"))
    implementation(project(":feature:history"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
}
```

- [ ] **Step 2: Write `MainActivity.kt`**

```kotlin
package com.stoicjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import com.stoicjournal.theme.StoicJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoicJournalTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                startDestination?.let { destination ->
                    StoicJournalNavHost(startDestination = destination)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Write `MainViewModel.kt`**

```kotlin
package com.stoicjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.data.seed.AssetSeedImporter
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import com.stoicjournal.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val assetSeedImporter: AssetSeedImporter,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination: StateFlow<Screen?> = _startDestination

    init {
        viewModelScope.launch {
            assetSeedImporter.seedIfEmpty()
            val prefs = preferencesRepository.preferences.first()
            if (prefs.reminderEnabled) {
                reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
            }
            _startDestination.value = if (prefs.onboardingComplete) {
                Screen.Today
            } else {
                Screen.Onboarding
            }
        }
    }
}
```

- [ ] **Step 4: Write `Screen.kt`**

```kotlin
package com.stoicjournal.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Today : Screen("today")
    data object GuidedJournal : Screen("guided_journal/{promptId}") {
        fun createRoute(promptId: String) = "guided_journal/$promptId"
    }
    data object FreeFormJournal : Screen("free_form_journal")
    data object ExerciseJournal : Screen("exercise_journal/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise_journal/$exerciseId"
    }
    data object Library : Screen("library")
    data object QuoteDetail : Screen("quote_detail/{quoteId}") {
        fun createRoute(quoteId: String) = "quote_detail/$quoteId"
    }
    data object History : Screen("history")
    data object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    data object Settings : Screen("settings")
}
```

- [ ] **Step 5: Write `StoicJournalNavHost.kt`**

```kotlin
package com.stoicjournal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stoicjournal.feature.history.HistoryScreen
import com.stoicjournal.feature.history.EntryDetailScreen
import com.stoicjournal.feature.journal.exercise.ExerciseJournalScreen
import com.stoicjournal.feature.journal.freeform.FreeFormJournalScreen
import com.stoicjournal.feature.journal.guided.GuidedJournalScreen
import com.stoicjournal.feature.library.LibraryScreen
import com.stoicjournal.feature.library.QuoteDetailScreen
import com.stoicjournal.feature.onboarding.OnboardingScreen
import com.stoicjournal.feature.settings.SettingsScreen
import com.stoicjournal.feature.today.TodayScreen

@Composable
fun StoicJournalNavHost(
    startDestination: Screen,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Today.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Today.route) {
            TodayScreen(
                onGuidedJournal = { promptId ->
                    navController.navigate(Screen.GuidedJournal.createRoute(promptId))
                },
                onFreeFormJournal = { navController.navigate(Screen.FreeFormJournal.route) },
                onExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseJournal.createRoute(exerciseId))
                },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.GuidedJournal.route) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getString("promptId") ?: return@composable
            GuidedJournalScreen(
                promptId = promptId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FreeFormJournal.route) {
            FreeFormJournalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ExerciseJournal.route) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: return@composable
            ExerciseJournalScreen(
                exerciseId = exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onQuoteClick = { quoteId ->
                    navController.navigate(Screen.QuoteDetail.createRoute(quoteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QuoteDetail.route) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("quoteId") ?: return@composable
            QuoteDetailScreen(
                quoteId = quoteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onEntryClick = { entryId ->
                    navController.navigate(Screen.EntryDetail.createRoute(entryId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EntryDetail.route) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
            EntryDetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 6: Write theme files**

`Color.kt`:

```kotlin
package com.stoicjournal.theme

import androidx.compose.ui.graphics.Color

val Stone50 = Color(0xFFFAFAF9)
val Stone100 = Color(0xFFF5F5F4)
val Stone200 = Color(0xFFE7E5E4)
val Stone400 = Color(0xFFA8A29E)
val Stone600 = Color(0xFF57534E)
val Stone800 = Color(0xFF292524)
val Stone900 = Color(0xFF1C1917)
val Slate700 = Color(0xFF334155)
val Amber200 = Color(0xFFFDE68A)
val Amber500 = Color(0xFFF59E0B)
val Amber700 = Color(0xFFB45309)
```

`Type.kt`:

```kotlin
package com.stoicjournal.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val StoicTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

`Theme.kt`:

```kotlin
package com.stoicjournal.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Amber700,
    onPrimary = Color.White,
    secondary = Slate700,
    onSecondary = Color.White,
    background = Stone50,
    onBackground = Stone900,
    surface = Stone100,
    onSurface = Stone900,
    surfaceVariant = Stone200,
    onSurfaceVariant = Stone600,
    outline = Stone400
)

private val DarkColors = darkColorScheme(
    primary = Amber200,
    onPrimary = Stone900,
    secondary = Stone200,
    onSecondary = Stone900,
    background = Stone900,
    onBackground = Stone100,
    surface = Stone800,
    onSurface = Stone100,
    surfaceVariant = Stone600,
    onSurfaceVariant = Stone200,
    outline = Stone400
)

@Composable
fun StoicJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StoicTypography,
        content = content
    )
}
```

Note: Need to import `androidx.compose.ui.graphics.Color` in `Theme.kt`.

`Shape.kt`:

```kotlin
package com.stoicjournal.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val StoicShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)
```

Update `Theme.kt` to include shapes:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = StoicTypography,
    shapes = StoicShapes,
    content = content
)
```

- [ ] **Step 7: Write `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".StoicJournalApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.StoicJournal">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.StoicJournal">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 8: Add app string resources and launcher icons**

Create `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Stoic Journal</string>
</resources>
```

Add placeholder launcher icons in `app/src/main/res/mipmap-xxxhdpi/` or generate via Android Studio asset studio.

- [ ] **Step 9: Create empty placeholder feature screens**

For the app module to compile before feature modules are fully implemented, create minimal placeholder Composables in each feature module. These will be replaced in subsequent tasks.

- [ ] **Step 10: Build app module**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add app/
git commit -m "feat(app): add MainActivity, navigation, theme, and application setup"
```

---

