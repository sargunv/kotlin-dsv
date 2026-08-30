package dev.sargunv.kotlindsv

/** How to treat a row with fewer fields than expected. */
public enum class ShortRowPolicy {
  /** Throw [DsvParseException]. */
  Reject,

  /** Omit the row. */
  Skip,

  /** Append empty strings until the row matches the expected width. */
  Pad,
}
