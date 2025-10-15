package dev.sargunv.kotlindsv

public object Csv : DsvFormat(scheme = DsvScheme(delimiter = ','))

public object Tsv : DsvFormat(scheme = DsvScheme(delimiter = '\t'))
