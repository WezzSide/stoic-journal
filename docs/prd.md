# Product Requirements Document: Stoic Journal

**Status:** Draft  
**Last updated:** 2026-07-04  
**Platform:** Android (API 26+)  
**Target release:** MVP v1.0

---

## 1. Overview

Stoic Journal is an offline-first Android journaling app that helps users build a daily reflective practice through Stoic prompts, wisdom, and guided exercises. It combines two core experiences:

- A **daily journal** rooted in Stoic themes (control, adversity, mortality, virtue).
- A **wisdom library** of curated quotes and short exercises from Marcus Aurelius, Epictetus, Seneca, and other Stoic philosophers.

The app is designed for both beginners and practicing Stoics. Beginners receive guided explanations and simple prompts; experienced users get direct access to primary-source quotes and structured reflection exercises.

---

## 2. Goals & Success Criteria

### Primary Goals
1. Help users establish a consistent daily reflection habit.
2. Make Stoic philosophy accessible without diluting its substance.
3. Keep the experience private, calm, and distraction-free.

### MVP Success Criteria
- A user can complete a guided daily journal entry in under 2 minutes.
- A user can discover and save quotes from Marcus Aurelius, Epictetus, Seneca, and others.
- A user can set a daily reminder and see a consistency indicator.
- All core functionality works without an internet connection.

---

## 3. Target Audience

| Segment | Needs | How the app serves them |
|---|---|---|
| **Stoicism beginners** | Gentle introduction, context for quotes, simple prompts | Adaptive onboarding, beginner prompts, explanatory exercise steps |
| **Practicing Stoics** | Reliable reflection tool, primary sources, structured exercises | Daily prompts, quote library, virtue-themed exercises, history |

---

## 4. MVP Features

### 4.1 Onboarding
- Welcome screen with the app’s purpose.
- Self-selection of Stoicism familiarity: *Beginner / Familiar / Advanced*.
- Daily reminder time picker.
- Seed bundled content (quotes, prompts, exercises) on first launch.

### 4.2 Today / Home Screen
- Fresh daily Stoic prompt selected from the prompt pool.
- Relevant quote shown above the prompt.
- Quick actions: **Guided reflection**, **Free-form entry**, **Stoic exercise**.
- Streak / consistency indicator.
- Date header and last-entry summary.

### 4.3 Journal Entry
**Guided entry:**
- Pre-filled prompt and quote.
- Multi-line text input for reflection.
- Auto-save as the user types.
- Save / discard actions.

**Free-form entry:**
- Blank title (optional) and body.
- Ability to attach a random or chosen quote for inspiration.

**Exercise entry:**
- Exercise title and step-by-step guidance.
- Reflection prompt at the end.

### 4.4 Wisdom Library
- Browse quotes by **philosopher**.
- Browse quotes by **theme**: Control, Adversity, Mortality, Temperance, Justice, Courage, Wisdom.
- Favorite quotes (saved locally).
- Quote detail with full attribution and source.

### 4.5 Stoic Exercises
Five core exercises at launch:
1. **Dichotomy of Control** — separate what is in your control from what is not.
2. **Evening Review** — reflect on the day’s actions and judgments.
3. **Premeditatio Malorum** — anticipate challenges with calm.
4. **View from Above** — gain perspective by imagining your place in the wider world.
5. **Amor Fati** — reflect on accepting what happens.

Each exercise includes a short explanation and guided steps.

### 4.6 History
- Chronological list of journal entries.
- Filter by entry type: All / Guided / Free-form / Exercise.
- Entry detail view.
- Swipe-to-delete with undo.

### 4.7 Reminders
- Daily local notification at the user-selected time.
- Settings to change or disable the reminder.
- Implemented with WorkManager for reliability and battery efficiency.

### 4.8 Settings
- Reminder time toggle.
- Reset onboarding / familiarity.
- Export/backup placeholder (UI only for MVP; implementation deferred).
- About / attribution.

---

## 5. User Stories

| ID | Story |
|---|---|
| US-1 | As a first-time user, I select my familiarity with Stoicism so the app can tailor prompts. |
| US-2 | As a user, I can set my daily reminder time during onboarding. |
| US-3 | As a user, I see a fresh daily Stoic prompt on the home screen. |
| US-4 | As a user, I can write a guided reflection tied to that prompt. |
| US-5 | As a user, I can start a free-form journal entry at any time. |
| US-6 | As a user, I see a relevant Stoic quote above each prompt. |
| US-7 | As a user, I can browse quotes by philosopher. |
| US-8 | As a user, I can browse quotes by theme. |
| US-9 | As a user, I can save a quote to my favorites. |
| US-10 | As a beginner, I can open a short Stoic exercise with guided steps. |
| US-11 | As a user, I can mark an exercise complete and add a short reflection. |
| US-12 | As a user, I can view a chronological list of my past entries. |
| US-13 | As a user, I can see a simple streak/consistency indicator. |
| US-14 | As a user, I can delete an entry. |
| US-15 | As a user, I receive a daily notification at my chosen time. |
| US-16 | As a user, I can change or disable the reminder in settings. |

---

## 6. Information Architecture

### Navigation
Bottom navigation with four tabs:

1. **Today** — daily prompt, streak, quick actions.
2. **Library** — quotes by philosopher or theme; favorites.
3. **History** — list of past entries.
4. **Settings** — reminder, onboarding reset, about.

### Screen Inventory
- Onboarding (3 steps)
- Today / Home
- Guided journal entry
- Free-form journal entry
- Exercise detail
- Quote detail
- Entry detail
- Settings

---

## 7. Data Model

### Entities

```kotlin
// JournalEntry
data class JournalEntry(
    val id: String,
    val type: EntryType,          // GUIDED, FREE_FORM, EXERCISE
    val promptId: String?,        // nullable
    val title: String?,           // nullable for free-form
    val body: String,
    val quoteId: String?,         // nullable
    val exerciseId: String?,      // nullable
    val createdAt: Instant,
    val updatedAt: Instant
)

// Prompt
data class Prompt(
    val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String?,
    val difficultyLevel: DifficultyLevel  // BEGINNER, INTERMEDIATE, ADVANCED
)

// Quote
data class Quote(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String?,          // e.g., "Meditations 4.19"
    val isFavorite: Boolean
)

// Philosopher
data class Philosopher(
    val id: String,
    val name: String,
    val lived: String,            // e.g., "121–180 AD"
    val shortBio: String
)

// Exercise
data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: Theme,
    val difficultyLevel: DifficultyLevel
)

// UserPreferences
data class UserPreferences(
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val stoicismLevel: StoicismLevel,  // BEGINNER, FAMILIAR, ADVANCED
    val onboardingComplete: Boolean
)

// Streak
data class Streak(
    val currentCount: Int,
    val lastEntryDate: LocalDate?
)
```

### Storage
- **Room/SQLite** for all user-generated data and favorite state.
- **Bundled JSON assets** for quotes, prompts, exercises, and philosopher bios, seeded on first launch.
- **DataStore** for user preferences and onboarding state.

---

## 8. Technical Architecture

### Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Toolkit | Jetpack Compose |
| Design System | Custom theme built on Material 3 tokens |
| Architecture | MVVM + Repository pattern |
| Dependency Injection | Hilt |
| Local Database | Room |
| Async | Kotlin Coroutines + Flow |
| Background Work | WorkManager |
| Preferences | DataStore |

### Module Structure
```
app/                  # Application shell, navigation, theme
feature:today/        # Home screen and daily prompt
feature:journal/      # Guided, free-form, and exercise entry flows
feature:library/      # Quote and philosopher browsing
feature:history/      # Entry list and detail
feature:settings/     # Reminders and preferences
core:data/            # Room entities, DAOs, repositories, seeders
core:domain/          # Use cases and models
core:common/          # Utilities, date helpers, constants
```

### API Levels
- **minSdk:** 26 (Android 8.0)
- **targetSdk:** Latest stable API at time of build
- **compileSdk:** Latest stable API at time of build

---

## 9. UI/UX Design Direction

### Visual Principles
- Calm, minimal, and focused; no flashy animations.
- Earthy, muted palette: stone, slate, warm amber accents.
- Serif headings for a timeless, philosophical feel; clean sans-serif body text.
- Generous whitespace and readable line length for journal text.
- Dark mode support from day one.

### Interaction Patterns
- Bottom sheet for selecting prompt type or exercise.
- Swipe-to-delete entries in history with undo snackbar.
- Simple sorting/filter chips in history and library.
- Floating action button on Today for quick free-form entry.

---

## 10. Content Strategy

### Philosophers Included at Launch
- Marcus Aurelius
- Epictetus
- Seneca the Younger
- Musonius Rufus
- Cleanthes
- Cato the Younger (selected sayings)

### Content Volume
- ~100 curated quotes with attribution and source.
- ~30 daily prompts rotating through themes and difficulty levels.
- 5 core Stoic exercises with guided steps.
- Short philosopher bios.

### Tone
- Direct, timeless, and non-preachy.
- Quotes remain faithful to public-domain translations.
- Prompts translate Stoic ideas into modern reflection without diluting them.

---

## 11. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Offline-first** | All reading and writing works without network. |
| **Privacy** | Journal entries never leave the device in the MVP. |
| **Performance** | Cold start under 1.5s; entry list scrolls at 60fps. |
| **Accessibility** | Minimum 48dp touch targets, dynamic type support, content descriptions on icons. |
| **Battery** | Reminder implemented with WorkManager; no persistent background work. |
| **Reliability** | Auto-save journal drafts to prevent data loss. |

---

## 12. Open Questions

1. Should the daily prompt rotate at a fixed time (e.g., midnight local time) or on first app open each day?
2. Should streaks count any journal entry, or only guided daily entries?
3. Should free-form entries support rich text (bold/italics) or remain plain text for MVP?
4. Should the app include a one-time import of bundled content on every reinstall, or preserve user favorites across reinstalls?

---

## 13. Out of Scope for MVP

- Cloud sync / user accounts
- Social sharing or community features
- Audio meditations
- Custom themes beyond light/dark
- Home screen widget
- Import from other journaling apps
- Full-text search across entries
- Encryption of local database

---

## 14. Release Criteria

The MVP is ready for release when:
- [ ] All user stories in Section 5 are implemented and manually verified.
- [ ] Daily reminder fires reliably across device reboots.
- [ ] App passes smoke tests on Android 8, 12, and latest API.
- [ ] No crashes in core flows: onboarding, journal entry, library browsing, history.
- [ ] Accessibility scanner passes on primary screens.
