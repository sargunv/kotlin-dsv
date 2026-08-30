package dev.sargunv.kotlindsv

/**
 * Separates records in a [DSV][DsvFormat] document.
 *
 * [Lf] and [CrLf] write that newline and, when reading, accept LF, CRLF, and extra CR bytes before
 * LF (including CRCRLF). [Exact] writes and reads one specific string, including multi-character
 * warehouse separators such as `\n%%\n`.
 */
public sealed class RecordDelimiter {
  /** String written after each record. */
  public abstract val value: String

  /** Writes LF. Reading accepts conventional newline forms. */
  public data object Lf : RecordDelimiter() {
    override val value: String = "\n"
  }

  /** Writes CRLF. Reading accepts conventional newline forms. */
  public data object CrLf : RecordDelimiter() {
    override val value: String = "\r\n"
  }

  /**
   * Writes and reads [value] exactly.
   *
   * @throws IllegalArgumentException if [value] is empty.
   */
  public data class Exact(override val value: String) : RecordDelimiter() {
    init {
      require(value.isNotEmpty()) { "Record delimiter must not be empty" }
    }
  }
}
