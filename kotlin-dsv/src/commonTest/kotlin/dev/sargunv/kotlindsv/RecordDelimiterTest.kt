package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString

class RecordDelimiterTest {

  private fun parse(input: String, scheme: DsvScheme): List<List<String>> {
    val buffer = Buffer()
    buffer.writeString(input)
    return DsvParser(buffer, scheme).parseRecords().toList()
  }

  private fun write(records: List<List<String>>, scheme: DsvScheme): String {
    val buffer = Buffer()
    DsvWriter(buffer, scheme).write(records)
    return buffer.readString()
  }

  private fun scheme(recordDelimiter: RecordDelimiter) =
    DsvScheme(delimiter = ',', recordDelimiter = recordDelimiter)

  @Test
  fun conventionalLfWritesUnixNewlines() {
    val output = write(listOf(listOf("a", "b"), listOf("1", "2")), scheme(RecordDelimiter.Lf))
    assertEquals("a,b\n1,2\n", output)
  }

  @Test
  fun conventionalCrlfWritesWindowsNewlines() {
    val output = write(listOf(listOf("a", "b"), listOf("1", "2")), scheme(RecordDelimiter.CrLf))
    assertEquals("a,b\r\n1,2\r\n", output)
  }

  @Test
  fun conventionalReadAcceptsLfCrlfAndCrcrlf() {
    val scheme = scheme(RecordDelimiter.CrLf)
    assertEquals(listOf(listOf("a"), listOf("b")), parse("a\nb", scheme))
    assertEquals(listOf(listOf("a"), listOf("b")), parse("a\r\nb", scheme))
    assertEquals(listOf(listOf("a"), listOf("b")), parse("a\r\r\nb", scheme))
  }

  @Test
  fun conventionalReadLeavesAThirdCrInField() {
    val scheme = scheme(RecordDelimiter.CrLf)
    assertEquals(listOf(listOf("a\r"), listOf("b")), parse("a\r\r\r\nb", scheme))
  }

  @Test
  fun customReadSetCanIncludeCarriageReturn() {
    val scheme = scheme(RecordDelimiter(write = "\n", read = listOf("\n", "\r\n", "\r")))
    assertEquals(listOf(listOf("a"), listOf("b")), parse("a\rb", scheme))
  }

  @Test
  fun lfIsWritePlusConventionalRead() {
    assertEquals(
      RecordDelimiter(write = "\n", read = listOf("\r\r\n", "\r\n", "\n")),
      RecordDelimiter.Lf,
    )
    assertEquals(
      RecordDelimiter(write = "\r\n", read = listOf("\r\r\n", "\r\n", "\n")),
      RecordDelimiter.CrLf,
    )
  }

  @Test
  fun longestReadTokenWins() {
    val scheme = scheme(RecordDelimiter(write = "%%", read = listOf("%", "%%")))
    assertEquals(listOf(listOf("a"), listOf("b")), parse("a%%b", scheme))
  }

  @Test
  fun exactCrReadsAndWritesOldMacFiles() {
    val scheme = scheme(RecordDelimiter("\r"))
    assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parse("a,b\r1,2\r", scheme))
    assertEquals("a,b\r1,2\r", write(listOf(listOf("a", "b"), listOf("1", "2")), scheme))
  }

  @Test
  fun exactLfKeepsCarriageReturnInField() {
    val scheme = scheme(RecordDelimiter("\n"))
    assertEquals(listOf(listOf("a\r"), listOf("b")), parse("a\r\nb", scheme))
  }

  @Test
  fun exactMultiCharacterMysqlLinesTerminatedBy() {
    val scheme = scheme(RecordDelimiter("\n%%\n"))
    val input = "a,b\n%%\n1,2\n%%\n"
    assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parse(input, scheme))
    assertEquals(input, write(listOf(listOf("a", "b"), listOf("1", "2")), scheme))
  }

  @Test
  fun exactRecordSeparatorChar() {
    val scheme = scheme(RecordDelimiter("\u001e"))
    val input = "a,b\u001e1,2\u001e"
    assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parse(input, scheme))
    assertEquals(input, write(listOf(listOf("a", "b"), listOf("1", "2")), scheme))
  }

  @Test
  fun prefixOfMultiCharacterDelimiterStaysInField() {
    val scheme = scheme(RecordDelimiter("%%"))
    assertEquals(listOf(listOf("a%", "b"), listOf("1", "2")), parse("a%,b%%1,2", scheme))
  }

  @Test
  fun quotedFieldMayContainExactDelimiter() {
    val scheme = scheme(RecordDelimiter("%%"))
    assertEquals(listOf(listOf("a%%b", "c")), parse("\"a%%b\",c%%", scheme))
    assertEquals("\"a%%b\",c%%", write(listOf(listOf("a%%b", "c")), scheme))
  }

  @Test
  fun exactDelimiterDoesNotQuoteLoneNewlines() {
    val scheme = scheme(RecordDelimiter("%%"))
    assertEquals("a\nb,c%%", write(listOf(listOf("a\nb", "c")), scheme))
    assertEquals(listOf(listOf("a\nb", "c")), parse("a\nb,c%%", scheme))
  }

  @Test
  fun conventionalStillQuotesEmbeddedNewlines() {
    val scheme = scheme(RecordDelimiter.Lf)
    assertEquals("\"a\nb\",c\n", write(listOf(listOf("a\nb", "c")), scheme))
  }

  @Test
  fun skipEmptyRecordsWithExactDelimiter() {
    val scheme =
      DsvScheme(
        delimiter = ',',
        recordDelimiter = RecordDelimiter("%%"),
        skipEmptyLines = true,
      )
    assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parse("a,b%%%%1,2%%", scheme))
  }

  @Test
  fun trailingExactDelimiterIsOptional() {
    val scheme = scheme(RecordDelimiter("%%"))
    assertEquals(listOf(listOf("a", "b")), parse("a,b", scheme))
  }

  @Test
  fun incompleteExactDelimiterIsFieldContent() {
    val scheme = scheme(RecordDelimiter("\n%%\n"))
    assertEquals(listOf(listOf("a", "b\n%%")), parse("a,b\n%%", scheme))
  }

  @Test
  fun unexpectedCharacterAfterExactField() {
    val scheme = scheme(RecordDelimiter("%%"))
    assertFailsWith<DsvParseException> { parse("\"quoted\"x%%", scheme) }
  }

  @Test
  fun newlineFieldDelimiterWithPercentRecords() {
    val scheme = DsvScheme(delimiter = '\n', recordDelimiter = RecordDelimiter("%%"))
    assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parse("a\nb%%1\n2", scheme))
  }
}
