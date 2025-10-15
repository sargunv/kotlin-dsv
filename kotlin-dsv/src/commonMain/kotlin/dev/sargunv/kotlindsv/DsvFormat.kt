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

/**
 * Configuration and entrypoint for encoding and decoding delimiter-separated values (DSV) data.
 *
 * This format supports serialization and deserialization of lists of Kotlin objects to/from DSV
 * formats like [CSV][Csv] and [TSV][Tsv] using [kotlinx.serialization][SerializationStrategy]. It
 * can work with strings or [Source]/[Sink] streams.
 *
 * ## Supported Types
 *
 * The format works with lists of flat data classes. Supported property types include:
 * - Primitives: [Int], [Long], [Short], [Byte], [Double], [Float], [Boolean], [Char], [String]
 * - Enums (serialized by name or ordinal)
 * - Nullable versions of the above types
 * - Any property with a serializer that serializes like one of the above (e.g., delegating
 *   serializers)
 *
 * ## Restrictions
 * - The top-level type must be a [List] of serializable objects
 * - Data classes must be flat (no nested objects, lists, or maps as properties)
 * - List elements must be serializable data classes, not primitives, maps, or lists
 *
 * @property scheme The [DsvScheme] defining delimiters and quoting rules.
 * @property serializersModule Custom serializers module for the format.
 * @property namingStrategy Strategy for transforming property names to/from column names.
 * @property treatMissingColumnsAsNull When true, missing columns are treated as null values instead
 *   of throwing an error.
 * @property ignoreUnknownKeys When true, unknown columns in the input are ignored instead of
 *   throwing an error.
 * @property writeEnumsByName When true, enums are written using their name; otherwise their ordinal
 *   is used.
 */
public open class DsvFormat(
  public val scheme: DsvScheme,
  public override val serializersModule: SerializersModule = EmptySerializersModule(),
  public val namingStrategy: DsvNamingStrategy = DsvNamingStrategy.Identity,
  public val treatMissingColumnsAsNull: Boolean = false,
  public val ignoreUnknownKeys: Boolean = false,
  public val writeEnumsByName: Boolean = true,
) : StringFormat {

  /** Decodes a value of type [T] from the given DSV string. */
  public inline fun <reified T> decodeFromString(@Language("csv") string: String): T =
    decodeFromString(serializersModule.serializer(), string)

  /** Encodes the given [value] to a DSV string. */
  public inline fun <reified T> encodeToString(value: T): String =
    encodeToString(serializersModule.serializer(), value)

  override fun <T> decodeFromString(
    deserializer: DeserializationStrategy<T>,
    @Language("csv") string: String,
  ): T = decodeFromSource(deserializer, Buffer().apply { writeString(string) })

  override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String =
    Buffer().also { encodeToSink(serializer, value, it) }.readString()

  /** Decodes a value of type [T] from the given [Source]. */
  public inline fun <reified T> decodeFromSource(source: Source): T =
    decodeFromSource(serializersModule.serializer(), source)

  /** Encodes the given [value] to the provided [Sink]. */
  public inline fun <reified T> encodeToSink(value: T, sink: Sink): Unit =
    encodeToSink(serializersModule.serializer(), value, sink)

  /** Decodes a value from the given [Source] using the specified [deserializer]. */
  public fun <T> decodeFromSource(deserializer: DeserializationStrategy<T>, source: Source): T =
    deserializer.deserialize(DsvDecoder(source, this))

  /** Encodes the given [value] to the provided [Sink] using the specified [serializer]. */
  public fun <T> encodeToSink(serializer: SerializationStrategy<T>, value: T, sink: Sink): Unit =
    serializer.serialize(DsvEncoder(sink, this), value)
}
