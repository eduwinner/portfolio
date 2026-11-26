# FX Rates — Android (Kotlin + Jetpack Compose)

Native Android app that shows live currency exchange rates and historical charts, built with **Kotlin**, **Jetpack Compose (Material 3)**, **coroutines** and **kotlinx.serialization**, consuming the public [Frankfurter API](https://frankfurter.dev) (central bank reference rates, no API key).

It is the Android twin of [`fxrates-ios`](../fxrates-ios) — same features, built natively on each platform.

## Features

- Latest rates for 10 major currencies against USD, EUR or BRL
- Refresh, loading and error states with retry
- Detail screen with 7D / 30D / 90D / 1Y line chart, change %, low/high
- Dependency-free chart drawn with Compose `Canvas`
- Material You dynamic colors and dark mode
- TalkBack descriptions on rows and chart (accessibility-first)
- Unit tests for API parsing

## Architecture

```
app/src/main/java/com/winnertecnologia/fxrates/
├── MainActivity.kt            # Entry point, theme, simple navigation
├── data/
│   ├── Models.kt              # API rows, domain models, currency helpers
│   └── FrankfurterApi.kt      # RatesRepository interface + HTTP client
└── ui/
    ├── RatesViewModel.kt      # MVVM state (LoadState) with viewModelScope
    ├── RatesScreen.kt         # Rates list
    └── PairDetailScreen.kt    # History chart
```

- **MVVM** with a `RatesRepository` interface, so the network layer can be mocked or swapped.
- Minimal dependencies: AndroidX + Compose + kotlinx only.

## Run

Requirements: Android Studio (Ladybug or newer), JDK 17, Android SDK 35.

1. Open the `fxrates-android` folder in Android Studio and let Gradle sync.
2. Run on an emulator or device (min SDK 26 / Android 8.0).

Command line:

```bash
./gradlew assembleDebug      # build APK
./gradlew testDebugUnitTest  # run unit tests
```

---
Built by **Eduardo — Winner Tecnologia**. Available for native iOS/Android apps and API integrations.
