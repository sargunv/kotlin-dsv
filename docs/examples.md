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

For very large datasets, use sequences to lazily encode and decode records one at
a time, minimizing memory usage:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:lazy-streaming"
```

This is ideal for:
- Processing large files that don't fit in memory
- Streaming data from databases or other sources
- Transforming data on-the-fly without intermediate storage

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
