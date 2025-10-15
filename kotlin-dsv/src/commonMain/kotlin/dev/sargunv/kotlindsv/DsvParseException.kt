package dev.sargunv.kotlindsv

import kotlinx.serialization.SerializationException

/** Exception thrown when [DSV][DsvFormat] parsing fails due to malformed input. */
public class DsvParseException(message: String) : SerializationException(message)
