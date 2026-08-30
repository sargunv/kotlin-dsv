# Examples

This page demonstrates common use cases for kotlin-dsv.

## Streaming Large Files

Use `encodeToSink` and `decodeFromSource` to work with streams instead of loading everything into
memory:

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

Use an exact [record delimiter](./api/kotlin-dsv/dev.sargunv.kotlindsv/-record-delimiter/index.html)
for warehouse dialects such as MySQL `LINES TERMINATED BY`, Snowflake `RECORD_DELIMITER`, or Spark
`lineSep`:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:custom-record-delimiter"
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

## Jagged Rows

Handle rows with fewer or more fields than the first kept row:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:jagged-rows-pad-truncate"
```

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:jagged-rows-skip"
```

## Enum Serialization

Serialize enums by name or ordinal:

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:enum-class"
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:enums"
```

## Put a structured value in one cell

`DsvFormat` keeps row classes flat. A nested class, list, or map fails when its generated serializer
opens a structure.

You can still store that value in one column. Give the property a `KSerializer` whose `serialize`
and `deserialize` write and read one primitive, usually a string. Annotate the property with
`@Serializable(with = ...)` or register the serializer as `@Contextual`. The format never inspects
the Kotlin type. It only sees the one primitive encode.

This example joins a list into one cell. A map, a class, or any other type uses the same adapter
shape.

```kotlin
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:pipe-list-serializer"
--8<-- "kotlin-dsv/src/commonTest/kotlin/dev/sargunv/kotlindsv/DocsTest.kt:custom-cell-serializer"
```

An empty cell on a nullable property decodes as null, even when your serializer would treat `""` as
an empty list or a default object.
