package dev.sargunv.kotlindsv

/** How to treat a row whose field count differs from the expected width. */
public enum class JaggedRowPolicy {
  /** Throw [DsvParseException]. */
  Reject,

  /** Omit the row. */
  Skip,

  /**
   * Pad a short row with empty strings, or drop extra fields from a long row, so it matches the
   * expected width.
   */
  Normalize,
}
