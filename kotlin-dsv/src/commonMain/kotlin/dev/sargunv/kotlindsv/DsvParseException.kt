package dev.sargunv.kotlindsv

import kotlinx.serialization.SerializationException

public class DsvParseException(message: String) : SerializationException(message)
