package dev.sargunv.kotlindsv

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
