package dev.sargunv.kotlindsv

/** How to treat a row with fewer fields than expected. */
public enum class ShortRowPolicy {
  /** Throw [DsvParseException]. */
  Reject,

  /** Omit the row. */
  Skip,

  /** Pad with empty strings. */
  Pad,
}
