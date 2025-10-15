package dev.sargunv.kotlindsv

import kotlin.jvm.JvmName
import kotlinx.io.Sink
import kotlinx.io.writeString

public class DsvWriter(private val sink: Sink, private val encoding: DsvEncoding) {
  private var numColumns = -1
  private var rowCount = 0

  private fun Sink.writeChar(c: Char) {
    writeString(c.toString())
  }

  private fun writeField(field: String) {
    if (
      field.any {
        it == encoding.delimiter ||
          it == encoding.quote ||
          it == encoding.newline ||
          it == encoding.carriageReturn
      }
    ) {
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

  @JvmName("writeTable")
  public fun write(table: DsvTable): Unit = write(sequenceOf(table.header) + table.records)

  @JvmName("writeRecords")
  public fun write(table: Sequence<List<String>>): Unit = sink.use { table.forEach(::writeRecord) }

  @JvmName("writeRecords")
  public fun write(table: List<List<String>>): Unit = write(table.asSequence())

  @JvmName("writeTable")
  public fun write(table: Sequence<Map<String, String>>): Unit =
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

  @JvmName("writeTable")
  public fun write(table: List<Map<String, String>>): Unit = write(table.asSequence())
}
