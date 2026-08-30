package dev.sargunv.kotlindsv

/** How to treat a row with more fields than expected. */
public enum class LongRowPolicy {
  /** Throw [DsvParseException]. */
  Reject,

  /** Omit the row. */
  Skip,

  /** Drop extra fields. */
  Truncate,
}
