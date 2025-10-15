package dev.sargunv.kotlindsv

public object Csv : DsvFormat(encoding = DsvEncoding(quote = '"', delimiter = ','))

public object Tsv : DsvFormat(DsvEncoding(quote = '"', delimiter = '\t'))
