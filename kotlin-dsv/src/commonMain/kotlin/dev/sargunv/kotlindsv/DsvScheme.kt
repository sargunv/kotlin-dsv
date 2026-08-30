package dev.sargunv.kotlindsv

import kotlin.jvm.JvmOverloads

/**
 * Defines the delimiters and quoting rules for a [DsvFormat].
 *
 * @property delimiter The character used to separate fields (e.g., ',' for CSV, '\t' for TSV).
 * @property quote The character used to quote fields containing special characters.
 * @property writeCrlf When true, writes CRLF line endings; otherwise uses LF.
 * @property skipEmptyLines When true, empty lines in the input are skipped during parsing.
 * @property jaggedRowPolicy How later rows with a different field count than the first kept row are
 *   handled. Defaults to [JaggedRowPolicy.Reject]. The first row that is not dropped by
 *   [skipEmptyLines] is always kept and becomes the expected width.
 */
public data class DsvScheme
@JvmOverloads
constructor(
  internal val delimiter: Char,
  internal val quote: Char = '"',
  internal val writeCrlf: Boolean = true,
  internal val skipEmptyLines: Boolean = false,
  internal val jaggedRowPolicy: JaggedRowPolicy = JaggedRowPolicy.Reject,
) {
  // line delimiters prob shouldn't be configurable
  internal val lineFeed: Char = '\n'
  internal val carriageReturn: Char = '\r'

  init {
    require(quote != delimiter) { "Quote and delimiter must be different characters" }
    require(quote != lineFeed && quote != carriageReturn) {
      "Quote must not be a newline character"
    }
    require(delimiter != lineFeed && delimiter != carriageReturn) {
      "Delimiter must not be a newline character"
    }
  }
}
