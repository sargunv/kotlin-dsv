@file:Suppress("PropertyName")

package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable

// Based on https://github.com/liwt31/china-city-subway-csv

class ChinaSubwayDataTest {
  private val samplesPath = "src/fsTest/resources/china-city-subway-csv"

  private fun openFile(filename: String): Source {
    return SystemFileSystem.source(Path("$samplesPath/$filename")).buffered()
  }

  private inline fun <reified T> encodeDecodeTestCase(
    csvFilename: String,
    numRecordsExpected: Int,
    format: DsvFormat = Csv,
  ) {
    val originalData = openFile(csvFilename)
    val decodedData = format.decodeFromSource<List<T>>(originalData)
    assertEquals(numRecordsExpected, decodedData.size)

    val encodedData = kotlinx.io.Buffer()
    format.encodeToSink(decodedData, encodedData)

    val decodedData2 = format.decodeFromSource<List<T>>(encodedData)
    decodedData.zip(decodedData2).forEachIndexed { i, (decoded1, decoded2) ->
      assertEquals(decoded1, decoded2, "Decoded data mismatch on record $i")
    }
  }

  @Serializable
  private data class City(
    val id: Int,
    val cn_name: String,
    val en_name: String,
    val code: Int,
    val pre: String,
    val created_at: String,
    val updated_at: String,
  )

  @Serializable
  private data class Line(
    val id: Int,
    val name: String,
    val uid: String,
    val pair_uid: String,
    val city_id: Int,
    val created_at: String,
    val updated_at: String,
  )

  @Serializable
  private data class Step(
    val id: Int,
    val name: String,
    val uid: String,
    val lat: Double,
    val lng: Double,
    val is_practical: Int,
    val line_id: Int,
    val created_at: String,
    val updated_at: String,
  )

  @Test fun china_subway_cities() = encodeDecodeTestCase<City>("cities.csv", 33)

  @Test fun china_subway_lines() = encodeDecodeTestCase<Line>("lines.csv", 385)

  @Test fun china_subway_steps() = encodeDecodeTestCase<Step>("steps.csv", 7335)
}
