package dev.sargunv.kotlindsv

public data class DsvEncoding(
  val delimiter: Char,
  val quote: Char = '"',
  val writeCrlf: Boolean = false,
) {
  // line delimiters prob shouldn't be configurable
  public val newline: Char = '\n'
  public val carriageReturn: Char = '\r'

  init {
    require(quote != delimiter) { "Quote and delimiter must be different characters" }
    require(quote != newline && quote != carriageReturn) { "Quote must not be a newline character" }
    require(delimiter != newline && delimiter != carriageReturn) {
      "Delimiter must not be a newline character"
    }
  }
}
