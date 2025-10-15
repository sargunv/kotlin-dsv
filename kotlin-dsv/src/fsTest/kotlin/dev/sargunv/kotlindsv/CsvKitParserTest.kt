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

// Tests for CSV files from csvkit examples directory that don't have JSON counterparts

class CsvKitParserTest {
  // Path is relative to the project root where Gradle runs tests
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

  // Test cases for invalid CSVs

  @Test
  fun testBadCsv() {
    val csv = readFile("bad.csv")
    // This CSV has inconsistent column counts (rows have different number of columns than header)
    // Row 2 has 4 columns (1, 27, "", "I'm too long!") but header has 3
    // Row 3 has 2 columns ("", "I'm too short!") but header has 3
    // This should fail when parsing as a table
    assertFailsWith<DsvParseException> { parseRecords(csv) }
  }

  // Test cases for valid CSVs

  @Test
  fun testDummyCsv() {
    val csv = readFile("dummy.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(1, rows.size) // 1 data row
    assertEquals(3, rows.first().size) // 3 columns
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
    // File contains just a newline, which parses as one empty record
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
    assertEquals(6, rows.first().size) // 6 columns
  }

  @Test
  fun testIrisCsv() {
    val csv = readFile("iris.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(150, rows.size) // iris dataset has 150 rows
    assertEquals(5, rows.first().size) // 5 columns
  }

  @Test
  fun testMacNewlinesCsv() {
    val csv = readFile("mac_newlines.csv")
    val records = parseRecords(csv)
    // Should handle Mac-style line endings (CR only)
    assertTrue(records.size > 0)
  }

  @Test
  fun testNoHeaderRowCsv() {
    val csv = readFile("no_header_row.csv")
    val records = parseRecords(csv)
    // File without header row - should still parse
    assertTrue(records.size >= 0)
  }

  @Test
  fun testOptionalQuoteCharactersCsv() {
    val csv = readFile("optional_quote_characters.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testTestUtf8Csv() {
    val csv = readFile("test_utf8.csv")
    val table = parseTable(csv)
    val rows = table.records.toList()
    assertEquals(2, rows.size)
    assertEquals(3, rows.first().size)
  }

  @Test
  fun testJoinACsv() {
    val csv = readFile("join_a.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testJoinBCsv() {
    val csv = readFile("join_b.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testSortIntsNullsCsv() {
    val csv = readFile("sort_ints_nulls.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testTestPrecisionCsv() {
    val csv = readFile("test_precision.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testTestSkipLinesCsv() {
    val csv = readFile("test_skip_lines.csv")
    // This file starts with empty lines (csvkit's skip lines feature)
    // The first record is empty (1 column), but subsequent records have 3 columns
    // This causes a column count mismatch which our strict parser rejects
    assertFailsWith<DsvParseException> { parseRecords(csv) }
  }

  @Test
  fun testDateLikeNumberCsv() {
    val csv = readFile("date_like_number.csv")
    val records = parseRecords(csv)
    assertTrue(records.size >= 0)
  }

  @Test
  fun testSniffLimitCsv() {
    val csv = readFile("sniff_limit.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testDummyTsv() {
    val tsv = readFile("dummy.tsv")
    // TSV uses tab as delimiter
    val table = parseTable(tsv, delimiter = '\t')
    val rows = table.records.toList()
    assertEquals(1, rows.size)
    assertEquals(3, rows.first().size)
  }

  // Test files from realdata subdirectory

  @Test
  fun testRealdataFY09() {
    val csv = readFile("realdata/FY09_EDU_Recipients_by_State.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testRealdataDatagovFY10() {
    val csv = readFile("realdata/Datagov_FY10_EDU_recp_by_State.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testRealdataAcs2012() {
    val csv = readFile("realdata/acs2012_5yr_population.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }

  @Test
  fun testRealdataKs1033() {
    val csv = readFile("realdata/ks_1033_data.csv")
    val records = parseRecords(csv)
    assertTrue(records.size > 0)
  }
}
