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

      for (numExcludedBytes in 0..MAX_UTF8_INCOMPLETE_BYTES) {
        val decoded =
          try {
            buffer.decodeToString(
              0,
              numBytesInBuffer - numExcludedBytes,
              throwOnInvalidSequence = numExcludedBytes != MAX_UTF8_INCOMPLETE_BYTES,
            )
          } catch (e: CharacterCodingException) {
            if (numExcludedBytes == MAX_UTF8_INCOMPLETE_BYTES) throw e else continue
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
      when {
        c == scheme.quote -> throw DsvParseException("Unexpected quote in non-quoted field")
        c == scheme.delimiter || atRecordBoundary(cursor) -> break
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
      when {
        atRecordBoundary(cursor) -> break
        c == scheme.delimiter -> {
          cursor++
          val fieldResult = readNonEmptyField(cursor) ?: ReadResult("", cursor)
          fields.add(fieldResult.value)
          cursor = fieldResult.newPos
        }
        else -> throw DsvParseException("Expected delimiter or end of record, got $c")
      }
    }

    return ReadResult(fields, cursor)
  }

  private fun atRecordBoundary(pos: Int): Boolean {
    val c = charAt(pos) ?: return false
    return when (val delimiter = scheme.recordDelimiter) {
      RecordDelimiter.Lf,
      RecordDelimiter.CrLf -> c == '\n' || c == '\r'
      is RecordDelimiter.Exact -> matchesLiteral(pos, delimiter.value)
    }
  }

  private fun matchesLiteral(pos: Int, value: String): Boolean {
    for (i in value.indices) {
      if (charAt(pos + i) != value[i]) return false
    }
    return true
  }

  private fun readEndOfRecord(pos: Int): ReadResult<Unit>? {
    if (charAt(pos) == null) return ReadResult(Unit, pos)
    return when (val delimiter = scheme.recordDelimiter) {
      RecordDelimiter.Lf,
      RecordDelimiter.CrLf -> readConventionalNewline(pos)
      is RecordDelimiter.Exact -> {
        if (!matchesLiteral(pos, delimiter.value)) null
        else ReadResult(Unit, pos + delimiter.value.length)
      }
    }
  }

  private fun readConventionalNewline(pos: Int): ReadResult<Unit>? {
    var cursor = pos
    var c = charAt(cursor) ?: return ReadResult(Unit, cursor)

    while (true) {
      // eat: \r*\n
      // because CRCRLF is apparently a thing
      when (c) {
        '\n' -> return ReadResult(Unit, cursor + 1)
        '\r' -> cursor += 1
        else -> return null
      }
      c = charAt(cursor) ?: return ReadResult(Unit, cursor)
    }
  }

  /**
   * Parses all records from the input as a sequence of string lists.
   *
   * Each list represents one record (row). Later short and long rows follow
   * [DsvScheme.shortRowPolicy] and [DsvScheme.longRowPolicy]. When [DsvScheme.skipEmptyLines] is
   * true, empty lines are dropped before the expected width is chosen.
   */
  public fun parseRecords(): Sequence<List<String>> = sequence {
    input.use {
      // UTF-8 BOM is an encoding prefix, not part of the first field
      var cursor = if (charAt(0) == UTF8_BOM) 1 else 0
      var numColumns: Int? = null

      while (true) {
        val (record, newPos) = readRecord(cursor) ?: break

        cursor =
          readEndOfRecord(newPos)?.newPos
            ?: throw DsvParseException("Expected end of record, got '${charAt(newPos)}'")
        data = StringBuilder(data.drop(cursor))
        cursor = 0

        if (scheme.skipEmptyLines && isEmptyRecord(record)) continue

        val expectedColumns = numColumns
        if (expectedColumns == null) {
          numColumns = record.size
          yield(record)
          continue
        }

        if (record.size == expectedColumns) {
          yield(record)
          continue
        }

        if (record.size < expectedColumns) {
          when (scheme.shortRowPolicy) {
            ShortRowPolicy.Reject ->
              throw DsvParseException(
                "Expected $expectedColumns columns, got ${record.size} in record $record"
              )
            ShortRowPolicy.Skip -> continue
            ShortRowPolicy.Pad -> yield(record + List(expectedColumns - record.size) { "" })
          }
        } else {
          when (scheme.longRowPolicy) {
            LongRowPolicy.Reject ->
              throw DsvParseException(
                "Expected $expectedColumns columns, got ${record.size} in record $record"
              )
            LongRowPolicy.Skip -> continue
            LongRowPolicy.Truncate -> yield(record.take(expectedColumns))
          }
        }
      }

      if (cursor < data.length || !input.exhausted()) {
        throw DsvParseException("Unexpected data at end of input")
      }
    }
  }

  private fun isEmptyRecord(record: List<String>): Boolean =
    record.isEmpty() || record.size == 1 && record[0].isEmpty()

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

  private companion object {
    private const val MAX_UTF8_INCOMPLETE_BYTES = 3
    private const val UTF8_BOM = '\uFEFF'
  }
}
