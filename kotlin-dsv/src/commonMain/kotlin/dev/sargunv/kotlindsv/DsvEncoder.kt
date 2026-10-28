package dev.sargunv.kotlindsv

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder

@OptIn(ExperimentalSerializationApi::class)
internal class DsvEncoder(
  private val format: DsvFormat,
  private val writer: DsvWriter,
  descriptor: SerialDescriptor,
) : AbstractEncoder() {

  override val serializersModule = format.serializersModule

  private var nextIndex: Int = -1
  private val record: MutableList<String> = MutableList(descriptor.elementsCount, init = { "" })
  private val columnNames: List<String> =
    List(descriptor.elementsCount) { index ->
      format.namingStrategy.toDsvName(descriptor.getElementName(index))
    }

  private var open = false
  private var recordNumber = 0
  private var currentColumnName: String? = null
  private var currentColumnIndex: Int? = null

  override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
    withEncodingExceptionHandling { super.encodeSerializableValue(serializer, value) }
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    if (open) throw encodingException("Nested structures are not supported in DSV format")
    open = true
    recordNumber++
    currentColumnName = null
    currentColumnIndex = null
    return this
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    require(open) { "No structure is open" }
    writer.writeRecord(record)
    record.fill("")
    open = false
    currentColumnName = null
    currentColumnIndex = null
  }

  override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
    nextIndex = index
    currentColumnIndex = index
    currentColumnName = columnNames.getOrNull(index)?.takeIf { it.isNotEmpty() }
    return true
  }

  override fun encodeValue(value: Any) = encodeString(value.toString())

  override fun encodeNull() = encodeString("")

  override fun encodeBoolean(value: Boolean): Unit = encodeString(value.toString())

  override fun encodeByte(value: Byte): Unit = encodeString(value.toString())

  override fun encodeShort(value: Short): Unit = encodeString(value.toString())

  override fun encodeInt(value: Int): Unit = encodeString(value.toString())

  override fun encodeLong(value: Long): Unit = encodeString(value.toString())

  override fun encodeFloat(value: Float): Unit = encodeString(value.toString())

  override fun encodeDouble(value: Double): Unit = encodeString(value.toString())

  override fun encodeChar(value: Char): Unit = encodeString(value.toString())

  override fun encodeString(value: String) {
    record[nextIndex] = value
    nextIndex = -1
  }

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
    if (format.writeEnumsByName) encodeString(enumDescriptor.getElementName(index))
    else encodeInt(index)

  private fun encodingException(message: String): DsvEncodingException =
    DsvEncodingException(
      message = message + dsvLocation(recordNumber, currentColumnName, currentColumnIndex),
      record = recordNumber,
      columnName = currentColumnName,
      columnIndex = currentColumnIndex,
    )

  private inline fun withEncodingExceptionHandling(block: () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: SerializationException) {
      throw e
    } catch (e: Exception) {
      throw DsvEncodingException(
        message = dsvExceptionMessage(e, recordNumber, currentColumnName, currentColumnIndex),
        record = recordNumber,
        columnName = currentColumnName,
        columnIndex = currentColumnIndex,
        cause = e,
      )
    }
  }
}
