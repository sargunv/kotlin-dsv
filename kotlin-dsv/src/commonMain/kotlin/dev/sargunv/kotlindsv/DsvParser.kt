package dev.sargunv.kotlindsv

import kotlinx.io.Source

/**
 * Low-level parser for [DSV][DsvFormat] data.
 *
 * Reads from a UTF-8 [Source] and parses according to the provided [DsvScheme]. For typical use
 * cases, prefer using [DsvFormat] instead.
 */
public class DsvParser(private val input: Source, private val scheme: DsvScheme) {
  private var data = StringBuilder()
  private val buffer = ByteArray(4096)
  private var incompleteByteCount = 0

  private data class ReadResult<T>(val value: T, val newPos: Int)

  private fun charAt(pos: Int): Char? {
    while (data.length <= pos) {
      if (input.exhausted()) {
        if (incompleteByteCount > 0) {
          val decoded = buffer.decodeToString(0, incompleteByteCount, throwOnInvalidSequence = true)
          data.append(decoded)
          incompleteByteCount = 0
        }
        return if (data.length <= pos) null else data[pos]
      }

      val numBytesRead =
        input.readAtMostTo(buffer, incompleteByteCount, buffer.size - incompleteByteCount)
      val numBytesInBuffer = incompleteByteCount + numBytesRead

      for (numExcludedBytes in 0..3) {
        val decoded =
          try {
            buffer.decodeToString(
              0,
              numBytesInBuffer - numExcludedBytes,
              throwOnInvalidSequence = numExcludedBytes != 3,
            )
          } catch (e: CharacterCodingException) {
            if (numExcludedBytes == 3) throw e else continue
          }
        incompleteByteCount = numExcludedBytes
        data.append(decoded)
        break
      }

      if (incompleteByteCount > 0)
        buffer.copyInto(buffer, 0, numBytesInBuffer - incompleteByteCount, numBytesInBuffer)
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
        scheme.lineFeed,
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
        scheme.lineFeed -> break
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
    var c = charAt(pos) ?: return ReadResult(Unit, pos)
    var pos = pos

    while (true) {
      // eat: \r*\n
      // because CRCRLF is apparently a thing
      when (c) {
        scheme.lineFeed -> return ReadResult(Unit, pos + 1)
        scheme.carriageReturn -> pos += 1
        else -> return null
      }
      c = charAt(pos) ?: return ReadResult(Unit, pos)
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

        cursor =
          readEndOfLine(newPos)?.newPos
            ?: throw DsvParseException("Expected end of line, got '${charAt(newPos)}'")
        data = StringBuilder(data.drop(cursor))
        cursor = 0

        if (scheme.skipEmptyLines && (record.isEmpty() || record.size == 1 && record[0].isEmpty()))
          continue

        if (record.size != numColumns) {
          throw DsvParseException(
            "Expected $numColumns columns, got ${record.size} in record $record"
          )
        }

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
