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

/**
 * Pre-configured Psv format with pipe delimiter.
 *
 * @see [DsvFormat]
 */
public object Psv : DsvFormat(scheme = DsvScheme(delimiter = '|'))
