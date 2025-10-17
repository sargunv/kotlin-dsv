# Kotlin DSV

[![Maven Central Version](https://img.shields.io/maven-central/v/dev.sargunv.kotlin-dsv/kotlin-dsv?label=Maven)](https://central.sonatype.com/artifact/dev.sargunv.kotlin-dsv/kotlin-dsv)
[![License](https://img.shields.io/github/license/sargunv/kotlin-dsv?label=License)](https://github.com/sargunv/kotlin-dsv/blob/main/LICENSE)
[![Kotlin Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fsargunv%2Fkotlin-dsv%2Frefs%2Fheads%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.kotlin&prefix=v&logo=kotlin&label=Kotlin)](./gradle/libs.versions.toml)
[![Documentation](https://img.shields.io/badge/Documentation-blue?logo=MaterialForMkDocs&logoColor=white)](https://sargunv.github.io/kotlin-dsv/)
[![API Reference](https://img.shields.io/badge/API_Reference-blue?logo=Kotlin&logoColor=white)](https://sargunv.github.io/kotlin-dsv/api/)

## Introduction

Kotlin DSV is a library for working with delimiter-separated values (CSV, TSV,
and custom formats) in Kotlin Multiplatform projects.

Features:

- Type-safe serialization using kotlinx.serialization
- Streaming support for large files via kotlinx.io
- Customizable delimiters, quoting, and naming strategies
- Multiplatform support for JVM, JS, Native, and WASM targets

## Getting Started

Add Kotlin DSV to your project:

```kotlin
dependencies {
    implementation("dev.sargunv.kotlin-dsv:kotlin-dsv:VERSION")
}
```

### Quick Start

```kotlin
@Serializable
data class Person(val name: String, val age: Int, val email: String?)

// Encode to CSV string
val people = listOf(
    Person("A", 30, "a@example.com"),
    Person("B", 25, null)
)
val csv = Csv.encodeToString(people)

// Decode from CSV string
val decoded = Csv.decodeFromString<Person>(csv)
```

See the [project site](https://sargunv.github.io/kotlin-dsv/) for more info and
examples.
