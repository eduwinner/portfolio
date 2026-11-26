# FX Rates — iOS (SwiftUI)

Native iOS app that shows live currency exchange rates and historical charts, built with **SwiftUI**, **Swift Charts** and **async/await**, consuming the public [Frankfurter API](https://frankfurter.dev) (central bank reference rates, no API key).

It is the iOS twin of [`fxrates-android`](../fxrates-android) — same features, built natively on each platform.

## Features

- Latest rates for 10 major currencies against USD, EUR or BRL
- Pull-to-refresh, loading and error states with retry
- Detail screen with 7D / 30D / 90D / 1Y line chart, change %, low/high
- VoiceOver labels on rows and chart (accessibility-first)
- Unit tests for API decoding

## Architecture

```
FXRates/
├── FXRatesApp.swift         # App entry point
├── Models.swift             # API rows, domain models, currency helpers
├── FrankfurterClient.swift  # RatesService protocol + URLSession client
├── RatesViewModel.swift     # MVVM view models (ObservableObject, @MainActor)
├── ContentView.swift        # Rates list
└── PairDetailView.swift     # History chart (Swift Charts)
FXRatesTests/                # XCTest
```

- **MVVM** with a `RatesService` protocol, so the network layer can be mocked in tests or swapped for another provider.
- No third-party dependencies.

## Run

Requirements: macOS with Xcode 15+, iOS 16+ target.

```bash
brew install xcodegen      # one time
cd fxrates-ios
xcodegen generate          # creates FXRates.xcodeproj from project.yml
open FXRates.xcodeproj     # then Run (⌘R) on a simulator
```

Run tests: `⌘U` in Xcode, or

```bash
xcodebuild test -project FXRates.xcodeproj -scheme FXRates \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```

---
Built by **Eduardo — Winner Tecnologia**. Available for native iOS/Android apps and API integrations.
