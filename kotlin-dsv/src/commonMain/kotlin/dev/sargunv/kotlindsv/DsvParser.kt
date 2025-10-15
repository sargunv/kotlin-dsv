package dev.sargunv.kotlindsv

import kotlinx.io.Source

/**
 * Low-level parser for [DSV][DsvFormat] data.
 *
 * Reads from a [Source] and parses according to the provided [DsvScheme]. For typical use cases,
 * prefer using [DsvFormat] instead.
 */
public class DsvParser(private val input: Source, private val scheme: DsvScheme) {
  private var data = StringBuilder()
  private val buffer = ByteArray(4096)

  private data class ReadResult<T>(val value: T, val newPos: Int)

  private fun charAt(pos: Int): Char? {
    while (data.length <= pos) {
      if (input.exhausted()) return null
      val numBytesRead = input.readAtMostTo(buffer, 0, buffer.size)
      data.append(buffer.decodeToString(0, numBytesRead))
    }
    return data[pos]
  }

  private fun readQuotedField(pos: Int): ReadResult<String>? {
    var cursor = pos

    // accept opening quote
    if (charAt(cursor) != scheme.quote) return null
    cursor++

    val result = StringBuilder()
    while (true) {
      // require content
      val c = charAt(cursor) ?: throw DsvParseException("Unterminated quoted value")
      if (c == scheme.quote) {
        val next = charAt(cursor + 1)
        if (next == scheme.quote) {
          // accept escaped quote
          result.append(scheme.quote)
          cursor += 2
        } else {
          // accept closing quote
          cursor++
          break
        }
      } else {
        // accept content
        result.append(c)
        cursor++
      }
    }
    return ReadResult(result.toString(), cursor)
  }

  private fun readNonQuotedField(pos: Int): ReadResult<String>? {
    val firstChar = charAt(pos) ?: return null
    if (firstChar == scheme.quote) return null // not a non-quoted field

    var cursor = pos
    val result = StringBuilder()

    while (true) {
      val c = charAt(cursor) ?: break
      when (c) {
        scheme.quote -> throw DsvParseException("Unexpected quote in non-quoted field")
        scheme.delimiter,
        scheme.newline,
        scheme.carriageReturn -> break
        else -> result.append(c)
      }
      cursor++
    }

    return ReadResult(result.toString(), cursor)
  }

  private fun readNonEmptyField(pos: Int): ReadResult<String>? =
    readQuotedField(pos) ?: readNonQuotedField(pos)

  private fun readRecord(pos: Int): ReadResult<List<String>>? {
    val (firstField, newPos) = readNonEmptyField(pos) ?: return null

    var cursor = newPos
    val fields = mutableListOf(firstField)

    while (true) {
      val c = charAt(cursor) ?: break
      when (c) {
        scheme.carriageReturn,
        scheme.newline -> break
        scheme.delimiter -> {
          cursor++
          val fieldResult = readNonEmptyField(cursor) ?: ReadResult("", cursor)
          fields.add(fieldResult.value)
          cursor = fieldResult.newPos
        }
        else -> throw DsvParseException("Expected delimiter or end of line, got $c")
      }
    }

    return ReadResult(fields, cursor)
  }

  private fun readEndOfLine(pos: Int): ReadResult<Unit>? {
    val c = charAt(pos) ?: return ReadResult(Unit, pos)
    return when (c) {
      scheme.newline -> ReadResult(Unit, pos + 1)
      scheme.carriageReturn -> {
        if (charAt(pos + 1) == scheme.newline) ReadResult(Unit, pos + 2)
        else ReadResult(Unit, pos + 1)
      }

      else -> null
    }
  }

  /**
   * Parses all records from the input as a sequence of string lists.
   *
   * Each list represents one record (row). All records must have the same number of fields.
   */
  public fun parseRecords(): Sequence<List<String>> = sequence {
    input.use {
      val (firstRecord, pos) = readRecord(0) ?: return@use
      var cursor =
        readEndOfLine(pos)?.newPos
          ?: throw DsvParseException("Expected end of line, got '${charAt(pos)}'")

      data = StringBuilder(data.drop(cursor))
      cursor = 0

      val numColumns = firstRecord.size
      yield(firstRecord)

      while (true) {
        val (record, newPos) = readRecord(cursor) ?: break
        if (record.size != numColumns) {
          throw DsvParseException(
            "Expected $numColumns columns, got ${record.size} in record $record"
          )
        }

        cursor =
          readEndOfLine(newPos)?.newPos
            ?: throw DsvParseException("Expected end of line, got '${charAt(newPos)}'")
        data = StringBuilder(data.drop(cursor))
        cursor = 0

        yield(record)
      }

      if (cursor < data.length || !input.exhausted()) {
        throw DsvParseException("Unexpected data at end of input")
      }
    }
  }

  /**
   * Parses the input as a [DsvTable], treating the first record as a header row.
   *
   * @throws DsvParseException if the input is empty or malformed.
   */
  public fun parseTable(): DsvTable {
    val records = parseRecords().iterator()
    if (!records.hasNext()) throw DsvParseException("Expected a header")
    val header = records.next()
    return DsvTable(header, records.asSequence())
  }
}
