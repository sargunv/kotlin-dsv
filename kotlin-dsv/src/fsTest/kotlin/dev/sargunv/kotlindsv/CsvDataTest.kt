@file:Suppress("PropertyName")

package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.Serializable

// Based on https://github.com/vincentlaucsb/csv-data/tree/master

class CsvDataTest {
  private val samplesPath = "src/fsTest/resources/csv-data"

  private fun openFile(filename: String): Source {
    return SystemFileSystem.source(Path("$samplesPath/$filename")).buffered()
  }

  private inline fun <reified T, reified E : Exception> invalidTestCase(
    csvFilename: String,
    format: DsvFormat = Csv,
  ) {
    val originalCsv = openFile(csvFilename).use { it.readString() }
    assertFailsWith<E> { format.decodeFromString<List<T>>(originalCsv) }
  }

  private inline fun <reified T> encodeDecodeTestCase(
    csvFilename: String,
    numRecordsExpected: Int,
    format: DsvFormat = Csv,
  ) {
    val originalData = openFile(csvFilename)
    val decodedData = format.decodeFromSource<List<T>>(originalData)
    assertEquals(numRecordsExpected, decodedData.size)

    val encodedData = Buffer()
    format.encodeToSink(decodedData, encodedData)

    val decodedData2 = format.decodeFromSource<List<T>>(encodedData)
    decodedData.zip(decodedData2).forEachIndexed { i, (decoded1, decoded2) ->
      assertEquals(decoded1, decoded2, "Decoded data mismatch on record $i")
    }
  }

  @Serializable
  private data class ABCDEFGHIJ(
    val A: Int,
    val B: Int,
    val C: Int,
    val D: Int,
    val E: Int,
    val F: Int,
    val G: Int,
    val H: Int,
    val I: Int,
    val J: Int,
  )

  @Test
  fun fake_ints_newline_sep_csv() =
    encodeDecodeTestCase<ABCDEFGHIJ>("fake_data/ints_newline_sep.csv", 100)

  @Test
  fun fake_ints_doesnt_end_in_newline() =
    encodeDecodeTestCase<ABCDEFGHIJ>("fake_data/ints_doesnt_end_in_newline.csv", 100)

  @Test
  fun fake_ints_skipline_csv() =
    invalidTestCase<ABCDEFGHIJ, NumberFormatException>("fake_data/ints_skipline.csv")

  @Test
  fun fake_delimiter() {
    @Serializable
    data class FakeUser(
      val username: String,
      val identifier: Int,
      val firstName: String,
      val lastName: String,
    )

    encodeDecodeTestCase<FakeUser>(
      "fake_data/delimeter.csv",
      5,
      DsvFormat(DsvScheme(';'), namingStrategy = DsvNamingStrategy.PascalCase),
    )
  }

  @Test
  fun mimesis_persons() {
    @Serializable
    data class MimesisPerson(
      val id: String,
      val fullName: String,
      val age: Int,
      val occupation: String,
      val email: String,
      val telephone: String,
      val nationality: String,
    )
    encodeDecodeTestCase<MimesisPerson>(
      "mimesis_data/persons.csv",
      50000,
      DsvFormat(Csv.scheme, namingStrategy = DsvNamingStrategy.TitleCaseWords),
    )
  }

  @Test
  fun real_2016_gaz_place_national() {
    @Serializable
    data class Place(
      val USPS: String,
      val GEOID: Long,
      val ANSICODE: Long,
      val NAME: String,
      val LSAD: String,
      val FUNCSTAT: Char,
      val ALAND: Long,
      val AWATER: Long,
      val ALAND_SQMI: Double,
      val AWATER_SQMI: Double,
      val INTPTLAT: Double,
      val INTPTLONG: Double,
    )

    encodeDecodeTestCase<Place>("real_data/2016_Gaz_place_national.txt", 29575, Tsv)
  }

  @Test
  fun real_2009_power_status() {
    @Serializable data class PowerStatus(val ReportDt: String, val Unit: String, val Power: Int)
    encodeDecodeTestCase<PowerStatus>(
      "real_data/2009PowerStatus.txt",
      37960,
      DsvFormat(DsvScheme('|')),
    )
  }

  @Test
  fun real_year07_cbsa_nac3() {
    @Serializable
    data class CbsaNac3(
      val CBSA_LABEL: String,
      val NAC3_Label: String,
      val _TYPE_: String,
      val TOTAL_UNIT: Int,
      // many, many, many more in the actual data ...
    )

    encodeDecodeTestCase<CbsaNac3>(
      "real_data/YEAR07_CBSA_NAC3.txt",
      9331,
      DsvFormat(DsvScheme(';'), ignoreUnknownKeys = true),
    )
  }

  @Test
  fun real_us_states() {
    @Serializable data class UsState(val State: String, val Abbreviation: String)
    encodeDecodeTestCase<UsState>(
      "real_data/us_states/states.csv",
      51, // includes DC
    )
  }

  @Test
  fun real_gdpc1() {
    @Serializable data class GDPC1(val DATE: String, val GDPC1: Double)
    encodeDecodeTestCase<GDPC1>("real_data/GDPC1.csv", 281)
  }

  @Test
  fun real_aug15_sample() {
    @Serializable
    data class Tweet(
      val id: String,
      val user_id: String,
      val user_name: String,
      val screen_name: String,
      val full_text: String,
      // many, many, many more in the actual data ...
    )
    encodeDecodeTestCase<Tweet>(
      "real_data/aug15_sample.csv",
      50000,
      DsvFormat(Csv.scheme, ignoreUnknownKeys = true),
    )
  }

  @Test
  fun real_storm_events() {
    @Serializable
    data class StormEvent(
      val YEARMONTH: Int,
      val EPISODE_ID: Long,
      val EVENT_ID: Long,
      val LOCATION_INDEX: Int,
      val RANGE: Double,
      val AZIMUTH: String,
      val LOCATION: String,
      val LATITUDE: Double,
      val LONGITUDE: Double,
      val LAT2: Long,
      val LON2: Long,
    )
    encodeDecodeTestCase<StormEvent>(
      "real_data/noaa_storm_events/StormEvents_locations-ftp_v1.0_d2014_c20170718.csv",
      53974,
    )
    encodeDecodeTestCase<StormEvent>(
      "real_data/noaa_storm_events/StormEvents_locations-ftp_v1.0_d2015_c20170718.csv",
      54979,
    )
    encodeDecodeTestCase<StormEvent>(
      "real_data/noaa_storm_events/StormEvents_locations-ftp_v1.0_d2016_c20170816.csv",
      42265,
    )
    encodeDecodeTestCase<StormEvent>(
      "real_data/noaa_storm_events/StormEvents_locations-ftp_v1.0_d2017_c20170816.csv",
      24130,
    )
  }
}
