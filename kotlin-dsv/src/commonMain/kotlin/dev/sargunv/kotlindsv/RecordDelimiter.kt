package dev.sargunv.kotlindsv

/**
 * Separates records in a [DSV][DsvFormat] document.
 *
 * [Lf] and [CrLf] write that newline and, when reading, accept `\r*\n` (any number of CR bytes
 * before LF, including CRCRLF). [Exact] writes and reads one specific string.
 */
public sealed interface RecordDelimiter {
  /** String written after each record. */
  public val value: String

  /** Writes LF. Reading accepts `\r*\n`. */
  public data object Lf : RecordDelimiter {
    override val value: String = "\n"
  }

  /** Writes CRLF. Reading accepts `\r*\n`. */
  public data object CrLf : RecordDelimiter {
    override val value: String = "\r\n"
  }

  /**
   * Writes and reads [value] exactly.
   *
   * @throws IllegalArgumentException if [value] is empty or starts with U+FEFF.
   */
  public data class Exact(override val value: String) : RecordDelimiter {
    init {
      require(value.isNotEmpty()) { "Record delimiter must not be empty" }
      require(value.first() != UTF8_BOM) { "Record delimiter must not start with a BOM" }
    }
  }
}

internal fun RecordDelimiter.matchLength(charAt: (Int) -> Char?, pos: Int): Int? =
  when (this) {
    RecordDelimiter.Lf,
    RecordDelimiter.CrLf -> matchCrStarLf(charAt, pos)
    is RecordDelimiter.Exact -> if (matchesLiteral(charAt, pos, value)) value.length else null
  }

internal fun RecordDelimiter.quoteNeedles(): Sequence<String> =
  when (this) {
    RecordDelimiter.Lf,
    RecordDelimiter.CrLf -> sequenceOf("\n", "\r")
    is RecordDelimiter.Exact -> sequenceOf(value)
  }

internal fun RecordDelimiter.suffixOverlapsWriteValue(field: String): Boolean =
  (field + value).indexOf(value) < field.length

internal const val UTF8_BOM = '\uFEFF'

private fun matchCrStarLf(charAt: (Int) -> Char?, pos: Int): Int? {
  var cursor = pos
  var consumed = 0
  var c = charAt(cursor) ?: return null

  while (true) {
    when (c) {
      '\n' -> return consumed + 1
      '\r' -> {
        consumed += 1
        cursor += 1
      }
      else -> return null
    }
    c = charAt(cursor) ?: return consumed
  }
}

private fun matchesLiteral(charAt: (Int) -> Char?, pos: Int, value: String): Boolean {
  for (i in value.indices) {
    if (charAt(pos + i) != value[i]) return false
  }
  return true
}
