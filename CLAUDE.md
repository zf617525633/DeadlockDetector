# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ThreadTest is an Android application built with Kotlin and Jetpack Compose. It uses the modern Android development stack with Material 3 design.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

## Testing

```bash
# Run all unit tests
./gradlew test

# Run a specific unit test class
./gradlew testDebugUnitTest --tests "com.expo.threadtest.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Architecture

- **Package**: `com.expo.threadtest`
- **Min SDK**: 22
- **Target SDK**: 36
- **UI Framework**: Jetpack Compose with Material 3
- **Build System**: Gradle with version catalog (`gradle/libs.versions.toml`)

### Source Structure

- `app/src/main/java/` - Main application code
  - `MainActivity.kt` - Entry point activity using Compose
  - `ui/theme/` - Compose theme configuration (Color, Theme, Type)
- `app/src/test/` - Local unit tests (JUnit 4)
- `app/src/androidTest/` - Instrumented tests (AndroidJUnit4)

## Dependencies

Dependencies are managed via Gradle version catalog in `gradle/libs.versions.toml`. Key dependencies:
- AndroidX Core KTX
- Jetpack Compose (BOM-managed)
- Material 3
- Lifecycle Runtime KTX
