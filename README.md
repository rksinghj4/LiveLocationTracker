# LiveLocationTracker

An Android sample app that streams the device's GPS location in real time and reverse-geocodes each significant movement into a human-readable address using the OpenStreetMap [Nominatim](https://nominatim.org/) API.

Built as a reference implementation of modern Android architecture: Jetpack Compose, Hilt, coroutines/Flow, Retrofit + kotlinx.serialization, and clean-architecture layering.

---

## Features

- **Live location stream** — receives location updates every 5 seconds via Google Play Services `FusedLocationProviderClient`.
- **Reverse geocoding** — looks up the street address for the current coordinates via Nominatim.
- **Smart rate-limiting** — only re-geocodes after the user has moved ~55 m, staying well under Nominatim's 1 request/second policy.
- **Permission-aware UI** — Compose screens guide the user through location-permission and location-services prompts.
- **Pull-to-refresh** — manual one-shot location + geocode update.
- **Graceful failure** — network or geocoding errors surface in a Snackbar; coordinates are still shown even if the address lookup fails.

---

## Tech stack

| Concern             | Choice                                                  |
| ------------------- | ------------------------------------------------------- |
| Language            | Kotlin 2.0.21 (JVM target 17)                           |
| UI                  | Jetpack Compose (BOM 2024.10.01), Material 3            |
| Architecture        | MVVM + clean-architecture (data / domain / presentation) |
| DI                  | Hilt 2.52 (KSP)                                         |
| Async               | Kotlin Coroutines 1.9.0 + Flow                          |
| Networking          | Retrofit 2.11 + OkHttp 4.12                             |
| JSON                | kotlinx.serialization 1.7.3                             |
| Location            | Play Services Location 21.3                             |
| Permissions         | Accompanist Permissions 0.36                            |
| Logging             | Timber 5.0.1                                            |
| Min / target / compile SDK | 24 / 35 / 35                                      |

---

## Architecture

The project follows a three-layer clean-architecture split. Dependencies point inward: `presentation` → `domain` ← `data`.

```
app/src/main/java/com/example/livelocationtracker/
├── data/
│   ├── mapper/          # DTO ↔ domain converters (AddressMapper)
│   ├── remote/api/      # Retrofit service: NominatimService
│   ├── remote/dto/      # @Serializable response models
│   └── repository/      # LocationRepositoryImpl, GeocodingRepositoryImpl
├── di/                  # Hilt modules: NetworkModule, LocationModule, RepositoryModule
├── domain/
│   ├── model/           # UserLocation, Address, LocationError
│   ├── repository/      # Repository interfaces (LocationRepository, GeocodingRepository)
│   └── usecase/         # GetLiveLocationUseCase, GetAddressFromLocationUseCase
└── presentation/
    ├── location/        # LocationScreen, LocationViewModel, LocationUiState
    ├── components/      # Reusable composables (PermissionRationale, InfoRow)
    └── theme/           # Material 3 theme
```

### Data flow

```
FusedLocationProvider
        │  Flow<Location>
        ▼
LocationRepository ─────► LocationViewModel ─────► LocationScreen (Compose)
                              │
                              │ if moved > ~55 m
                              ▼
                 GetAddressFromLocationUseCase
                              │
                              ▼
                    GeocodingRepository
                              │
                              ▼
                 NominatimService (Retrofit)
                              │
                              ▼
                   Nominatim API (JSON)
```

---

## Getting started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 35
- A device or emulator running Android 7.0 (API 24) or above with Google Play services

### Build & run

```bash
git clone <this-repo>
cd LiveLocationTracker
./gradlew :app:installDebug
```

Or open the project in Android Studio and press **Run**.

### First launch

1. Grant **Fine location** permission when prompted.
2. Ensure **Location services** are enabled (the app prompts to open Settings if they're off).
3. The app starts streaming location every 5 s; the address card resolves once a fix is acquired.

### Testing reverse-geocoding on an emulator

```bash
# Set a fake GPS fix (longitude, latitude)
adb emu geo fix -122.084 37.422   # Googleplex
```

---

## Project layout — what to read first

| If you want to understand…       | Open this                                                          |
| -------------------------------- | ------------------------------------------------------------------ |
| How the screen is laid out       | `presentation/location/LocationScreen.kt`                          |
| State & business logic           | `presentation/location/LocationViewModel.kt`                       |
| How DI is wired                  | `di/NetworkModule.kt`, `di/LocationModule.kt`, `di/RepositoryModule.kt` |
| The Nominatim contract           | `data/remote/api/NominatimService.kt`, `data/remote/dto/NominatimResponseDto.kt` |
| Rate-limit / distance throttle   | `presentation/location/LocationViewModel.kt:109` (≈ 0.0005° ≈ 55 m) |

---

## Permissions

Declared in `AndroidManifest.xml`:

- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.ACCESS_COARSE_LOCATION`
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

The app also declares `android.hardware.location.gps` as an optional feature so it remains installable on devices without GPS.

---

## Nominatim usage

The app calls the public Nominatim instance at `https://nominatim.openstreetmap.org/`. Per the [Nominatim usage policy](https://operations.osmfoundation.org/policies/nominatim/):

- Each request sends a descriptive `User-Agent` (`LiveLocationTracker/1.0 (Android)`) — set in `NetworkModule.provideOkHttp`.
- Requests are throttled by movement (≥ ~55 m) to stay well under the 1 req/sec ceiling.
- `Accept-Language: en` is sent so responses are localized consistently.

If you fork this project for production use, please point at your own Nominatim deployment.

---

## Tests

- Unit: `app/src/test/.../AddressMapperTest.kt`
- Instrumented: `app/src/androidTest/.../ExampleInstrumentedTest.kt`

Run with:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest   # device/emulator required
```

---

## Release build

Release builds enable R8 minification and resource shrinking. ProGuard rules are in `app/proguard-rules.pro` and cover:

- Retrofit / OkHttp reflection
- kotlinx.serialization generated `$$serializer` and `Companion.serializer()` accessors
- Hilt generated components
- DTOs in `data.remote.dto.**`

```bash
./gradlew :app:assembleRelease
```

---

## License

This project is provided as a learning/reference sample. No license file is included; treat it as "all rights reserved" unless you add one.