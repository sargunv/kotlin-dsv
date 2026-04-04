package dev.sargunv.kotlindsv

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
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
) : SerialFormat {

  /** Encodes the given [value] to a DSV string. */
  public inline fun <reified T> encodeToString(value: List<T>): String =
    encodeToString(serializersModule.serializer(), value)

  /** Decodes a value of type [T] from the given DSV string. */
  public inline fun <reified T> decodeFromString(@Language("csv") string: String): List<T> =
    decodeFromString(serializersModule.serializer(), string)

  /** Encodes the given [value] to a DSV string using the specified [serializer]. */
  public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: List<T>): String {
    val sink = Buffer()
    encodeToSink(serializer, value.asSequence(), sink)
    return sink.readString()
  }

  /** Decodes a value of type [T] from the given DSV string using the specified [serializer]. */
  public fun <T> decodeFromString(
    deserializer: DeserializationStrategy<T>,
    @Language("csv") string: String,
  ): List<T> {
    val source = Buffer()
    source.writeString(string)
    return decodeFromSource(source, deserializer).toList()
  }

  /**
   * Encodes the given [sequence] to the provided [sink] as UTF-8 text using the specified
   * [serializer].
   */
  public inline fun <reified T> encodeToSink(sequence: Sequence<T>, sink: Sink) {
    encodeToSink(serializersModule.serializer(), sequence, sink)
  }

  /**
   * Transforms the given UTF-8 [source] into lazily deserialized [Sequence] of elements of type
   * [T]. The resulting sequence is tied to the [source] and can be evaluated only once.
   */
  public inline fun <reified T> decodeFromSource(source: Source): Sequence<T> =
    decodeFromSource(source, serializersModule.serializer())

  /**
   * Encodes the given [sequence] to the provided [sink] as UTF-8 text using the specified
   * [serializer].
   */
  public fun <T> encodeToSink(
    serializer: SerializationStrategy<T>,
    sequence: Sequence<T>,
    sink: Sink,
  ) {
    val descriptor = serializer.descriptor
    require(descriptor.kind == StructureKind.CLASS) {
      "Element type must be a class (got ${descriptor.kind})"
    }

    val writer = DsvWriter(sink, scheme)
    val header = descriptor.elementNames.map(namingStrategy::toDsvName)
    writer.writeRecord(header)

    val encoder = DsvEncoder(this, writer, descriptor)
    sequence.forEach { record -> serializer.serialize(encoder, record) }
  }

  /**
   * Transforms the given UTF-8 [source] into lazily deserialized [Sequence] of elements of type
   * [T]. The resulting sequence is tied to the [source] and can be evaluated only once.
   */
  public fun <T> decodeFromSource(
    source: Source,
    deserializer: DeserializationStrategy<T>,
  ): Sequence<T> {
    val descriptor = deserializer.descriptor
    require(descriptor.kind == StructureKind.CLASS) {
      "Element type must be a class (got ${descriptor.kind})"
    }

    val parser = DsvParser(source, scheme)
    val table = parser.parseTable()

    val decoder = DsvDecoder(this, table, descriptor)

    return sequence {
      while (decoder.hasNext()) {
        yield(deserializer.deserialize(decoder))
      }
    }
  }
}
