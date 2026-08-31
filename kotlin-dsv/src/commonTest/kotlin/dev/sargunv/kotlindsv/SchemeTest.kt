package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SchemeTest {

  @Test
  fun quoteEqualsDelimiter() {
    assertFailsWith<IllegalArgumentException> { DsvScheme(delimiter = ',', quote = ',') }
  }

  @Test
  fun quoteEqualsNewline() {
    assertFailsWith<IllegalArgumentException> { DsvScheme(delimiter = ',', quote = '\n') }
  }

  @Test
  fun quoteEqualsCarriageReturn() {
    assertFailsWith<IllegalArgumentException> { DsvScheme(delimiter = ',', quote = '\r') }
  }

  @Test
  fun delimiterEqualsNewline() {
    assertFailsWith<IllegalArgumentException> { DsvScheme(delimiter = '\n') }
  }

  @Test
  fun delimiterEqualsCarriageReturn() {
    assertFailsWith<IllegalArgumentException> { DsvScheme(delimiter = '\r') }
  }

  @Test
  fun emptyWriteRecordDelimiter() {
    assertFailsWith<IllegalArgumentException> { RecordDelimiter("") }
  }

  @Test
  fun emptyReadRecordDelimiter() {
    assertFailsWith<IllegalArgumentException> { RecordDelimiter(write = "\n", read = emptyList()) }
  }

  @Test
  fun emptyReadToken() {
    assertFailsWith<IllegalArgumentException> {
      RecordDelimiter(write = "\n", read = listOf("\n", ""))
    }
  }

  @Test
  fun quoteInExactRecordDelimiter() {
    assertFailsWith<IllegalArgumentException> {
      DsvScheme(delimiter = ',', recordDelimiter = RecordDelimiter("\n\"\n"))
    }
  }

  @Test
  fun delimiterInExactRecordDelimiter() {
    assertFailsWith<IllegalArgumentException> {
      DsvScheme(delimiter = ',', recordDelimiter = RecordDelimiter(",\n"))
    }
  }

  @Test
  fun newlineFieldDelimiterWithCustomRecordDelimiter() {
    DsvScheme(delimiter = '\n', recordDelimiter = RecordDelimiter("%%"))
  }
}
