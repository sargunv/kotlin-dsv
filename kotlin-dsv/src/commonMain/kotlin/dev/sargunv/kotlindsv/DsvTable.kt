package dev.sargunv.kotlindsv

public data class DsvTable(val header: List<String>, val records: Sequence<List<String>>)
