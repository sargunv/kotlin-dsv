package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

class CsvKitParserTest {
  private val examplesPath = "src/fsTest/resources/csvkit/examples"

  private fun readFile(filename: String): String {
    return SystemFileSystem.source(Path("$examplesPath/$filename")).buffered().use { it.readString() }
  }

  private fun parseRecords(csv: String, delimiter: Char = ','): List<List<String>> {
    val buffer = Buffer()
    buffer.writeString(csv)
    val parser = DsvParser(buffer, DsvScheme(delimiter))
    return parser.parseRecords().toList()
  }

  private fun parseTable(csv: String, delimiter: Char = ','): DsvTable {
    val buffer = Buffer()
    buffer.writeString(csv)
    val parser = DsvParser(buffer, DsvScheme(delimiter))
    return parser.parseTable()
  }

  private fun validTestCase(filename: String, delimiter: Char = ',') {
    val csv = readFile(filename)
    val records = parseRecords(csv, delimiter)
    assertTrue(records.size >= 0)
  }

  private fun invalidTestCase(filename: String, delimiter: Char = ',') {
    val csv = readFile(filename)
    assertFailsWith<DsvParseException> { parseRecords(csv, delimiter) }
  }

  @Test
  fun testBadCsv() = invalidTestCase("bad.csv")

  @Test
  fun testDummyCsv() {
    val csv = readFile("dummy.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(1, rows.size)
    assertEquals(3, rows.first().size)
    assertEquals("1", rows.first()[0])
    assertEquals("2", rows.first()[1])
    assertEquals("3", rows.first()[2])
  }

  @Test
  fun testDummy2Csv() {
    val csv = readFile("dummy2.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(1, rows.size)
    assertEquals(3, rows.first().size)
  }

  @Test
  fun testDummy3Csv() {
    val csv = readFile("dummy3.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(2, rows.size)
    assertEquals(3, rows.first().size)
  }

  @Test
  fun testEmptyCsv() {
    val csv = readFile("empty.csv")
    val records = parseRecords(csv)
    assertEquals(1, records.size)
    assertEquals(1, records.first().size)
    assertEquals("", records.first()[0])
  }

  @Test
  fun testBlanksCsv() {
    val csv = readFile("blanks.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(1, rows.size)
    assertEquals(6, rows.first().size)
  }

  @Test
  fun testIrisCsv() {
    val csv = readFile("iris.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(150, rows.size)
    assertEquals(5, rows.first().size)
  }

  @Test
  fun testMacNewlinesCsv() = validTestCase("mac_newlines.csv")

  @Test
  fun testNoHeaderRowCsv() = validTestCase("no_header_row.csv")

  @Test
  fun testOptionalQuoteCharactersCsv() = validTestCase("optional_quote_characters.csv")

  @Test
  fun testTestUtf8Csv() {
    val csv = readFile("test_utf8.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(2, rows.size)
    assertEquals(3, rows.first().size)
  }

  @Test
  fun testJoinACsv() = validTestCase("join_a.csv")

  @Test
  fun testJoinBCsv() = validTestCase("join_b.csv")

  @Test
  fun testSortIntsNullsCsv() = validTestCase("sort_ints_nulls.csv")

  @Test
  fun testTestPrecisionCsv() = validTestCase("test_precision.csv")

  @Test
  fun testTestSkipLinesCsv() = invalidTestCase("test_skip_lines.csv")

  @Test
  fun testDateLikeNumberCsv() = validTestCase("date_like_number.csv")

  @Test
  fun testSniffLimitCsv() = validTestCase("sniff_limit.csv")

  @Test
  fun testDummyTsv() {
    val tsv = readFile("dummy.tsv")
    val table = parseTable(tsv, delimiter = '\t')
    val rows = table.records.toList()
    assertEquals(1, rows.size)
    assertEquals(3, rows.first().size)
  }

  @Test
  fun testFoo1Csv() = validTestCase("foo1.csv")

  @Test
  fun testFoo2Csv() = validTestCase("foo2.csv")

  @Test
  fun testTestGeoJsonCsv() = validTestCase("test_geojson.csv")

  @Test
  fun testIrismetaCsv() = validTestCase("irismeta.csv")

  @Test
  fun testNullByteCsv() = validTestCase("null_byte.csv")

  @Test
  fun testTestJsonConvertedCsv() = validTestCase("testjson_converted.csv")

  @Test
  fun testTestJsonNestedConvertedCsv() = validTestCase("testjson_nested_converted.csv")

  @Test
  fun testBadSkipLinesCsv() = invalidTestCase("bad_skip_lines.csv")

  @Test
  fun testRealdataFY09() = validTestCase("realdata/FY09_EDU_Recipients_by_State.csv")

  @Test
  fun testRealdataDatagovFY10() = validTestCase("realdata/Datagov_FY10_EDU_recp_by_State.csv")

  @Test
  fun testRealdataAcs2012() = validTestCase("realdata/acs2012_5yr_population.csv")

  @Test
  fun testRealdataKs1033() = validTestCase("realdata/ks_1033_data.csv")

  @Test
  fun testRealdataReadme() = validTestCase("realdata/README.csv")

  @Test
  fun testRealdataCensus2000GeoSchema() =
    validTestCase("realdata/census_2000/census2000_geo_schema.csv")

  @Test
  fun testRealdataCensus2000Determination() =
    validTestCase("realdata/census_2000/determination.csv")

  @Test
  fun testRealdataCensus2000DeterminationSchema() =
    validTestCase("realdata/census_2000/determination_schema.csv")

  @Test
  fun testRealdataCensus2010GeoSchema() =
    validTestCase("realdata/census_2010/census2010_geo_schema.csv")

  @Test
  fun testRealdataCensus2010IlGeoExcerpt() =
    validTestCase("realdata/census_2010/ilgeo2010_excerpt.csv")
}
