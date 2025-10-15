package dev.sargunv.kotlindsv

import kotlinx.io.Sink
import kotlinx.io.writeString

public class DsvWriter(private val sink: Sink, private val encoding: DsvEncoding) {
  private var numColumns = -1
  private var rowCount = 0

  private fun Sink.writeChar(c: Char) {
    writeString(c.toString())
  }

  private fun writeField(field: String) {
    if (field.contains(encoding.delimiter) || field.contains(encoding.quote)) {
      sink.writeChar(encoding.quote)
      sink.writeString(
        field.replace(encoding.quote.toString(), encoding.quote.toString() + encoding.quote)
      )
      sink.writeChar(encoding.quote)
    } else {
      sink.writeString(field)
    }
  }

  internal fun writeRecord(record: List<String>) {
    if (numColumns < 0) numColumns = record.size
    else {
      require(record.size == numColumns) {
        "Row $rowCount has ${record.size} columns; expected $numColumns"
      }
    }
    rowCount++

    record.forEachIndexed { i, field ->
      if (i > 0) sink.writeChar(encoding.delimiter)
      writeField(field)
    }

    if (encoding.writeCrlf) sink.writeChar(encoding.carriageReturn)
    sink.writeChar(encoding.newline)
  }

  public fun write(table: Sequence<List<String>>): Unit = sink.use { table.forEach(::writeRecord) }

  public fun write(table: DsvTable): Unit = write(sequenceOf(table.header) + table.records)

  public fun write(table: List<List<String>>): Unit = write(table.asSequence())

  public fun writeMaps(table: Sequence<Map<String, String>>): Unit =
    write(
      sequence<List<String>> {
        val keys = table.firstOrNull()?.keys ?: return@sequence
        yield(keys.toList())
        for (row in table) {
          require(row.keys == keys) { "All rows must have the same keys" }
          yield(keys.map { row[it]!! })
        }
      }
    )

  public fun writeMaps(table: List<Map<String, String>>): Unit = writeMaps(table.asSequence())
}
