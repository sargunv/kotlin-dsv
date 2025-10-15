package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertFailsWith

class EncodingTest {

  @Test
  fun quoteEqualsDelimiter() {
    assertFailsWith<IllegalArgumentException> { DsvEncoding(delimiter = ',', quote = ',') }
  }

  @Test
  fun quoteEqualsNewline() {
    assertFailsWith<IllegalArgumentException> { DsvEncoding(delimiter = ',', quote = '\n') }
  }

  @Test
  fun quoteEqualsCarriageReturn() {
    assertFailsWith<IllegalArgumentException> { DsvEncoding(delimiter = ',', quote = '\r') }
  }

  @Test
  fun delimiterEqualsNewline() {
    assertFailsWith<IllegalArgumentException> { DsvEncoding(delimiter = '\n') }
  }

  @Test
  fun delimiterEqualsCarriageReturn() {
    assertFailsWith<IllegalArgumentException> { DsvEncoding(delimiter = '\r') }
  }
}
