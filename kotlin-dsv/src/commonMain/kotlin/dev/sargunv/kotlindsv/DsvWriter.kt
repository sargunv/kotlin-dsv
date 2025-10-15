package dev.sargunv.kotlindsv

import kotlin.jvm.JvmName
import kotlinx.io.Sink
import kotlinx.io.writeString

public class DsvWriter(private val sink: Sink, private val scheme: DsvScheme) {
  private var numColumns = -1
  private var rowCount = 0

  private fun Sink.writeChar(c: Char) = writeString(c.toString())

  private fun writeField(field: String) =
    if (
      field.any {
        it == scheme.delimiter ||
          it == scheme.quote ||
          it == scheme.newline ||
          it == scheme.carriageReturn
      }
    ) {
      sink.writeChar(scheme.quote)
      sink.writeString(
        field.replace(scheme.quote.toString(), scheme.quote.toString() + scheme.quote)
      )
      sink.writeChar(scheme.quote)
    } else {
      sink.writeString(field)
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
      if (i > 0) sink.writeChar(scheme.delimiter)
      writeField(field)
    }

    if (scheme.writeCrlf) sink.writeChar(scheme.carriageReturn)
    sink.writeChar(scheme.newline)
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
