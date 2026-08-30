package dev.sargunv.kotlindsv

import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.Buffer
import kotlinx.io.writeString

class ParserTest {

  private inline fun testCase(
    input: String,
    scheme: DsvScheme = Csv.scheme,
    block: DsvParser.() -> Unit,
  ) {
    val buffer = Buffer()
    buffer.writeString(input)
    val parser = DsvParser(buffer, scheme)
    parser.block()
  }

  fun rows() =
    testCase(
      input =
        """
        a|b|c
        1|2|3
        4|5|6
        """
          .trimIndent(),
      scheme = DsvScheme('|'),
    ) {
      assertEquals(
        sequenceOf(listOf("a", "b", "c"), listOf("1", "2", "3"), listOf("4", "5", "6")).toList(),
        parseRecords().toList(),
      )
    }

  @Test fun emptyRows() = testCase("") { assertEquals(emptyList(), parseRecords().toList()) }

  @Test
  fun headerOnlyRows() =
    testCase("a,b,c") { assertEquals(listOf(listOf("a", "b", "c")), parseRecords().toList()) }

  fun table() =
    testCase(
      input =
        """
        a|b|c
        1|2|3
        4|5|6
        """
          .trimIndent(),
      scheme = DsvScheme('|'),
    ) {
      val (header, rows) = parseTable()
      assertEquals(listOf("a", "b", "c"), header)
      assertEquals(listOf(listOf("1", "2", "3"), listOf("4", "5", "6")), rows.toList())
    }

  @Test fun emptyTable() = testCase("") { assertFailsWith<DsvParseException> { parseTable() } }

  @Test
  fun headerOnlyTable() =
    testCase("a,b,c") {
      val (header, rows) = parseTable()
      assertEquals(listOf("a", "b", "c"), header)
      assertEquals(emptyList(), rows.toList())
    }

  @Test
  fun spaces() =
    testCase(" a , b , c ") {
      assertEquals(listOf(listOf(" a ", " b ", " c ")), parseRecords().toList())
    }

  @Test
  fun quotes() =
    testCase(
      """
      "comma,comma","newline
      newline","quote""quote"
      """
        .trimIndent()
    ) {
      assertEquals(
        listOf(listOf("comma,comma", "newline\nnewline", "quote\"quote")),
        parseRecords().toList(),
      )
    }

  @Test
  fun singleColumn() =
    testCase("a\nb") { assertEquals(listOf(listOf("a"), listOf("b")), parseRecords().toList()) }

  @Test
  fun singleColumnCrlf() =
    testCase("a\r\nb") { assertEquals(listOf(listOf("a"), listOf("b")), parseRecords().toList()) }

  @Test
  fun singleColumnCrcrlf() =
    testCase("a\r\r\nb") { assertEquals(listOf(listOf("a"), listOf("b")), parseRecords().toList()) }

  @Test
  fun singleField() = testCase("a") { assertEquals(listOf(listOf("a")), parseRecords().toList()) }

  @Test
  fun firstEmptyField() =
    testCase(",b,c") { assertEquals(listOf(listOf("", "b", "c")), parseRecords().toList()) }

  @Test
  fun trailingNewline() =
    testCase("a,b,c\n") { assertEquals(listOf(listOf("a", "b", "c")), parseRecords().toList()) }

  @Test
  fun trailingNewline2() =
    testCase("a,b,c\n\n") { assertFailsWith<DsvParseException> { parseRecords().toList() } }

  @Test
  fun trailingNewline2Skip() =
    testCase("a,b,c\n\n", scheme = Csv.scheme.copy(skipEmptyLines = true)) {
      assertEquals(listOf(listOf("a", "b", "c")), parseRecords().toList())
    }

  @Test
  fun leadingEmptyLineSkip() =
    testCase("\na,b\n1,2", scheme = Csv.scheme.copy(skipEmptyLines = true)) {
      assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parseRecords().toList())
    }

  @Test
  fun skipEmptyLinesOnlyEmptyInput() =
    testCase("\n\n", scheme = Csv.scheme.copy(skipEmptyLines = true)) {
      assertEquals(emptyList(), parseRecords().toList())
    }

  @Test
  fun trailingNewlineCrlf() =
    testCase("a,b,c\r\n") { assertEquals(listOf(listOf("a", "b", "c")), parseRecords().toList()) }

  @Test
  fun middleEmptyField() =
    testCase("a,,c") { assertEquals(listOf(listOf("a", "", "c")), parseRecords().toList()) }

  @Test
  fun lastEmptyField() =
    testCase("a,b,") { assertEquals(listOf(listOf("a", "b", "")), parseRecords().toList()) }

  @Test
  fun unterminatedQuotedValue() =
    testCase("\"unterminated") { assertFailsWith<DsvParseException> { parseRecords().toList() } }

  @Test
  fun unexpectedQuoteInNonQuotedField() =
    testCase("a,b\"c,d") { assertFailsWith<DsvParseException> { parseRecords().toList() } }

  @Test
  fun unexpectedCharacterAfterField() =
    testCase("\"quoted\"x,b") { assertFailsWith<DsvParseException> { parseRecords().toList() } }

  @Test
  fun wrongNumberOfColumns() =
    testCase("a,b,c\n1,2") { assertFailsWith<DsvParseException> { parseRecords().toList() } }

  @Test
  fun unexpectedDataAtEnd() =
    testCase("a,b,c\n1,2,3\nextra") {
      assertFailsWith<DsvParseException> { parseRecords().toList() }
    }

  @Test
  fun leadingUtf8Bom() =
    testCase("\uFEFFa,b,c") { assertEquals(listOf(listOf("a", "b", "c")), parseRecords().toList()) }

  @Test
  fun leadingUtf8BomTable() =
    testCase("\uFEFFagency_id\nDTA") {
      val (header, rows) = parseTable()
      assertEquals(listOf("agency_id"), header)
      assertEquals(listOf(listOf("DTA")), rows.toList())
    }

  @Test
  fun leadingUtf8BomQuotedField() =
    testCase("\uFEFF\"a,b\",c") {
      assertEquals(listOf(listOf("a,b", "c")), parseRecords().toList())
    }

  @Test
  fun bomInsideQuotedFirstFieldIsPreserved() =
    testCase("\"\uFEFFa\",b") {
      assertEquals(listOf(listOf("\uFEFFa", "b")), parseRecords().toList())
    }

  @Test fun bomOnly() = testCase("\uFEFF") { assertEquals(emptyList(), parseRecords().toList()) }

  @Test
  fun bomInLaterFieldIsPreserved() =
    testCase("a,\uFEFFb,c") {
      assertEquals(listOf(listOf("a", "\uFEFFb", "c")), parseRecords().toList())
    }

  @Test
  fun bomInLaterRowIsPreserved() =
    testCase("a,b\n\uFEFF1,2") {
      assertEquals(listOf(listOf("a", "b"), listOf("\uFEFF1", "2")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeShorter() =
    testCase("a,b,c\n1,2", scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize)) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeLonger() =
    testCase(
      "a,b,c\n1,2,3,4",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeMixed() =
    testCase(
      "a,b,c\n1\n2,3,4,5\n6,7,8",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize),
    ) {
      assertEquals(
        listOf(
          listOf("a", "b", "c"),
          listOf("1", "", ""),
          listOf("2", "3", "4"),
          listOf("6", "7", "8"),
        ),
        parseRecords().toList(),
      )
    }

  @Test
  fun jaggedRowsNormalizeSingleColumn() =
    testCase("a\n1,2", scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize)) {
      assertEquals(listOf(listOf("a"), listOf("1")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeWithQuotes() =
    testCase(
      "\"a\",\"b\",\"c\"\n\"1\"\n\"2\",\"3\",\"4\",\"5\"",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize),
    ) {
      assertEquals(
        listOf(listOf("a", "b", "c"), listOf("1", "", ""), listOf("2", "3", "4")),
        parseRecords().toList(),
      )
    }

  @Test
  fun jaggedRowsRejectLonger() =
    testCase("a,b,c\n1,2,3,4") { assertFailsWith<DsvParseException> { parseRecords().toList() } }

  @Test
  fun jaggedRowsNormalizeSkipEmptyLines() =
    testCase(
      "a,b,c\n1,2\n\n3,4,5,6",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize, skipEmptyLines = true),
    ) {
      assertEquals(
        listOf(listOf("a", "b", "c"), listOf("1", "2", ""), listOf("3", "4", "5")),
        parseRecords().toList(),
      )
    }

  @Test
  fun jaggedRowsNormalizeSkipLeadingEmptyLines() =
    testCase(
      "\n\na,b,c\n1,2",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize, skipEmptyLines = true),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeLeadingEmptyLineWithoutSkip() =
    testCase(
      "\na,b\n1,2",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize),
    ) {
      assertEquals(listOf(listOf(""), listOf("a"), listOf("1")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeWithBom() =
    testCase(
      "\uFEFFa,b,c\n1,2",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsNormalizeParseTable() =
    testCase(
      "a,b,c\n1\n2,3,4,5",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Normalize),
    ) {
      val table = parseTable()
      assertEquals(listOf("a", "b", "c"), table.header)
      assertEquals(
        listOf(mapOf("a" to "1", "b" to "", "c" to ""), mapOf("a" to "2", "b" to "3", "c" to "4")),
        table.recordsAsMaps().toList(),
      )
    }

  @Test
  fun jaggedRowsSkipShorter() =
    testCase(
      "a,b,c\n1,2\n3,4,5",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("3", "4", "5")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsSkipLonger() =
    testCase(
      "a,b,c\n1,2,3,4\n5,6,7",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("5", "6", "7")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsSkipMixed() =
    testCase(
      "a,b,c\n1\n2,3,4,5\n6,7,8",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("6", "7", "8")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsSkipAllDataRows() =
    testCase(
      "a,b,c\n1\n2,3,4,5",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip),
    ) {
      assertEquals(listOf(listOf("a", "b", "c")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsSkipEmptyLineIsJagged() =
    testCase(
      "a,b,c\n1,2\n\n3,4,5",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("3", "4", "5")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsSkipAndSkipEmptyLines() =
    testCase(
      "a,b,c\n1,2\n\n3,4,5,6\n7,8,9",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip, skipEmptyLines = true),
    ) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("7", "8", "9")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsSkipParseTable() =
    testCase(
      "a,b,c\n1\n2,3,4\n5,6,7,8",
      scheme = Csv.scheme.copy(jaggedRowPolicy = JaggedRowPolicy.Skip),
    ) {
      val table = parseTable()
      assertEquals(listOf("a", "b", "c"), table.header)
      assertEquals(
        listOf(mapOf("a" to "2", "b" to "3", "c" to "4")),
        table.recordsAsMaps().toList(),
      )
    }
}
