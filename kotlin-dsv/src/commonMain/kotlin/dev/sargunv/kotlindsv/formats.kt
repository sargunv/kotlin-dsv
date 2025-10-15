package dev.sargunv.kotlindsv

/**
 * Pre-configured CSV format with comma delimiter.
 *
 * @see [DsvFormat]
 */
public object Csv : DsvFormat(scheme = DsvScheme(delimiter = ','))

/**
 * Pre-configured TSV format with tab delimiter.
 *
 * @see [DsvFormat]
 */
public object Tsv : DsvFormat(scheme = DsvScheme(delimiter = '\t'))
