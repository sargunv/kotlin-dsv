package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement

class CsvKitTest {
  private val examplesPath = "src/fsTest/resources/csvkit/examples"

  private fun readFile(filename: String): String {
    return SystemFileSystem.source(Path("$examplesPath/$filename")).buffered().use { it.readString() }
  }

  private inline fun <reified T> testRoundTrip(csvFilename: String, jsonFilename: String) {
    val csv = readFile(csvFilename)
    val json = readFile(jsonFilename)

    val expectedJson = Json.decodeFromString<JsonArray>(json)
    val decoded = Csv.decodeFromString<List<T>>(csv)
    val decodedJson = Json.encodeToJsonElement(decoded)
    assertEquals(expectedJson, decodedJson)

    val encoded = Csv.encodeToString(decoded)
    val decoded2 = Csv.decodeFromString<List<T>>(encoded)
    assertEquals(decoded, decoded2)
  }

  @Test
  fun testGeojsonSkipped() {
    // TODO: This test is skipped because it contains nested JSON structures which are not yet
    // supported (see issue #3). When nested serialization is implemented, uncomment the following:
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
