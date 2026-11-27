package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.Buffer
import kotlinx.io.writeString
import kotlinx.serialization.Serializable

class TrimFieldsTest {

  @Serializable data class Text(val value: String)

  @Serializable data class Number(val value: Int)

  @Serializable
  enum class Status {
    ACTIVE,
    INACTIVE,
  }

  @Serializable data class EnumValue(val value: Status)

  @Serializable
  data class NullableFields(val text: String?, val number: Double?, val status: Status?)

  private val format = DsvFormat(Csv.scheme, trimFields = true)

  @Test
  fun trimUnquotedAndQuotedFields() {
    val csv = "value\n State & Harrison/Ida B Wells Drive \n\" \t005B95 \"\n"
    assertEquals(
      listOf(Text("State & Harrison/Ida B Wells Drive"), Text("005B95")),
      format.decodeFromString<Text>(csv),
    )
  }

  @Test
  fun trimAfterUnquotingPreservesInternalWhitespaceAndQuotes() {
    val csv = "value\n\"\u00a0\n A  \"\"B\"\"\tC \r\n\"\n"
    assertEquals(listOf(Text("A  \"B\"\tC")), format.decodeFromString<Text>(csv))
  }

  @Test
  fun whitespaceOnlyNullableFieldsAreNull() {
    val csv = "text,number,status\n , , \n\" \t\",\" \n\",\"\u00a0\"\n"
    assertEquals(
      listOf(NullableFields(null, null, null), NullableFields(null, null, null)),
      format.decodeFromString<NullableFields>(csv),
    )
  }

  @Test
  fun whitespaceOnlyNonNullableStringIsEmpty() {
    val csv = "value\n \n\" \t\"\n"
    assertEquals(listOf(Text(""), Text("")), format.decodeFromString<Text>(csv))
  }

  @Test
  fun whitespaceOnlyNonNullableNumbersAndEnumsStillFail() {
    for (value in listOf(" ", "\" \t\"")) {
      assertFailsWith<DsvDecodingException> { format.decodeFromString<Number>("value\n$value\n") }
      assertFailsWith<DsvDecodingException> {
        format.decodeFromString<EnumValue>("value\n$value\n")
      }
    }
  }

  @Test
  fun paddedNumbersAndEnumsDecodeOnlyWithTrimming() {
    for (value in listOf(" 42 ", "\" 42 \"")) {
      val csv = "value\n$value\n"
      assertFailsWith<DsvDecodingException> { Csv.decodeFromString<Number>(csv) }
      assertEquals(listOf(Number(42)), format.decodeFromString<Number>(csv))
    }
    for (value in listOf(" ACTIVE ", "\" ACTIVE \"", " 0 ", "\" 0 \"")) {
      val csv = "value\n$value\n"
      assertFailsWith<DsvDecodingException> { Csv.decodeFromString<EnumValue>(csv) }
      assertEquals(listOf(EnumValue(Status.ACTIVE)), format.decodeFromString<EnumValue>(csv))
    }
  }

  @Test
  fun paddedHeadersAreAlwaysTrimmedBeforeNamingStrategy() {
    @Serializable data class Named(val stopName: String)

    for (trimFields in listOf(false, true)) {
      val namedFormat =
        DsvFormat(
          Csv.scheme,
          namingStrategy = DsvNamingStrategy.SnakeCase,
          trimFields = trimFields,
        )
      for (header in listOf(" stop_name ", "\" \tstop_name \"")) {
        assertEquals(
          listOf(Named("Central")),
          namedFormat.decodeFromString<Named>("$header\nCentral\n"),
        )
      }
    }
  }

  @Test
  fun defaultPreservesFieldWhitespace() {
    val csv = "value\n padded \n\" \tquoted \n\"\n \n"
    assertEquals(
      listOf(Text(" padded "), Text(" \tquoted \n"), Text(" ")),
      Csv.decodeFromString<Text>(csv),
    )
    assertEquals(
      listOf(NullableFields(" ", null, null)),
      Csv.decodeFromString<NullableFields>("text,number,status\n ,,\n"),
    )
  }

  @Test
  fun trimStreamingRecords() {
    val source = Buffer().apply { writeString("value\n first \n\" second \"\n") }
    assertEquals(
      listOf(Text("first"), Text("second")),
      format.decodeFromSource<Text>(source).toList(),
    )
  }

  @Test
  fun encodingPreservesWhitespaceWithTrimmingEnabled() {
    val values = listOf(Text(" padded "), Text(" \tquoted \n"))
    val expected = "value\r\n padded \r\n\" \tquoted \n\"\r\n"
    assertEquals(expected, Csv.encodeToString(values))
    assertEquals(expected, format.encodeToString(values))
  }
}
