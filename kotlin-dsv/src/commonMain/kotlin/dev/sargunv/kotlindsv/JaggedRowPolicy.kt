package dev.sargunv.kotlindsv

/**
 * How [DsvParser] treats later rows whose field count differs from the first kept row.
 *
 * The first row that is not dropped by [DsvScheme.skipEmptyLines] is always kept and becomes the
 * canonical width. This policy applies only to rows after that.
 */
public enum class JaggedRowPolicy {
  /**
   * Throw [DsvParseException] when a later row has a different field count. This is the default.
   */
  Reject,

  /** Omit later rows whose field count differs from the first kept row. */
  Skip,

  /**
   * Force later rows to the first kept row's width: pad short rows with empty strings and drop
   * extra fields from long rows.
   */
  Normalize,
}
