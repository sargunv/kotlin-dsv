@file:Suppress("UnusedVariable", "unused")

package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable

class DocsTest {
  @Test
  fun quickStart() {
    // --8<-- [start:quick-start]
    @Serializable data class Person(val name: String, val age: Int, val email: String?)

    // Encode to CSV string
    val people = listOf(Person("Keiko", 30, "keiko@example.com"), Person("Rashid", 25, null))
    val csv = Csv.encodeToString(people)

    // Decode from CSV string
    val decoded = Csv.decodeFromString<List<Person>>(csv)
    // --8<-- [end:quick-start]
  }

  @Test
  fun streaming() {
    @Serializable data class Product(val id: Int, val name: String, val price: Double)

    val products =
      listOf(
        Product(1, "Widget", 9.99),
        Product(2, "Gadget", 19.99),
        Product(3, "Doohickey", 29.99),
      )

    // --8<-- [start:streaming]
    // Write to a buffer (can be any Sink like a file)
    val buffer = Buffer()
    Csv.encodeToSink(products, buffer)

    // Read from the buffer (can be any Source like a file)
    val decoded = Csv.decodeFromSource<List<Product>>(buffer)
    // --8<-- [end:streaming]
  }

  @Test
  fun lazyStreaming() {
    @Serializable data class Product(val id: Int, val name: String, val price: Double)

    // --8<-- [start:lazy-streaming]
    // For very large datasets, use sequences for lazy evaluation
    // Elements are processed one at a time, reducing memory usage

    // Generate a sequence of products (could be from a database cursor, etc.)
    val productSequence = generateSequence(1) { if (it < 1000000) it + 1 else null }
      .map { Product(it, "Product $it", it * 9.99) }

    // Write to a buffer lazily - elements are serialized as the sequence is iterated
    val buffer = Buffer()
    Csv.encodeSequenceToSink(productSequence, buffer)

    // Read from the buffer lazily - elements are deserialized as the sequence is consumed
    val decodedSequence = Csv.decodeSourceToSequence<Product>(buffer)

    // Process each element without loading everything into memory
    decodedSequence.take(10).forEach { product ->
      // Process product...
    }
    // --8<-- [end:lazy-streaming]
  }

  @Test
  fun tsv() {
    @Serializable data class Record(val id: Int, val name: String, val value: String)

    val records = listOf(Record(1, "First", "alpha"), Record(2, "Second", "beta"))

    // --8<-- [start:tsv]
    // TSV (tab-separated values) format
    val tsv = Tsv.encodeToString(records)
    val decoded = Tsv.decodeFromString<List<Record>>(tsv)
    // --8<-- [end:tsv]
  }

  @Test
  fun customDelimiter() {
    @Serializable data class Data(val a: String, val b: String, val c: String)

    val items = listOf(Data("one", "two", "three"), Data("four", "five", "six"))

    // --8<-- [start:custom-delimiter]
    // Custom delimiter (pipe-separated values)
    val format = DsvFormat(scheme = DsvScheme(delimiter = '|'))
    val psv = format.encodeToString(items)
    val decoded = format.decodeFromString<List<Data>>(psv)
    // --8<-- [end:custom-delimiter]
  }

  @Test
  fun namingStrategy() {
    // --8<-- [start:naming-strategy]
    @Serializable
    data class User(val firstName: String, val lastName: String, val emailAddress: String)

    val format = DsvFormat(Csv.scheme, namingStrategy = DsvNamingStrategy.SnakeCase)

    val users =
      listOf(User("Amara", "Okafor", "amara@example.com"), User("Chen", "Wei", "chen@example.com"))

    val csv = format.encodeToString(users)
    // CSV will have headers: first_name,last_name,email_address

    val decoded = format.decodeFromString<List<User>>(csv)
    // --8<-- [end:naming-strategy]
  }

  @Test
  fun missingColumnsAndUnknownKeys() {
    @Serializable data class PartialData(val id: Int, val name: String?, val description: String?)

    // --8<-- [start:missing-columns]
    val format =
      DsvFormat(scheme = Csv.scheme, treatMissingColumnsAsNull = true, ignoreUnknownKeys = true)

    // CSV has extra column 'extra' and missing column 'description'
    val csv =
      """
      id,name,extra
      1,Item A,ignored
      2,Item B,also ignored
      """
        .trimIndent()

    val decoded = format.decodeFromString<List<PartialData>>(csv)
    // Missing 'description' column is treated as null
    // Extra 'extra' column is ignored
    // --8<-- [end:missing-columns]
  }

  fun streamingFiles() {
    data class MyData(val id: Int, val name: String)
    val myData = emptyList<MyData>()

    // --8<-- [start:streaming-files]
    // Write to file
    SystemFileSystem.sink(Path("data.csv")).buffered().use { sink ->
      Csv.encodeToSink(myData, sink)
    }

    // Read from file
    val data =
      SystemFileSystem.source(Path("data.csv")).buffered().use { source ->
        Csv.decodeFromSource<List<MyData>>(source)
      }
    // --8<-- [end:streaming-files]
  }

  @Test
  fun customQuoteAndLineEndings() {
    // --8<-- [start:custom-quote]
    val format =
      DsvFormat(
        scheme =
          DsvScheme(
            delimiter = ';',
            quote = '\'',
            writeCrlf = false, // Use Unix-style line endings
          )
      )
    // --8<-- [end:custom-quote]
  }

  // --8<-- [start:enum-class]
  @Serializable
  enum class Status {
    ACTIVE,
    INACTIVE,
    PENDING,
  }

  // --8<-- [end:enum-class]

  @Test
  fun enumsByNameOrOrdinal() {
    // --8<-- [start:enums]
    @Serializable data class Item(val id: Int, val status: Status)

    val items = listOf(Item(1, Status.ACTIVE), Item(2, Status.PENDING))

    // By name (default)
    val csvByName = Csv.encodeToString(items)
    // Output: id,status\n1,ACTIVE\n2,PENDING

    // By ordinal
    val formatByOrdinal = DsvFormat(scheme = Csv.scheme, writeEnumsByName = false)
    val csvByOrdinal = formatByOrdinal.encodeToString(items)
    // Output: id,status\n1,0\n2,2
    // --8<-- [end:enums]
  }
}
