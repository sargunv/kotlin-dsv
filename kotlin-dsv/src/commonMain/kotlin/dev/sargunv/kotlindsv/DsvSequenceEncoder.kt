package dev.sargunv.kotlindsv

import kotlinx.io.Sink
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder

/**
 * Internal encoder for lazily encoding a sequence of objects to DSV records.
 *
 * Unlike [DsvEncoder], this encoder works on individual elements of a sequence rather than
 * serializing an entire list at once, allowing for lazy evaluation and streaming.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DsvSequenceEncoder(
  private val sink: Sink,
  private val format: DsvFormat,
) {
  private val dsvWriter = DsvWriter(sink, format.scheme)
  private var headerWritten = false
  private lateinit var header: List<String>
  private lateinit var recordDescriptor: SerialDescriptor

  fun <T> encodeSequence(serializer: SerializationStrategy<T>, sequence: Sequence<T>) {
    for (element in sequence) {
      if (!headerWritten) {
        // Write header on first element
        recordDescriptor = serializer.descriptor
        require(recordDescriptor.kind == StructureKind.CLASS) {
          "Element type must be a class (got ${recordDescriptor.kind})"
        }
        header = recordDescriptor.elementNames.toList()
        dsvWriter.writeRecord(header.map { format.namingStrategy.toDsvName(it) })
        headerWritten = true
      }

      val recordEncoder = RecordEncoder(format, dsvWriter, header, recordDescriptor)
      serializer.serialize(recordEncoder, element)
      recordEncoder.finishRecord()
    }

    // If no elements were encoded, still write the header if we have a descriptor
    // This is handled by the caller checking if the sequence is empty
  }

  /**
   * Encoder for a single DSV record.
   */
  private class RecordEncoder(
    private val format: DsvFormat,
    private val dsvWriter: DsvWriter,
    private val header: List<String>,
    private val recordDescriptor: SerialDescriptor,
  ) : AbstractEncoder() {

    override val serializersModule = format.serializersModule

    private var nextIndex: Int = -1
    private val record: MutableList<String> = MutableList(header.size) { "" }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
      require(descriptor === recordDescriptor) {
        "All records must have the same structure (expected $recordDescriptor, got $descriptor)"
      }
      return this
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
      nextIndex = index
      return true
    }

    override fun encodeString(value: String) {
      record[nextIndex] = value
      nextIndex = -1
    }

    override fun encodeValue(value: Any) = encodeString(value.toString())

    override fun encodeNull() = encodeString("")

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
      if (format.writeEnumsByName) encodeString(enumDescriptor.getElementName(index))
      else encodeInt(index)

    fun finishRecord() {
      dsvWriter.writeRecord(record)
      record.fill("")
    }
  }
}
