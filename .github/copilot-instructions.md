# GitHub Copilot Instructions for kotlin-dsv

This document provides guidance for GitHub Copilot when generating code for the kotlin-dsv repository.

## Project Overview

kotlin-dsv is a Kotlin Multiplatform library for working with delimiter-separated values (CSV, TSV, and custom formats). The library provides type-safe serialization using kotlinx.serialization and streaming support for large files via kotlinx.io.

**Package**: `dev.sargunv.kotlindsv`

**Supported Platforms**: JVM, JS, Native, and WASM targets

## Project Structure

- `kotlin-dsv/` - Main library module with core DSV parsing, writing, encoding/decoding
- `benchmark/` - Performance benchmarks
- `docs/` - Documentation source files
- `buildSrc/` - Custom Gradle build logic

## Development Commands

- **Build**: `./gradlew build` or `just build`
- **Lint**: `./gradlew detekt` or `just detekt`
- **Format**: `just format` (runs pre-commit hooks)
- **Test (all)**: `./gradlew allTests` or `just test`
- **Test (JVM)**: `./gradlew jvmTest` or `just test-jvm`
- **Test (single file)**: `./gradlew jvmTest --tests ClassName`
- **Coverage**: `./gradlew :koverHtmlReport` or `just coverage`
- **Docs**: `./gradlew :dokkaGenerateHtml` or `just build-dokka`

## Code Style and Conventions

### Indentation
- **Kotlin files**: 2 spaces
- **Other files**: 4 spaces

### Documentation
- All public APIs MUST be documented with KDoc comments
- Detekt enforces `UndocumentedPublicClass`, `UndocumentedPublicFunction`, and `UndocumentedPublicProperty` rules
- Include parameter descriptions and return value documentation
- Add `@throws` documentation for exceptions

### Naming Conventions
- Use standard Kotlin naming conventions (PascalCase for classes, camelCase for functions/properties)
- Column name transformations should use `DsvNamingStrategy`
- Follow existing patterns in the codebase

### Serialization
- Use kotlinx.serialization annotations (e.g., `@Serializable`)
- Mark data classes that represent DSV records as `@Serializable`

### Error Handling
- Throw `DsvParseException` for parsing errors
- Provide clear error messages with context
- Include line/column information when applicable

### Testing
- Write tests for all new functionality
- Place tests in appropriate test source sets (commonTest, jvmTest, etc.)
- Use descriptive test names that explain what is being tested
- Follow existing test patterns in the repository
- Consider edge cases: empty files, special characters, null values, malformed data

## Common Patterns

### DSV Reading Example
```kotlin
@Serializable
data class Person(val name: String, val age: Int, val email: String?)

// Decode from CSV string
val people = Csv.decodeFromString<List<Person>>(csvString)

// Decode from stream
val people = Csv.decodeFromSource<List<Person>>(source)
```

### DSV Writing Example
```kotlin
// Encode to CSV string
val csv = Csv.encodeToString(people)

// Encode to stream
Csv.encodeToSink(people, sink)
```

### Custom Format Configuration
```kotlin
val format = Dsv.Format(
  delimiter = '|',
  quote = '"',
  escape = '\\',
  namingStrategy = DsvNamingStrategy.SnakeCase
)
```

## API Stability

- This library follows semantic versioning
- Public API changes are tracked via ABI dump
- Run `./gradlew updateLegacyAbi` to update the API dump after public API changes
- Format code and dump ABI with pre-commit hooks before committing

## Dependencies

- **kotlinx.serialization**: For type-safe serialization
- **kotlinx.io**: For streaming I/O operations
- Keep dependencies minimal and avoid adding new ones unless necessary

## Before Committing

1. Format code: `just format`
2. Run linter: `./gradlew detekt` or `just detekt`
3. Run tests: `./gradlew allTests` or `just test`
4. Update ABI dump if public API changed: `./gradlew updateLegacyAbi`
5. Ensure all public APIs are documented

## Additional Resources

- [Project Documentation](https://sargunv.github.io/kotlin-dsv/)
- [API Reference](https://sargunv.github.io/kotlin-dsv/api/)
- [README.md](../README.md) - Getting started guide
- [AGENTS.md](../AGENTS.md) - Guide for AI coding agents
