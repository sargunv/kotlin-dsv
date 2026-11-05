package dev.sargunv.kotlindsv

/** Exception thrown when [DSV][DsvFormat] parsing fails due to malformed input. */
public class DsvParseException(message: String) : DsvException(message)
