package dev.sargunv.kotlindsv

import kotlinx.io.Source
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME

/**
 * Internal decoder for lazily decoding DSV records into a sequence of objects.
 *
 * Unlike [DsvDecoder], this decoder works on individual records rather than the entire list,
 * allowing for lazy evaluation.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DsvSequenceDecoder(
  source: Source,
  private val format: DsvFormat,
) {
  private val parser = DsvParser(source, format.scheme)
  private val table = parser.parseTable()
  private val originalHeader: List<String> = table.header
  private val mappedHeader: List<String> =
    table.header.map { format.namingStrategy.fromDsvName(it.trim()) }
  private val records: Iterator<List<String>> = table.records.iterator()

  private lateinit var recordDescriptor: SerialDescriptor
  private lateinit var nullHeaders: List<String>

  fun <T> decodeSequence(deserializer: kotlinx.serialization.DeserializationStrategy<T>): Sequence<T> =
    sequence {
      // Initialize the record descriptor from the first decode
      if (!::recordDescriptor.isInitialized) {
        recordDescriptor = deserializer.descriptor
        require(recordDescriptor.kind == StructureKind.CLASS) {
          "Element type must be a class (got ${recordDescriptor.kind})"
        }

        nullHeaders =
          if (format.treatMissingColumnsAsNull) {
            val originalHeaderSet = originalHeader.toSet()
            recordDescriptor.elementNames.filter { fieldName ->
              !recordDescriptor.isElementOptional(recordDescriptor.getElementIndex(fieldName)) &&
                !originalHeaderSet.contains(format.namingStrategy.toDsvName(fieldName))
            }
          } else {
            emptyList()
          }
      }

      while (records.hasNext()) {
        val record = records.next()
        yield(
          deserializer.deserialize(
            RecordDecoder(record, originalHeader, mappedHeader, nullHeaders, format, recordDescriptor)
          )
        )
      }
    }

  /**
   * Decoder for a single DSV record.
   */
  private class RecordDecoder(
    private val record: List<String>,
    private val originalHeader: List<String>,
    private val mappedHeader: List<String>,
    private val nullHeaders: List<String>,
    private val format: DsvFormat,
    private val recordDescriptor: SerialDescriptor,
  ) : AbstractDecoder() {

    override val serializersModule = format.serializersModule

    private var col = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
      require(descriptor === recordDescriptor) {
        "All records must have the same structure (expected $recordDescriptor, got $descriptor)"
      }
      return this
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
      var ret: Int
      do {
        ret =
          when {
            // end of record
            col >= record.size + nullHeaders.size -> CompositeDecoder.DECODE_DONE

            // implicit null
            col >= record.size -> descriptor.getElementIndex(nullHeaders[col - record.size])

            // regular element
            else -> descriptor.getElementIndex(mappedHeader[col])
          }
        col++
      } while (
        ret == UNKNOWN_NAME &&
          (format.ignoreUnknownKeys ||
            throw SerializationException("Unknown column '${originalHeader[col - 1]}'"))
      )
      col--
      return ret
    }

    override fun decodeValue(): Any {
      val ret = record[col]
      col++
      return ret
    }

    override fun decodeString(): String = decodeValue() as String

    override fun decodeNotNullMark(): Boolean =
      if (col >= record.size) false // implicit null
      else record[col] != ""

    override fun decodeNull(): Nothing? {
      if (col >= record.size) {
        // implicit null
        col++
        return null
      }
      val value = decodeString()
      if (value.isEmpty()) return null
      throw SerializationException("Expected null, but got '$value'")
    }

    override fun decodeBoolean(): Boolean = decodeString().toBoolean()

    override fun decodeByte(): Byte = decodeString().toByte()

    override fun decodeShort(): Short = decodeString().toShort()

    override fun decodeInt(): Int = decodeString().toInt()

    override fun decodeLong(): Long = decodeString().toLong()

    override fun decodeFloat(): Float = decodeString().toFloat()

    override fun decodeDouble(): Double = decodeString().toDouble()

    override fun decodeChar(): Char {
      val str = decodeString()
      require(str.length == 1) { "Expected Char, but got '$str'" }
      return str[0]
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
      val value = decodeString()
      val index = enumDescriptor.elementNames.indexOf(value)
      if (index >= 0) return index

      val ordinal =
        try {
          value.toInt()
        } catch (_: NumberFormatException) {
          throw IllegalArgumentException("Enum value '$value' not found in $enumDescriptor")
        }

      if (ordinal < 0 || ordinal >= enumDescriptor.elementsCount) {
        throw IllegalArgumentException("Enum ordinal $ordinal not found in $enumDescriptor")
      }

      return ordinal
    }
  }
}
