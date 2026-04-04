package dev.sargunv.kotlindsv

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder

@OptIn(ExperimentalSerializationApi::class)
internal class DsvDecoder(
  private val format: DsvFormat,
  private val table: DsvTable,
  descriptor: SerialDescriptor,
) : AbstractDecoder() {

  override val serializersModule = format.serializersModule

  private val records: Iterator<List<String>> = table.records.iterator()

  private val knownElementNames = table.header.map { format.namingStrategy.fromDsvName(it.trim()) }
  private val implicitNullElementNames: List<String> =
    if (format.treatMissingColumnsAsNull) {
      val knownElementNames = knownElementNames.toSet()
      descriptor.elementNames.filter { elementName ->
        !descriptor.isElementOptional(descriptor.getElementIndex(elementName)) &&
          !knownElementNames.contains(elementName)
      }
    } else emptyList()

  private var col = 0
  private lateinit var record: List<String>
  private var open = false

  internal fun hasNext(): Boolean = records.hasNext()

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    require(!open) { "Nested structures are not supported in DSV format" }
    open = true
    record = records.next()
    return this
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    require(open) { "No structure is open" }
    col = 0
    open = false
  }

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    var index: Int
    do {
      index =
        when {
          // end of record
          col >= record.size + implicitNullElementNames.size -> CompositeDecoder.DECODE_DONE

          // implicit null
          col >= record.size ->
            descriptor.getElementIndex(implicitNullElementNames[col - record.size])

          // regular element
          else -> descriptor.getElementIndex(knownElementNames[col])
        }
      col++
    } while (
      index == CompositeDecoder.UNKNOWN_NAME &&
        (format.ignoreUnknownKeys ||
          throw SerializationException("Unknown column '${table.header[col - 1]}'"))
    )
    col--
    return index
  }

  override fun decodeValue(): Any = decodeString()

  override fun decodeString(): String = record[col].also { col += 1 }

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
