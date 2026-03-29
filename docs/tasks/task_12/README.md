# Task 12: Migrate from Gson to kotlinx-serialization

## Status: Complete

## Summary
Replaced all Gson usage with kotlinx-serialization across the entire codebase, eliminating a redundant dependency and improving Kotlin null-safety guarantees.

## Motivation
- Gson ignores Kotlin nullability, bypasses constructors via reflection, and can silently corrupt non-null fields
- kotlinx-serialization was already a project dependency (used for Compose Navigation routes) with the compiler plugin applied
- Consolidating to one serialization library reduces dependency count and ensures consistent behavior

## Changes

### Build Configuration
- **`gradle/libs.versions.toml`**: Removed `gson` version, `gson` library, and `retrofit-converter-gson` library
- **`app/build.gradle`**: Replaced `api libs.retrofit.converter.gson` with `implementation libs.retrofit.converter.kotlinx.serialization`; removed `force libs.gson` from `resolutionStrategy`
- **`app/proguard-rules.pro`**: Removed dead `-dontwarn android.support.v4.**` rule

### DI
- **`di/AppModule.kt`**: Removed `GsonBuilder` singleton; `Json` singleton (already present) serves all serialization needs

### Networking
- **`models/messages/network/RetrofitBuilder.kt`**: Replaced `GsonConverterFactory` with `json.asConverterFactory("application/json".toMediaType())`

### Model Annotations (19 files)
Replaced `@SerializedName("x")` with `@SerialName("x")` and added `@Serializable` to all API request/response models:
- 11 request models in `api/models/requests/`
- 7 response models in `models/messages/network/responses/`
- 2 internal models (`LocationData`, `Destination`)

### Enum Serialization
- **`models/messages/network/responses/MessageType.kt`**: Added `@Serializable` with `@SerialName` per variant, replacing the custom `MessageTypeDeserializer`
- **Deleted**: `models/messages/network/MessageTypeDeserializer.kt`

### Runtime Serialization (8 files)
Replaced `gson.toJson()`/`gson.fromJson()` with `json.encodeToString()`/`json.decodeFromString()`:
- `TreeUploader.kt`, `SessionUploader.kt`, `PlanterUploader.kt`, `DeviceConfigUploader.kt`
- `MessageUploader.kt`, `UploadLocationDataUseCase.kt`, `LocationDataCapturer.kt`
- `OrgRepo.kt` (also replaced `JsonParser().parse()` with `Json.parseToJsonElement()` and eliminated `TypeToken`)

### Room Converters
- **`database/Converters.kt`**: Replaced `Gson()` with `Json { ignoreUnknownKeys = true }`; null maps now correctly store as SQL NULL instead of the string `"null"`

### GeoJSON Generation
- **`map/LibreMap.kt`**: Replaced `com.google.gson.JsonObject`/`JsonArray` with kotlinx-serialization `buildJsonObject`/`buildJsonArray` DSL

### Tests (8 files)
Updated all test files to use `Json` instead of `Gson`/`GsonBuilder`:
- `TreeUploaderTest.kt`, `SessionUploaderTest.kt`, `PlanterUploaderTest.kt`
- `DeviceConfigUploaderTest.kt`, `MessageUploaderTest.kt`, `LocationDataCapturerTest.kt`
- `OrgRepoTest.kt`, `ConvertersTest.kt`

## Verification
- `./gradlew clean assembleDebug` -- BUILD SUCCESSFUL
- `./gradlew testDebugUnitTest` -- 208 tests, 0 failures
- Zero `com.google.gson` imports remain in the codebase
- Zero Gson references in `libs.versions.toml`
