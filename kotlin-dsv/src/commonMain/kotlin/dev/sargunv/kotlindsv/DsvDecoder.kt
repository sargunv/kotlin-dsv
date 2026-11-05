package dev.sargunv.kotlindsv

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
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
  private var recordNumber = 0
  private var currentColumnName: String? = null
  private var currentColumnIndex: Int? = null

  internal fun hasNext(): Boolean = records.hasNext()

  override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
    withDecodingExceptionHandling {
      deserializer.deserialize(this)
    }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    if (open) throw decodingException("Nested structures are not supported in DSV format")
    open = true
    val next = records.next()
    record = if (format.trimFields) next.map { it.trim() } else next
    recordNumber++
    currentColumnName = null
    currentColumnIndex = null
    return this
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    require(open) { "No structure is open" }
    col = 0
    open = false
    currentColumnName = null
    currentColumnIndex = null
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
        (format.ignoreUnknownKeys || throw unknownColumnException(col - 1))
    )
    col--
    if (index == CompositeDecoder.DECODE_DONE) {
      currentColumnName = null
      currentColumnIndex = null
    } else {
      currentColumnIndex = col
      currentColumnName = columnNameAt(col)
    }
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
    throw decodingException("Expected null, but got '$value'")
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

  private fun columnNameAt(index: Int): String? {
    val name =
      if (index < table.header.size) table.header[index].trim()
      else implicitNullElementNames.getOrNull(index - record.size)
    return name?.takeIf { it.isNotEmpty() }
  }

  private fun unknownColumnException(index: Int): DsvDecodingException {
    val header = table.header[index]
    return decodingException(
      message = "Unknown column '$header'",
      columnName = columnNameAt(index),
      columnIndex = index,
    )
  }

  private fun decodingException(
    message: String,
    columnName: String? = currentColumnName,
    columnIndex: Int? = currentColumnIndex,
  ): DsvDecodingException =
    DsvDecodingException(
      message = message + dsvLocation(recordNumber, columnName, columnIndex),
      record = recordNumber,
      columnName = columnName,
      columnIndex = columnIndex,
    )

  private inline fun <T> withDecodingExceptionHandling(block: () -> T): T {
    try {
      return block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: MissingFieldException) {
      throw DsvDecodingException(
        message = dsvExceptionMessage(e, recordNumber, currentColumnName, currentColumnIndex),
        record = recordNumber,
        columnName = currentColumnName,
        columnIndex = currentColumnIndex,
        cause = e,
      )
    } catch (e: SerializationException) {
      throw e
    } catch (e: Exception) {
      throw DsvDecodingException(
        message = dsvExceptionMessage(e, recordNumber, currentColumnName, currentColumnIndex),
        record = recordNumber,
        columnName = currentColumnName,
        columnIndex = currentColumnIndex,
        cause = e,
      )
    }
  }
}
