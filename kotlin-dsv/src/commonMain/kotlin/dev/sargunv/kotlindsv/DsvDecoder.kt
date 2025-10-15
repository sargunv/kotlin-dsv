package dev.sargunv.kotlindsv

import kotlinx.io.Source
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder

@OptIn(ExperimentalSerializationApi::class)
internal class DsvDecoder(source: Source, private val format: DsvFormat) : AbstractDecoder() {

  private val originalHeader: List<String>
  private val mappedHeader: List<String>
  private val records: Iterator<List<String>>

  // current location of the decoder
  private var level = 0
  private var row = 0
  private var record: List<String>? = null
  private var col = 0

  // initialized on first record
  private lateinit var nullHeaders: List<String>
  private lateinit var recordDescriptor: SerialDescriptor

  init {
    val table = DsvParser(source, format.encoding).parseTable()
    originalHeader = table.header
    mappedHeader = table.header.map { format.namingStrategy.fromDsvName(it) }
    records = table.records.iterator()
    record = if (records.hasNext()) records.next() else null
  }

  override val serializersModule = format.serializersModule

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    return when (level) {
      0 -> {
        require(descriptor.kind == StructureKind.LIST) {
          "Top-level structure must be a list (got ${descriptor.kind})"
        }
        level++
        this
      }

      1 -> {
        require(descriptor.kind == StructureKind.CLASS) {
          "Second-level structure must be a class (got ${descriptor.kind})"
        }

        if (!::recordDescriptor.isInitialized) {
          recordDescriptor = descriptor
        } else {
          require(descriptor === recordDescriptor) {
            "All records must have the same structure (expected $recordDescriptor, got $descriptor)"
          }
        }

        if (!::nullHeaders.isInitialized) {
          nullHeaders =
            if (format.treatMissingColumnsAsNull) {
              val originalHeaderSet = originalHeader.toSet()
              descriptor.elementNames.filter { fieldName ->
                // fields required for decoding and not present in the CSV will be treated as null
                !descriptor.isElementOptional(descriptor.getElementIndex(fieldName)) &&
                  !originalHeaderSet.contains(format.namingStrategy.toDsvName(fieldName))
              }
            } else {
              emptyList()
            }
        }

        level++
        this
      }

      else -> throw SerializationException("Structures within fields are not supported")
    }
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    level--
    if (level < 0) throw SerializationException("Unbalanced structure")
  }

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    return when (level) {
      1 -> if (record == null) return CompositeDecoder.DECODE_DONE else row

      2 -> {
        var ret: Int
        do {
          ret =
            when {
              // end of row
              col >= record!!.size + nullHeaders.size -> {
                row++
                record = if (records.hasNext()) records.next() else null
                col = 0
                CompositeDecoder.DECODE_DONE
              }

              // implicit null
              col >= record!!.size -> descriptor.getElementIndex(nullHeaders[col - record!!.size])

              // regular element
              else -> descriptor.getElementIndex(mappedHeader[col])
            }
          col++
        } while (ret == CompositeDecoder.UNKNOWN_NAME && format.ignoreUnknownKeys)
        col--
        ret
      }

      else ->
        throw SerializationException("Fields must be within a list of objects (got level $level)")
    }
  }

  override fun decodeValue(): Any {
    require(level == 2) { "Fields must be within a list of objects (got level $level)" }
    val ret = record!![col]
    col++
    return ret
  }

  override fun decodeString(): String = decodeValue() as String

  override fun decodeNotNullMark(): Boolean {
    if (col >= record!!.size) return false // implicit null
    return record!![col] != ""
  }

  override fun decodeNull(): Nothing? {
    if (col >= record!!.size) {
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
