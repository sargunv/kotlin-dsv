package dev.sargunv.kotlindsv

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.intellij.lang.annotations.Language

public open class DsvFormat(
  public val encoding: DsvEncoding,
  public override val serializersModule: SerializersModule = EmptySerializersModule(),
  public val namingStrategy: DsvNamingStrategy = DsvNamingStrategy.Identity,
  public val treatMissingColumnsAsNull: Boolean = false,
  public val ignoreUnknownKeys: Boolean = false,
  public val writeEnumsByName: Boolean = true,
) : StringFormat {

  public inline fun <reified T> decodeFromString(@Language("csv") string: String): T {
    return decodeFromString(serializersModule.serializer(), string)
  }

  public inline fun <reified T> encodeToString(value: T): String {
    return encodeToString(serializersModule.serializer(), value)
  }

  override fun <T> decodeFromString(
    deserializer: DeserializationStrategy<T>,
    @Language("csv") string: String,
  ): T {
    return decodeFromSource(deserializer, Buffer().apply { writeString(string) })
  }

  override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
    return Buffer().also { encodeToSink(serializer, value, it) }.readString()
  }

  public inline fun <reified T> decodeFromSource(source: Source): T {
    return decodeFromSource(serializersModule.serializer(), source)
  }

  public inline fun <reified T> encodeToSink(value: T, sink: Sink) {
    return encodeToSink(serializersModule.serializer(), value, sink)
  }

  public fun <T> decodeFromSource(deserializer: DeserializationStrategy<T>, source: Source): T {
    return deserializer.deserialize(DsvDecoder(source, this))
  }

  public fun <T> encodeToSink(serializer: SerializationStrategy<T>, value: T, sink: Sink) {
    serializer.serialize(DsvEncoder(sink, this), value)
  }
}
