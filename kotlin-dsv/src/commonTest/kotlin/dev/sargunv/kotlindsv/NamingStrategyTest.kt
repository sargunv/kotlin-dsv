package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals

class NamingStrategyTest {

  private fun testCase(strategy: DsvNamingStrategy, kotlinName: String, dsvName: String) {
    assertEquals(dsvName, strategy.toDsvName(kotlinName))
    assertEquals(kotlinName, strategy.fromDsvName(dsvName))
  }

  @Test
  fun testIdentity() =
    testCase(DsvNamingStrategy.Identity, kotlinName = "fooBarBaz", dsvName = "fooBarBaz")

  @Test
  fun testSnakeCase() =
    testCase(DsvNamingStrategy.SnakeCase, kotlinName = "fooBarBaz", dsvName = "foo_bar_baz")

  @Test
  fun testKebabCase() =
    testCase(DsvNamingStrategy.KebabCase, kotlinName = "fooBarBaz", dsvName = "foo-bar-baz")

  @Test
  fun testReversed() =
    testCase(
      DsvNamingStrategy.SnakeCase.reversed(),
      kotlinName = "foo_bar_baz",
      dsvName = "fooBarBaz",
    )

  @Test
  fun testComposite() =
    testCase(
      DsvNamingStrategy.Composite(
        DsvNamingStrategy.KebabCase.reversed(),
        DsvNamingStrategy.SnakeCase,
      ),
      kotlinName = "foo-bar-baz",
      dsvName = "foo_bar_baz",
    )

  @Test
  fun testPascalCase() =
    testCase(DsvNamingStrategy.PascalCase, kotlinName = "fooBarBaz", dsvName = "FooBarBaz")

  @Test
  fun testTitleCaseWords() =
    testCase(DsvNamingStrategy.TitleCaseWords, kotlinName = "fooBarBaz", dsvName = "Foo Bar Baz")

  @Test
  fun testSentenceCaseWords() =
    testCase(
      DsvNamingStrategy.SentenceCaseWords,
      kotlinName = "fooBarBaz",
      dsvName = "Foo bar baz",
    )

  @Test
  fun testLowercaseWords() =
    testCase(DsvNamingStrategy.LowercaseWords, kotlinName = "fooBarBaz", dsvName = "foo bar baz")

  @Test
  fun testUppercaseWords() =
    testCase(DsvNamingStrategy.UppercaseWords, kotlinName = "fooBarBaz", dsvName = "FOO BAR BAZ")
}
