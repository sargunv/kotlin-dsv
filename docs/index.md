# Kotlin DSV

Kotlin Multiplatform library for working with delimiter-separated values (CSV,
TSV, and custom formats).

## Features

- Type-safe serialization using kotlinx.serialization
- Streaming support for large files via kotlinx-io
- Customizable delimiters, quoting, and naming strategies
- Multiplatform support for JVM, JS, Native, and WASM targets

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.sargunv.kotlin-dsv:kotlin-dsv:{{ gradle.project_version }}")
}
```

## Quick Start

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:quick-start"
```

## Next Steps

- Browse the [API Reference](api/index.html) for detailed documentation
- See the [examples](./examples.md) section for more advanced use cases
