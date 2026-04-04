package dev.sargunv.kotlindsv

import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertEquals

class TableTest {
  @Test
  fun recordsAsMaps() {
    val maps =
      DsvTable(listOf("a", "b", "c"), sequenceOf(listOf("1", "2", "3"), listOf("4", "5", "6")))
        .recordsAsMaps()
        .toList()
    assertEquals(
      listOf(mapOf("a" to "1", "b" to "2", "c" to "3"), mapOf("a" to "4", "b" to "5", "c" to "6")),
      maps,
    )
  }

  @Test
  fun recordsAsMapsEmpty() {
    val maps = DsvTable(listOf("a,b,c"), emptySequence()).recordsAsMaps().toList()
    assertEquals(emptyList(), maps)
  }
}
