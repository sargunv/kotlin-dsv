package dev.sargunv.kotlindsv

import kotlin.jvm.JvmOverloads

/**
 * Separates records in a [DSV][DsvFormat] document.
 *
 * [write] is emitted after every record. [read] is the set of strings that terminate a record when
 * parsing. Matching uses the longest [read] string at the current position.
 *
 * [Lf] and [CrLf] write that newline and read LF, CRLF, and CRCRLF. A single-string constructor
 * such as `RecordDelimiter("\n%%\n")` reads and writes that string exactly.
 *
 * @property write String written after each record.
 * @property read Strings accepted as a record terminator when reading.
 */
public class RecordDelimiter
@JvmOverloads
constructor(write: String, read: List<String> = listOf(write)) {
  public val write: String = write
  public val read: List<String> = read.toList()

  internal val tokens: List<String>
  internal val heads: Set<Char>

  init {
    require(write.isNotEmpty()) { "Record delimiter write string must not be empty" }
    require(this.read.isNotEmpty()) { "Record delimiter must accept at least one read string" }
    require(this.read.all { it.isNotEmpty() }) { "Record delimiter read strings must not be empty" }
    tokens = this.read.distinct().sortedByDescending { it.length }
    heads = tokens.mapTo(mutableSetOf()) { it.first() }
  }

  internal fun matchLength(charAt: (Int) -> Char?, pos: Int): Int? {
    if (charAt(pos) !in heads) return null
    for (token in tokens) {
      if (matchesLiteral(charAt, pos, token)) return token.length
    }
    return null
  }

  override fun equals(other: Any?): Boolean =
    other is RecordDelimiter && write == other.write && tokens.toSet() == other.tokens.toSet()

  override fun hashCode(): Int = 31 * write.hashCode() + tokens.toSet().hashCode()

  override fun toString(): String = "RecordDelimiter(write=$write, read=$read)"

  internal fun strings(): Sequence<String> = sequence {
    yield(write)
    yieldAll(read)
  }

  /** Built-in newline delimiters. */
  public companion object {
    private val conventionalNewlines: List<String> = listOf("\r\r\n", "\r\n", "\n")

    /** Writes LF. Reading accepts LF, CRLF, and CRCRLF. */
    public val Lf: RecordDelimiter = RecordDelimiter(write = "\n", read = conventionalNewlines)

    /** Writes CRLF. Reading accepts LF, CRLF, and CRCRLF. */
    public val CrLf: RecordDelimiter = RecordDelimiter(write = "\r\n", read = conventionalNewlines)
  }
}

private fun matchesLiteral(charAt: (Int) -> Char?, pos: Int, value: String): Boolean {
  for (i in value.indices) {
    if (charAt(pos + i) != value[i]) return false
  }
  return true
}
