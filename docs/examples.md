# Examples

This page demonstrates common use cases for kotlin-dsv.

## Streaming Large Files

Use `encodeToSink` and `decodeFromSource` to work with streams instead of
loading everything into memory:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:streaming"
```

This is particularly useful when working with large files:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:streaming-files"
```

## TSV Format

Work with tab-separated values using the pre-configured `Tsv` format:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:tsv"
```

## Custom Delimiters

Create a custom format with any delimiter:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:custom-delimiter"
```

You can also customize the quote character and line endings:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:custom-quote"
```

## Naming Strategies

Transform property names to match different naming conventions:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:naming-strategy"
```

Available strategies:

- [`DsvNamingStrategy.Identity`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-identity/index.html) -
  No transformation (default)
- [`DsvNamingStrategy.SnakeCase`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-snake-case/index.html) -
  Converts `camelCase` to `snake_case`
- [`DsvNamingStrategy.KebabCase`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-kebab-case/index.html) -
  Converts `camelCase` to `kebab-case`
- [`DsvNamingStrategy.PascalCase`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-pascal-case/index.html) -
  Converts `camelCase` to `PascalCase`
- [`DsvNamingStrategy.TitleCaseWords`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-title-case-words/index.html) -
  Converts `camelCase` to `Title Case Words`
- [`DsvNamingStrategy.SentenceCaseWords`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-sentence-case-words/index.html) -
  Converts `camelCase` to `Sentence case words`
- [`DsvNamingStrategy.LowercaseWords`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-lowercase-words/index.html) -
  Converts `camelCase` to `lowercase words`
- [`DsvNamingStrategy.UppercaseWords`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-uppercase-words/index.html) -
  Converts `camelCase` to `UPPERCASE WORDS`
- [`DsvNamingStrategy.Composite`](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/-composite/index.html) -
  Chain multiple strategies together

You can also implement custom strategies by extending
[DsvNamingStrategy](./api/kotlin-dsv/dev.sargunv.kotlindsv/-dsv-naming-strategy/index.html).

## Handling Missing or Extra Columns

Handle CSVs with incomplete or extra columns:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:missing-columns"
```

## Enum Serialization

Serialize enums by name or ordinal:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:enum-class"
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:enums"
```
