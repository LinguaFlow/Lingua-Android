# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Architecture

LinguaFlow is an Android application for Japanese kanji learning with PDF vocabulary extraction. The project follows clean architecture with three modules:

- **presentation**: UI layer with Activities, Fragments, ViewModels using MVVM pattern
- **domain**: Business logic layer with use cases, models, and repository interfaces 
- **data**: Data layer with API services, Room database, and repository implementations

### Key Technologies
- **Language**: Kotlin with coroutines for async operations
- **DI**: Hilt for dependency injection
- **Database**: Room with KSP for local storage
- **Networking**: Retrofit + OkHttp with Kotlin Serialization
- **UI**: Android View system with DataBinding/ViewBinding
- **Navigation**: Navigation Component with SafeArgs
- **Authentication**: Firebase Auth with Kakao/Naver social login

## Common Development Commands

### Build Commands
```bash
# Clean and build the entire project
./gradlew clean build

# Build specific modules
./gradlew :presentation:build
./gradlew :data:build
./gradlew :domain:build

# Build release APK
./gradlew assembleRelease

# Build debug APK
./gradlew assembleDebug
```

### Testing Commands
```bash
# Run all unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run tests for specific module
./gradlew :presentation:test
./gradlew :data:test
./gradlew :domain:test
```

### Lint and Code Quality
```bash
# Run lint checks
./gradlew lint

# Run lint on specific module
./gradlew :presentation:lint
```

### Installation
```bash
# Install debug build to connected device
./gradlew installDebug

# Install release build
./gradlew installRelease
```

## Module Dependencies

- **presentation** depends on **domain** and **data**
- **data** depends on **domain**
- **domain** has no dependencies (pure Kotlin/Android)

## Key Features

- **Kanji Learning**: PDF upload and vocabulary extraction
- **Social Authentication**: Kakao and Naver login integration
- **Offline Storage**: Room database for kanji vocabulary
- **Quiz System**: Multiple choice and keyboard input quizzes
- **TTS Integration**: Text-to-speech for pronunciation
- **Chapter Management**: Organized learning with progress tracking

## Configuration Notes

- API base URL is configured in `domain/build.gradle`: `https://linguaflow.store/`
- Social login keys are configured in `presentation/build.gradle` (requires actual keys)
- Firebase configuration via `google-services.json` in each module
- Room database with automatic migrations and converters for LocalDateTime