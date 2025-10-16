package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable

class SequenceEncoderDecoderTest {

  @Serializable
  enum class Status {
    ACTIVE,
    INACTIVE,
    PENDING,
  }

  @Serializable
  data class Sample(
    val id: Int,
    val name: String,
    val price: Double,
    val count: Long?,
    val active: Boolean,
    val status: Status,
    val description: String?,
  )

  private val format = DsvFormat(DsvScheme(delimiter = ',', writeCrlf = false))

  @Test
  fun decodeSequenceLazily() {
    val csv =
      """
      id,name,price,count,active,status,description
      1,Item A,19.99,100,true,ACTIVE,First item
      2,Item B,29.99,,false,INACTIVE,
      3,Item C,39.99,50,true,PENDING,Third item
      """
        .trimIndent()

    val source = Buffer().apply { writeString(csv) }
    val sequence = format.decodeSourceToSequence<Sample>(source)

    // Verify we get a sequence (lazy evaluation)
    val samples = sequence.toList()

    val expected =
      listOf(
        Sample(1, "Item A", 19.99, 100, true, Status.ACTIVE, "First item"),
        Sample(2, "Item B", 29.99, null, false, Status.INACTIVE, null),
        Sample(3, "Item C", 39.99, 50, true, Status.PENDING, "Third item"),
      )

    assertEquals(expected, samples)
  }

  @Test
  fun decodeEmptySequence() {
    val csv =
      """
      id,name,price,count,active,status,description
      """
        .trimIndent()

    val source = Buffer().apply { writeString(csv) }
    val sequence = format.decodeSourceToSequence<Sample>(source)
    val samples = sequence.toList()

    assertTrue(samples.isEmpty())
  }

  @Test
  fun encodeSequenceLazily() {
    val samples =
      sequenceOf(
        Sample(1, "Item A", 19.99, 100, true, Status.ACTIVE, "First item"),
        Sample(2, "Item B", 29.99, null, false, Status.INACTIVE, null),
        Sample(3, "Item C", 39.99, 50, true, Status.PENDING, "Third item"),
      )

    val sink = Buffer()
    format.encodeSequenceToSink(samples, sink)
    val result = sink.readString()

    val expected =
      """
      id,name,price,count,active,status,description
      1,Item A,19.99,100,true,ACTIVE,First item
      2,Item B,29.99,,false,INACTIVE,
      3,Item C,39.99,50,true,PENDING,Third item
      
      """
        .trimIndent()

    assertEquals(expected, result)
  }

  @Test
  fun encodeEmptySequence() {
    val samples = emptySequence<Sample>()

    val sink = Buffer()
    format.encodeSequenceToSink(samples, sink)
    val result = sink.readString()

    // Empty sequence should produce empty output
    assertEquals("", result)
  }

  @Test
  fun roundTripSequence() {
    val original =
      listOf(
        Sample(1, "Item A", 19.99, 100, true, Status.ACTIVE, "First item"),
        Sample(2, "Item B", 29.99, null, false, Status.INACTIVE, null),
        Sample(3, "Item C", 39.99, 50, true, Status.PENDING, "Third item"),
      )

    // Encode as sequence
    val sink = Buffer()
    format.encodeSequenceToSink(original.asSequence(), sink)

    // Decode as sequence
    val decoded = format.decodeSourceToSequence<Sample>(sink).toList()

    assertEquals(original, decoded)
  }

  @Test
  fun sequenceCanOnlyBeIteratedOnce() {
    val csv =
      """
      id,name,price,count,active,status,description
      1,Item A,19.99,100,true,ACTIVE,First item
      2,Item B,29.99,,false,INACTIVE,
      """
        .trimIndent()

    val source = Buffer().apply { writeString(csv) }
    val sequence = format.decodeSourceToSequence<Sample>(source)

    // First iteration should work
    val firstIteration = sequence.toList()
    assertEquals(2, firstIteration.size)

    // Second iteration should return empty (sequence exhausted)
    val secondIteration = sequence.toList()
    assertEquals(0, secondIteration.size)
  }

  @Test
  fun sequenceWithSpecialCharacters() {
    val samples =
      sequenceOf(
        Sample(1, "Item, with comma", 19.99, null, true, Status.ACTIVE, "Quote: \"test\""),
        Sample(2, "Item\nwith newline", 29.99, null, false, Status.INACTIVE, "Normal"),
      )

    val sink = Buffer()
    format.encodeSequenceToSink(samples, sink)
    val result = sink.readString()

    val expected =
      """
      id,name,price,count,active,status,description
      1,"Item, with comma",19.99,,true,ACTIVE,"Quote: ""test""${""}"
      2,"Item
      with newline",29.99,,false,INACTIVE,Normal
      
      """
        .trimIndent()

    assertEquals(expected, result)

    // Decode it back
    val decoded = format.decodeSourceToSequence<Sample>(Buffer().apply { writeString(result) }).toList()
    assertEquals(samples.toList(), decoded)
  }

  @Test
  fun sequenceWithSnakeCaseNaming() {
    val formatSnakeCase =
      DsvFormat(
        scheme = DsvScheme(delimiter = ',', writeCrlf = false),
        namingStrategy = DsvNamingStrategy.SnakeCase,
      )

    @Serializable
    data class CamelCaseSample(
      val firstName: String,
      val lastName: String,
      val emailAddress: String,
      val phoneNumber: String?,
    )

    val samples =
      sequenceOf(
        CamelCaseSample("John", "Doe", "john@example.com", "555-1234"),
        CamelCaseSample("Jane", "Smith", "jane@example.com", null),
      )

    val sink = Buffer()
    formatSnakeCase.encodeSequenceToSink(samples, sink)
    val result = sink.readString()

    val expected =
      """
      first_name,last_name,email_address,phone_number
      John,Doe,john@example.com,555-1234
      Jane,Smith,jane@example.com,
      
      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = formatSnakeCase.decodeSourceToSequence<CamelCaseSample>(Buffer().apply { writeString(result) }).toList()
    assertEquals(samples.toList(), decoded)
  }

  @Test
  fun sequenceWithEnumsByOrdinal() {
    val formatByOrdinal =
      DsvFormat(DsvScheme(delimiter = ',', writeCrlf = false), writeEnumsByName = false)

    val samples = sequenceOf(Sample(1, "Item", 9.99, null, true, Status.PENDING, null))

    val sink = Buffer()
    formatByOrdinal.encodeSequenceToSink(samples, sink)
    val result = sink.readString()

    val expected =
      """
      id,name,price,count,active,status,description
      1,Item,9.99,,true,2,
      
      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = formatByOrdinal.decodeSourceToSequence<Sample>(Buffer().apply { writeString(result) }).toList()
    assertEquals(samples.toList(), decoded)
  }

  @Test
  fun sequenceWithTreatMissingColumnsAsNull() {
    val formatWithOption =
      DsvFormat(
        scheme = DsvScheme(delimiter = ',', writeCrlf = false),
        treatMissingColumnsAsNull = true,
      )

    @Serializable
    data class PartialSample(
      val id: Int?,
      val name: String?,
      val age: Byte,
      val quantity: Short,
      val rating: Float,
      val grade: Char,
    )

    val csvWithMissingColumns =
      """
      age,quantity,rating,grade
      25,100,4.5,A
      30,200,3.8,B
      """
        .trimIndent()

    val source = Buffer().apply { writeString(csvWithMissingColumns) }
    val decoded = formatWithOption.decodeSourceToSequence<PartialSample>(source).toList()

    val expected =
      listOf(
        PartialSample(null, null, 25, 100, 4.5f, 'A'),
        PartialSample(null, null, 30, 200, 3.8f, 'B'),
      )

    assertEquals(expected, decoded)
  }
}
