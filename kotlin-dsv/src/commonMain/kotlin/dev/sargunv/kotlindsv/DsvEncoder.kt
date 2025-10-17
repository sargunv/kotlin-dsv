package dev.sargunv.kotlindsv

import kotlinx.serialization.ExperimentalSerializationApi
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

  private var open = false

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    require(!open) { "Nested structures are not supported in DSV format" }
    open = true
    return this
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    require(open) { "No structure is open" }
    writer.writeRecord(record)
    record.fill("")
    open = false
  }

  override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
    nextIndex = index
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
}
