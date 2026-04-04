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
}
