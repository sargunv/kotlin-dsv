package dev.sargunv.kotlindsv

/** Pre-configured CSV format with comma delimiter. */
public object Csv : DsvFormat(scheme = DsvScheme(delimiter = ','))

/** Pre-configured TSV format with tab delimiter. */
public object Tsv : DsvFormat(scheme = DsvScheme(delimiter = '\t'))
