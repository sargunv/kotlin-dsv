package dev.sargunv.kotlindsv

/**
 * Defines the delimiters and quoting rules for a [DsvFormat].
 *
 * @property delimiter The character used to separate fields (e.g., ',' for CSV, '\t' for TSV).
 * @property quote The character used to quote fields containing special characters.
 * @property writeCrlf When true, writes CRLF line endings; otherwise uses LF.
 */
public data class DsvScheme(
  internal val delimiter: Char,
  internal val quote: Char = '"',
  internal val writeCrlf: Boolean = false,
) {
  // line delimiters prob shouldn't be configurable
  internal val newline: Char = '\n'
  internal val carriageReturn: Char = '\r'

  init {
    require(quote != delimiter) { "Quote and delimiter must be different characters" }
    require(quote != newline && quote != carriageReturn) { "Quote must not be a newline character" }
    require(delimiter != newline && delimiter != carriageReturn) {
      "Delimiter must not be a newline character"
    }
  }
}
