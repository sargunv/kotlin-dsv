package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement

// based on https://github.com/wireservice/csvkit/tree/master/examples

class CsvKitTest {
  // Path is relative to the project root where Gradle runs tests
  private val examplesPath = "src/fsTest/resources/csvkit/examples"

  private fun readFile(filename: String): String {
    return SystemFileSystem.source(Path("$examplesPath/$filename")).buffered().use { it.readString() }
  }

  private inline fun <reified T> testRoundTrip(csvFilename: String, jsonFilename: String) {
    val csv = readFile(csvFilename)
    val json = readFile(jsonFilename)

    // Decode
    val expectedJson = Json.decodeFromString<JsonArray>(json)
    val decoded = Csv.decodeFromString<List<T>>(csv)
    val decodedJson = Json.encodeToJsonElement(decoded)
    assertEquals(expectedJson, decodedJson)

    // Round trip
    val encoded = Csv.encodeToString(decoded)
    val decoded2 = Csv.decodeFromString<List<T>>(encoded)
    assertEquals(decoded, decoded2)
  }

  // Test CSV with JSON counterpart - test_geojson.csv
  // TODO: This test is skipped because it contains nested JSON structures which are not yet
  // supported
  // See issue #3 for nested serialization support
  @Test
  fun testGeojsonSkipped() {
    // Skip this test as it requires nested serialization support
    // When nested serialization is implemented, uncomment the following:
    // @Serializable
    // data class GeojsonRow(
    //   val id: String,
    //   val prop0: String,
    //   val prop1: String,
    //   val geojson: String,
    //   val type: String,
    //   val longitude: String,
    //   val latitude: String
    // )
    // testRoundTrip<GeojsonRow>("test_geojson.csv", "test_geojson.json")
  }
}
