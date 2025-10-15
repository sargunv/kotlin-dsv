package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.Buffer
import kotlinx.io.readString

class WriterTest {

  private inline fun testCase(
    expected: String,
    delimiter: Char = ',',
    block: DsvWriter.() -> Unit,
  ) {
    val buffer = Buffer()
    val writer = DsvWriter(buffer, DsvScheme(delimiter))
    writer.block()
    assertEquals(expected.trim(), buffer.readString().trim())
  }

  private inline fun simpleTestCase(block: DsvWriter.() -> Unit) =
    testCase(
      expected =
        """
        a|b|c
        1|2|3
        4|5|6
        7|8|9
        """
          .trimIndent(),
      delimiter = '|',
      block = block,
    )

  @Test
  fun table() = simpleTestCase {
    write(
      DsvTable(
        header = listOf("a", "b", "c"),
        records = sequenceOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")),
      )
    )
  }

  @Test
  fun listSequence() = simpleTestCase {
    write(
      sequenceOf(
        listOf("a", "b", "c"),
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
      )
    )
  }

  @Test
  fun listList() = simpleTestCase {
    write(
      listOf(
        listOf("a", "b", "c"),
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
      )
    )
  }

  @Test
  fun mapSequence() = simpleTestCase {
    write(
      sequenceOf(
        mapOf("a" to "1", "b" to "2", "c" to "3"),
        mapOf("a" to "4", "b" to "5", "c" to "6"),
        mapOf("a" to "7", "b" to "8", "c" to "9"),
      )
    )
  }

  @Test
  fun mapList() = simpleTestCase {
    write(
      listOf(
        mapOf("a" to "1", "b" to "2", "c" to "3"),
        mapOf("a" to "4", "b" to "5", "c" to "6"),
        mapOf("a" to "7", "b" to "8", "c" to "9"),
      )
    )
  }

  @Test fun spaces() = testCase(" a , b , c ") { write(listOf(listOf(" a ", " b ", " c "))) }

  @Test
  fun quotes() =
    testCase(
      """
      "comma,comma","newline
      newline","quote""quote"
      """
        .trimIndent()
    ) {
      write(listOf(listOf("comma,comma", "newline\nnewline", "quote\"quote")))
    }

  @Test fun noRows() = testCase("") { write(listOf(emptyList())) }

  @Test fun emptyFields() = testCase(",,") { write(listOf(listOf("", "", ""))) }
}
