@file:OptIn(ExperimentalSerializationApi::class)

package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SerializerExceptionTest {

  private val format = DsvFormat(DsvScheme(delimiter = ',', recordDelimiter = RecordDelimiter.Lf))

  @Serializable data class Person(val name: String, val age: Int)

  @Serializable
  data class StopTime(
    val tripId: String,
    @Serializable(with = StrictTimeSerializer::class) val arrivalTime: String,
  )

  @Serializable
  data class NonNegative(val value: Int) {
    init {
      require(value >= 0) { "must be non-negative" }
    }
  }

  object StrictTimeSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("StrictTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
      require(value.count { it == ':' } == 2) { "Invalid time: $value" }
      encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
      val raw = decoder.decodeString()
      require(raw.count { it == ':' } == 2) { "Invalid time: $raw" }
      return raw
    }
  }

  object SerializationExceptionSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("SeString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
      throw SerializationException("custom encoding failure")
    }

    override fun deserialize(decoder: Decoder): String {
      decoder.decodeString()
      throw SerializationException("custom decoding failure")
    }
  }

  @Serializable
  data class SeRow(@Serializable(with = SerializationExceptionSerializer::class) val value: String)

  @Test
  fun customSerializerIllegalArgumentExceptionIsWrapped() {
    val e =
      assertFailsWith<DsvDecodingException> {
        format.decodeFromString<StopTime>("tripId,arrivalTime\nA,1:2\n")
      }
    assertEquals(1, e.record)
    assertEquals("arrivalTime", e.columnName)
    assertEquals(1, e.columnIndex)
    val cause = assertIs<IllegalArgumentException>(e.cause)
    assertEquals("Invalid time: 1:2", cause.message)
    assertTrue(e.message!!.contains("Invalid time: 1:2"))
    assertTrue(e.message!!.contains("at record 1, column 'arrivalTime'"))
    assertFalse(cause is SerializationException)
  }

  @Test
  fun wrapsSecondRecordAndKeepsCause() {
    val e =
      assertFailsWith<DsvDecodingException> {
        format.decodeFromString<StopTime>("tripId,arrivalTime\nA,00:00:00\nB,nope\n")
      }
    assertEquals(2, e.record)
    assertEquals("arrivalTime", e.columnName)
    assertEquals("Invalid time: nope", e.cause?.message)
  }

  @Test
  fun primitiveNumberFormatExceptionIsWrapped() {
    val e =
      assertFailsWith<DsvDecodingException> {
        format.decodeFromString<Person>("name,age\nAda,thirty\n")
      }
    assertEquals(1, e.record)
    assertEquals("age", e.columnName)
    assertEquals(1, e.columnIndex)
    assertIs<NumberFormatException>(e.cause)
    e.cause?.message?.let { assertTrue(it in e.message!!) }
    assertTrue(e.message!!.contains("at record 1, column 'age'"))
  }

  @Test
  fun constructorRequireIsWrappedWithoutColumn() {
    val e =
      assertFailsWith<DsvDecodingException> { format.decodeFromString<NonNegative>("value\n-1\n") }
    assertEquals(1, e.record)
    assertNull(e.columnName)
    assertNull(e.columnIndex)
    val cause = assertIs<IllegalArgumentException>(e.cause)
    assertEquals("must be non-negative", cause.message)
    assertTrue(e.message!!.contains("must be non-negative at record 1"))
    assertFalse(e.message!!.contains("column"))
  }

  @Test
  fun serializationExceptionFromSerializerIsRethrown() {
    val e = assertFailsWith<SerializationException> { format.decodeFromString<SeRow>("value\nx\n") }
    assertEquals("custom decoding failure", e.message)
    assertFalse(e is DsvDecodingException)
    assertFalse(e is DsvParseException)
  }

  @Test
  fun missingFieldExceptionIsWrappedWithLocation() {
    val e = assertFailsWith<DsvDecodingException> { format.decodeFromString<Person>("name\nAda\n") }
    assertEquals(1, e.record)
    assertNull(e.columnName)
    assertTrue(e.message!!.contains("at record 1"))
    val cause = assertIs<MissingFieldException>(e.cause)
    assertEquals(listOf("age"), cause.missingFields)
  }

  @Test
  fun parseErrorsStayDsvParseException() {
    val e =
      assertFailsWith<DsvParseException> {
        format.decodeFromString<Person>("name,age\nAda,30\n\"unterminated")
      }
    assertEquals("Unterminated quoted value", e.message)
    assertNull(e.cause)
  }

  @Test
  fun unknownColumnIsDsvDecodingException() {
    val e =
      assertFailsWith<DsvDecodingException> {
        format.decodeFromString<Person>("name,age,extra\nAda,30,x\n")
      }
    assertEquals(1, e.record)
    assertEquals("extra", e.columnName)
    assertEquals(2, e.columnIndex)
    assertNull(e.cause)
    assertTrue(e.message!!.contains("Unknown column 'extra'"))
    assertTrue(e.message!!.contains("at record 1, column 'extra'"))
  }

  @Test
  fun encodingWrapsCustomSerializerFailure() {
    val e =
      assertFailsWith<DsvEncodingException> {
        format.encodeToString(listOf(StopTime("A", "00:00:00"), StopTime("B", "nope")))
      }
    assertEquals(2, e.record)
    assertEquals("arrivalTime", e.columnName)
    assertEquals(1, e.columnIndex)
    val cause = assertIs<IllegalArgumentException>(e.cause)
    assertEquals("Invalid time: nope", cause.message)
    assertTrue(e.message!!.contains("Invalid time: nope"))
    assertTrue(e.message!!.contains("at record 2, column 'arrivalTime'"))
  }

  @Test
  fun encodingRethrowsSerializationException() {
    val e = assertFailsWith<SerializationException> { format.encodeToString(listOf(SeRow("x"))) }
    assertEquals("custom encoding failure", e.message)
    assertFalse(e is DsvEncodingException)
  }

  @Test
  fun tsvUsesTheSameWrapping() {
    val e =
      assertFailsWith<DsvDecodingException> {
        Tsv.decodeFromString<Person>("name\tage\nAda\tthirty\n")
      }
    assertEquals(1, e.record)
    assertEquals("age", e.columnName)
    assertIs<NumberFormatException>(e.cause)
  }
}
