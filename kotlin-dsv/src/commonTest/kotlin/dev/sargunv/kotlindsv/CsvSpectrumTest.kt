package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement
import org.intellij.lang.annotations.Language

// based on https://github.com/max-mapper/csv-spectrum

class CsvSpectrumTest {

  private inline fun <reified T> testCase(
    @Language("csv") csv: String,
    @Language("json") json: String,
  ) {
    val expectedJson = Json.decodeFromString<JsonArray>(json)
    val actual = Csv.decodeFromString<List<T>>(csv)
    val actualJson = Json.encodeToJsonElement(actual)
    assertEquals(expectedJson, actualJson)
  }

  @Serializable
  private data class NameAddress(
    val first: String,
    val last: String,
    val address: String,
    val city: String,
    val zip: String,
  )

  @Serializable private data class ABC(val a: String, val b: String, val c: String)

  @Serializable private data class AB(val a: String, val b: String)

  @Serializable private data class KeyVal(val key: String, val `val`: String)

  @Serializable
  @Suppress("PropertyName")
  private data class Location(
    val `Contact Phone Number`: String,
    val `Location Coordinates`: String,
    val Cities: String,
    val Counties: String,
  )

  @Test
  fun commaInQuotes() =
    testCase<NameAddress>(
      csv =
        """
        first,last,address,city,zip
        John,Doe,120 any st.,"Anytown, WW",08123
        """
          .trimIndent(),
      json =
        """
        [
          {
            "first": "John",
            "last": "Doe",
            "address": "120 any st.",
            "city": "Anytown, WW",
            "zip": "08123"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun empty() =
    testCase<ABC>(
      csv =
        """
        a,b,c
        1,"",""
        2,3,4
        """
          .trimIndent(),
      json =
        """
        [
          { "a": "1", "b": "", "c": "" },
          { "a": "2", "b": "3", "c": "4" }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun emptyCrlf() =
    testCase<ABC>(
      csv = "a,b,c\r\n" + "1,\"\",\"\"\r\n" + "2,3,4",
      json =
        """
        [
          { "a": "1", "b": "", "c": "" },
          { "a": "2", "b": "3", "c": "4" }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun escapedQuotes() =
    testCase<AB>(
      csv =
        """
        a,b
        1,"ha ""ha"" ha"
        3,4
        """
          .trimIndent(),
      json =
        """
        [
          {
            "a": "1",
            "b": "ha \"ha\" ha"
          },
          {
            "a": "3",
            "b": "4"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun json() =
    testCase<KeyVal>(
      csv =
        """
        key,val
        1,"{""type"": ""Point"", ""coordinates"": [102.0, 0.5]}"
        """
          .trimIndent(),
      json =
        """
        [
          {
            "key": "1",
            "val": "{\"type\": \"Point\", \"coordinates\": [102.0, 0.5]}"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun locationCoordinates() =
    testCase<Location>(
      csv =
        """
        Contact Phone Number,Location Coordinates,Cities,Counties
        1234567890,37°36′37.8″N 121°2′17.9″W,Modesto,Stanislaus
        """
          .trimIndent(),
      json =
        """
        [
          {
            "Contact Phone Number": "1234567890",
            "Location Coordinates": "37°36′37.8″N 121°2′17.9″W",
            "Cities": "Modesto",
            "Counties": "Stanislaus"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun newlines() =
    testCase<ABC>(
      csv =
        """
        a,b,c
        1,2,3
        "Once upon${" "}
        a time",5,6
        7,8,9
        """
          .trimIndent(),
      json =
        """
        [
          {
            "a": "1",
            "b": "2",
            "c": "3"
          },
          {
            "a": "Once upon \na time",
            "b": "5",
            "c": "6"
          },
          {
            "a": "7",
            "b": "8",
            "c": "9"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun newlinesCrlf() =
    testCase<ABC>(
      csv = "a,b,c\r\n" + "1,2,3\r\n" + "\"Once upon \r\n" + "a time\",5,6\r\n" + "7,8,9",
      json =
        """
        [
          {
            "a": "1",
            "b": "2",
            "c": "3"
          },
          {
            "a": "Once upon \r\na time",
            "b": "5",
            "c": "6"
          },
          {
            "a": "7",
            "b": "8",
            "c": "9"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun quotesAndNewlines() =
    testCase<AB>(
      csv =
        """
        a,b
        1,"ha${" "}
        ""ha""${" "}
        ha"
        3,4
        """
          .trimIndent(),
      json =
        """
        [
          {
            "a": "1",
            "b": "ha \n\"ha\" \nha"
          },
          {
            "a": "3",
            "b": "4"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun simple() =
    testCase<ABC>(
      csv =
        """
        a,b,c
        1,2,3
        """
          .trimIndent(),
      json =
        """
        [
          {
            "a": "1",
            "b": "2",
            "c": "3"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun simpleCrlf() =
    testCase<ABC>(
      csv = "a,b,c\r\n" + "1,2,3",
      json =
        """
        [
          {
            "a": "1",
            "b": "2",
            "c": "3"
          }
        ]
        """
          .trimIndent(),
    )

  @Test
  fun utf8() =
    testCase<ABC>(
      csv =
        """
        a,b,c
        1,2,3
        4,5,ʤ
        """
          .trimIndent(),
      json =
        """
        [
          {
            "a": "1",
            "b": "2",
            "c": "3"
          },
          {
            "a": "4",
            "b": "5",
            "c": "ʤ"
          }
        ]
        """
          .trimIndent(),
    )
}
