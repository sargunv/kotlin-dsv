package dev.sargunv.kotlindsv

import kotlinx.io.Source

public class DsvParser(private val input: Source, private val encoding: DsvEncoding) {
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
    if (charAt(cursor) != encoding.quote) return null
    cursor++

    val result = StringBuilder()
    while (true) {
      // require content
      val c = charAt(cursor) ?: throw DsvParseException("Unterminated quoted value")
      if (c == encoding.quote) {
        val next = charAt(cursor + 1)
        if (next == encoding.quote) {
          // accept escaped quote
          result.append(encoding.quote)
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
    if (firstChar == encoding.quote) return null // not a non-quoted field

    var cursor = pos
    val result = StringBuilder()

    while (true) {
      val c = charAt(cursor) ?: break
      when (c) {
        encoding.quote -> throw DsvParseException("Unexpected quote in non-quoted field")
        encoding.delimiter,
        encoding.newline,
        encoding.carriageReturn -> break
        else -> result.append(c)
      }
      cursor++
    }

    return ReadResult(result.toString(), cursor)
  }

  private fun readNonEmptyField(pos: Int): ReadResult<String>? {
    return readQuotedField(pos) ?: readNonQuotedField(pos)
  }

  private fun readRecord(pos: Int): ReadResult<List<String>>? {
    val (firstField, newPos) = readNonEmptyField(pos) ?: return null

    var cursor = newPos
    val fields = mutableListOf(firstField)

    while (true) {
      val c = charAt(cursor) ?: break
      when (c) {
        encoding.carriageReturn,
        encoding.newline -> break
        encoding.delimiter -> {
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
      encoding.newline -> ReadResult(Unit, pos + 1)
      encoding.carriageReturn -> {
        if (charAt(pos + 1) == encoding.newline) ReadResult(Unit, pos + 2)
        else ReadResult(Unit, pos + 1)
      }

      else -> null
    }
  }

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

  public fun parseTable(): DsvTable {
    val records = parseRecords().iterator()
    if (!records.hasNext()) throw DsvParseException("Expected a header")
    val header = records.next()
    return DsvTable(header, records.asSequence())
  }
}
