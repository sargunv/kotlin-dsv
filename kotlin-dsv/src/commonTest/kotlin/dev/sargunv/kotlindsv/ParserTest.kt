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
  fun jaggedRowsShorter() =
    testCase("a,b,c\n1,2", scheme = Csv.scheme.copy(allowJaggedRows = true)) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsLonger() =
    testCase("a,b,c\n1,2,3,4", scheme = Csv.scheme.copy(allowJaggedRows = true)) {
      assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsMixed() =
    testCase("a,b,c\n1\n2,3,4,5\n6,7,8", scheme = Csv.scheme.copy(allowJaggedRows = true)) {
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
  fun jaggedRowsSingleColumn() =
    testCase("a\n1,2", scheme = Csv.scheme.copy(allowJaggedRows = true)) {
      assertEquals(listOf(listOf("a"), listOf("1")), parseRecords().toList())
    }

  @Test
  fun jaggedRowsWithQuotes() =
    testCase(
      "\"a\",\"b\",\"c\"\n\"1\"\n\"2\",\"3\",\"4\",\"5\"",
      scheme = Csv.scheme.copy(allowJaggedRows = true),
    ) {
      assertEquals(
        listOf(listOf("a", "b", "c"), listOf("1", "", ""), listOf("2", "3", "4")),
        parseRecords().toList(),
      )
    }

  @Test
  fun jaggedRowsDisabledShorter() =
    testCase("a,b,c\n1,2", scheme = Csv.scheme.copy(allowJaggedRows = false)) {
      assertFailsWith<DsvParseException> { parseRecords().toList() }
    }

  @Test
  fun jaggedRowsDisabledLonger() =
    testCase("a,b,c\n1,2,3,4", scheme = Csv.scheme.copy(allowJaggedRows = false)) {
      assertFailsWith<DsvParseException> { parseRecords().toList() }
    }
}
