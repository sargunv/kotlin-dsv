package dev.sargunv.kotlindsv

import kotlin.jvm.JvmOverloads

/**
 * Defines the delimiters and quoting rules for a [DsvFormat].
 *
 * @property delimiter The character used to separate fields (e.g., ',' for CSV, '\t' for TSV).
 * @property quote The character used to quote fields containing special characters.
 * @property recordDelimiter How records are terminated when reading and writing.
 * @property skipEmptyLines When true, empty lines in the input are skipped during parsing.
 * @property shortRowPolicy How later rows with fewer fields than the first kept row are handled.
 * @property longRowPolicy How later rows with more fields than the first kept row are handled.
 */
public data class DsvScheme
@JvmOverloads
constructor(
  internal val delimiter: Char,
  internal val quote: Char = '"',
  internal val recordDelimiter: RecordDelimiter = RecordDelimiter.CrLf,
  internal val skipEmptyLines: Boolean = false,
  internal val shortRowPolicy: ShortRowPolicy = ShortRowPolicy.Reject,
  internal val longRowPolicy: LongRowPolicy = LongRowPolicy.Reject,
) {
  init {
    require(quote != delimiter) { "Quote and delimiter must be different characters" }
    require(quote !in recordDelimiter.value) { "Quote must not appear in the record delimiter" }
    require(delimiter !in recordDelimiter.value) {
      "Delimiter must not appear in the record delimiter"
    }
  }
}
