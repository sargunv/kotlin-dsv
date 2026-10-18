package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable

class SkipInitialSpaceTest {

  private val scheme = Csv.scheme.copy(skipInitialSpace = true)

  private fun parse(input: String, scheme: DsvScheme = this.scheme): List<List<String>> =
    DsvParser(Buffer().apply { writeString(input) }, scheme).parseRecords().toList()

  private val nftaRoutes =
    "route_id,agency_id,route_short_name,route_long_name,route_url,route_desc,route_type," +
      "route_color,route_text_color,route_sort_order\r\n" +
      "251,NFTA,251,Game Day Express,http://www.nfta.com/251, \"Bills Game Day Service between " +
      "Niagara Falls Transportation Center and Highmark Stadium.\",3,E12A40,FFFFFF,\r\n"

  @Serializable
  data class Route(
    val routeId: String,
    val agencyId: String,
    val routeShortName: String,
    val routeLongName: String,
    val routeUrl: String,
    val routeDesc: String,
    val routeType: Int,
    val routeColor: String,
    val routeTextColor: String,
    val routeSortOrder: Int?,
  )

  @Test
  fun readsNftaRouteWithSpaceBeforeQuotedField() {
    assertEquals(
      listOf(
        Route(
          routeId = "251",
          agencyId = "NFTA",
          routeShortName = "251",
          routeLongName = "Game Day Express",
          routeUrl = "http://www.nfta.com/251",
          routeDesc =
            "Bills Game Day Service between Niagara Falls Transportation Center and Highmark " +
              "Stadium.",
          routeType = 3,
          routeColor = "E12A40",
          routeTextColor = "FFFFFF",
          routeSortOrder = null,
        ),
      ),
      DsvFormat(scheme, namingStrategy = DsvNamingStrategy.SnakeCase)
        .decodeFromString<Route>(nftaRoutes),
    )
  }

  @Test
  fun defaultRejectsSpaceBeforeQuotedField() {
    assertFailsWith<DsvParseException> { parse(nftaRoutes, Csv.scheme) }
  }

  @Test
  fun skipsSpacesBeforeQuotedFieldAtStartOfRecord() {
    assertEquals(listOf(listOf("a", "b"), listOf("c,d", "e")), parse("  \"a\",b\n \"c,d\",e\n"))
  }

  @Test
  fun skipsSpacesBeforeUnquotedFields() {
    assertEquals(listOf(listOf("a", "b c ", "d")), parse(" a,  b c , d"))
  }

  @Test
  fun keepsSpacesInsideQuotedFields() {
    assertEquals(listOf(listOf("a", " b ")), parse("a, \" b \""))
  }

  @Test
  fun spaceOnlyFieldsAreEmpty() {
    assertEquals(listOf(listOf("a", "", "b", "")), parse("a,  ,b,   "))
  }

  @Test
  fun spaceOnlyLinesAreEmptyLines() {
    assertEquals(
      listOf(listOf("a"), listOf("b")),
      parse("a\n   \nb\n  ", scheme.copy(skipEmptyLines = true)),
    )
  }

  @Test
  fun quoteInsideUnquotedFieldStillFails() {
    assertFailsWith<DsvParseException> { parse("a, b\"c\"") }
  }

  @Test
  fun spaceAfterClosingQuoteStillFails() {
    assertFailsWith<DsvParseException> { parse("\"a\" ,b") }
  }

  @Test
  fun skipsOnlySpaces() {
    assertFailsWith<DsvParseException> { parse("a,\t\"b\"") }
  }

  @Test
  fun writesLeadingSpacesQuotedSoTheyReadBack() {
    val records = listOf(listOf(" a", "b ", "  "))
    val buffer = Buffer()
    DsvWriter(buffer, scheme).write(records)
    val written = buffer.readString()
    assertEquals("\" a\",b ,\"  \"\r\n", written)
    assertEquals(records, parse(written))
  }

  @Test
  fun defaultWritesLeadingSpacesUnquoted() {
    val buffer = Buffer()
    DsvWriter(buffer, Csv.scheme).write(listOf(listOf(" a", "b")))
    assertEquals(" a,b\r\n", buffer.readString())
  }

  @Test
  fun spaceCannotBeDelimiterOrQuote() {
    assertFailsWith<IllegalArgumentException> {
      DsvScheme(delimiter = ' ', skipInitialSpace = true)
    }
    assertFailsWith<IllegalArgumentException> {
      DsvScheme(delimiter = ',', quote = ' ', skipInitialSpace = true)
    }
  }

  @Test
  fun recordDelimiterCannotStartWithSpace() {
    assertFailsWith<IllegalArgumentException> {
      DsvScheme(
        delimiter = ',',
        recordDelimiter = RecordDelimiter.Exact(" ;"),
        skipInitialSpace = true,
      )
    }
  }
}
