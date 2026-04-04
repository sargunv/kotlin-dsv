package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.io.Buffer
import kotlinx.serialization.Serializable

class StreamingTest {

  @Serializable data class TestData(val id: Int, val name: String, val value: Double)

  fun generateTestData() =
    generateSequence(1) { if (it < 1000) it + 1 else null }
      .map { TestData(it, "Name $it", it * 1.5) }

  @Test
  fun streamingDecode() {
    val buffer = Buffer()

    Csv.encodeToSink(generateTestData(), buffer)
    val data = Csv.decodeFromSource<TestData>(buffer)

    // Assert first data comes before buffer is exhausted
    var asserted = false
    data.forEach { _ ->
      if (!asserted) {
        assertFalse(buffer.exhausted())
        asserted = true
      }
    }
    assertTrue(buffer.exhausted())
  }

  @Test
  fun streamingEncode() {
    val buffer = Buffer()

    assertEquals(0, buffer.size)
    var lastSize = 0L

    // Assert the buffer grows while we're producing data
    val testData =
      generateTestData().map { item ->
        assertTrue(buffer.size > lastSize)
        lastSize = buffer.size
        assertNotEquals(0, lastSize)
        item
      }

    Csv.encodeToSink(testData, buffer)
  }
}
