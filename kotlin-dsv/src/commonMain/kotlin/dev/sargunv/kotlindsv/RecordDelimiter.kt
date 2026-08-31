package dev.sargunv.kotlindsv

import kotlin.jvm.JvmOverloads

/**
 * Separates records in a [DSV][DsvFormat] document.
 *
 * [write] is emitted after every record. [read] is the set of strings that terminate a record when
 * parsing. Matching uses the longest [read] string at the current position.
 *
 * [Lf] and [CrLf] write that newline and, when reading, accept `\r*\n` (any number of CR bytes
 * before LF, including CRCRLF). A single-string constructor such as `RecordDelimiter("\n%%\n")`
 * reads and writes that string exactly.
 *
 * @property write String written after each record.
 * @property read Strings accepted as a record terminator when reading.
 */
public class RecordDelimiter
private constructor(
  write: String,
  read: List<String>,
  private val acceptCrStarLf: Boolean,
) {
  @JvmOverloads
  public constructor(
    write: String,
    read: List<String> = listOf(write),
  ) : this(write, read, acceptCrStarLf = false)

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
    if (acceptCrStarLf) return matchCrStarLf(charAt, pos)
    if (charAt(pos) !in heads) return null
    for (token in tokens) {
      if (matchesLiteral(charAt, pos, token)) return token.length
    }
    return null
  }

  override fun equals(other: Any?): Boolean =
    other is RecordDelimiter &&
      write == other.write &&
      tokens.toSet() == other.tokens.toSet() &&
      acceptCrStarLf == other.acceptCrStarLf

  override fun hashCode(): Int =
    31 * (31 * write.hashCode() + tokens.toSet().hashCode()) + acceptCrStarLf.hashCode()

  override fun toString(): String =
    if (acceptCrStarLf) "RecordDelimiter(write=$write, read=\\r*\\n)"
    else "RecordDelimiter(write=$write, read=$read)"

  internal fun strings(): Sequence<String> = sequence {
    yield(write)
    yieldAll(read)
    if (acceptCrStarLf) yield("\r")
  }

  /** Built-in newline delimiters. */
  public companion object {
    private val lfAndCrlf: List<String> = listOf("\r\n", "\n")

    /** Writes LF. Reading accepts `\r*\n`. */
    public val Lf: RecordDelimiter = RecordDelimiter("\n", lfAndCrlf, acceptCrStarLf = true)

    /** Writes CRLF. Reading accepts `\r*\n`. */
    public val CrLf: RecordDelimiter = RecordDelimiter("\r\n", lfAndCrlf, acceptCrStarLf = true)
  }
}

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
