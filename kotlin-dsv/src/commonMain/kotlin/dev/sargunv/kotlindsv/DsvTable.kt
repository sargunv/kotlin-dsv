package dev.sargunv.kotlindsv

public data class DsvTable(val header: List<String>, val records: Sequence<List<String>>) {
  public fun recordsAsMaps(): Sequence<Map<String, String>> =
    records.map { record -> header.zip(record).toMap() }
}
