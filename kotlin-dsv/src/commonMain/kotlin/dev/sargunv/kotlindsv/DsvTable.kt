package dev.sargunv.kotlindsv

/**
 * Represents a [DSV][DsvFormat] table with a header row and data records.
 *
 * @property header The column names from the first row.
 * @property records The data rows, each containing values for all columns. A [Sequence] is used to
 *   support streaming large files.
 */
public data class DsvTable(val header: List<String>, val records: Sequence<List<String>>) {
  /** Converts records to a sequence of maps, using header values as keys. */
  public fun recordsAsMaps(): Sequence<Map<String, String>> =
    records.map { record -> header.zip(record).toMap() }
}
