## Task 2: Create core domain models

**Files:**
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/EntryType.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/StoicismLevel.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Theme.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/JournalEntry.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Quote.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Prompt.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Exercise.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Philosopher.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Streak.kt`
- Create: `core/domain/build.gradle.kts`

**Interfaces:**
- Produces: Domain model classes used by repositories, use cases, ViewModels, and UI.

- [ ] **Step 1: Write `core/domain/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.library")
}

android {
    namespace = "com.stoicjournal.core.domain"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

- [ ] **Step 2: Write `EntryType.kt`**

```kotlin
package com.stoicjournal.core.domain.model

enum class EntryType {
    GUIDED,
    FREE_FORM,
    EXERCISE
}
```

- [ ] **Step 3: Write `StoicismLevel.kt`**

```kotlin
package com.stoicjournal.core.domain.model

enum class StoicismLevel {
    BEGINNER,
    FAMILIAR,
    ADVANCED
}
```

- [ ] **Step 4: Write `Theme.kt`**

```kotlin
package com.stoicjournal.core.domain.model

enum class Theme {
    CONTROL,
    ADVERSITY,
    MORTALITY,
    TEMPERANCE,
    JUSTICE,
    COURAGE,
    WISDOM
}
```

- [ ] **Step 5: Write `JournalEntry.kt`**

```kotlin
package com.stoicjournal.core.domain.model

import java.time.Instant

data class JournalEntry(
    val id: String,
    val type: EntryType,
    val promptId: String? = null,
    val title: String? = null,
    val body: String,
    val quoteId: String? = null,
    val exerciseId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

- [ ] **Step 6: Write `Quote.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Quote(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String? = null,
    val isFavorite: Boolean = false
)
```

- [ ] **Step 7: Write `Prompt.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Prompt(
    val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String? = null,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 8: Write `Exercise.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: Theme,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 9: Write `Philosopher.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Philosopher(
    val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
)
```

- [ ] **Step 10: Write `Streak.kt`**

```kotlin
package com.stoicjournal.core.domain.model

import java.time.LocalDate

data class Streak(
    val currentCount: Int,
    val lastEntryDate: LocalDate?
)
```

- [ ] **Step 11: Run a build to verify domain module compiles**

Run: `./gradlew :core:domain:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 12: Commit**

```bash
git add core/domain/
git commit -m "feat(domain): add core domain models"
```

---

