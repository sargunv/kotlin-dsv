package dev.sargunv.kotlindsv

import kotlinx.io.Sink
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder

/**
 * Internal encoder for DSV that wraps [DsvSequenceEncoder] to encode lists.
 *
 * This encoder buffers list elements and delegates to [DsvSequenceEncoder] for the actual encoding.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DsvEncoder(private val sink: Sink, private val format: DsvFormat) :
  AbstractEncoder() {

  override val serializersModule = format.serializersModule

  private val dsvWriter = DsvWriter(sink, format.scheme)
  private var level = 0
  private var elementSerializer: SerializationStrategy<Any?>? = null
  private val elements = mutableListOf<Any?>()
  private lateinit var recordDescriptor: SerialDescriptor

  override fun beginCollection(
    descriptor: SerialDescriptor,
    collectionSize: Int,
  ): CompositeEncoder {
    require(level == 0) { "Top-level structure must be a list of records" }
    require(descriptor.kind == StructureKind.LIST) {
      "Top-level structure must be a list (got ${descriptor.kind})"
    }

    // Validate and write header immediately
    recordDescriptor = descriptor.elementDescriptors.first()
    require(recordDescriptor.kind == StructureKind.CLASS) {
      "List elements must be classes (got ${recordDescriptor.kind})"
    }

    val header = recordDescriptor.elementNames.toList()
    dsvWriter.writeRecord(header.map { format.namingStrategy.toDsvName(it) })

    level++
    return this
  }

  override fun <T> encodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T,
  ) {
    require(level == 1) { "Encoding elements must be at level 1" }
    if (elementSerializer == null) {
      @Suppress("UNCHECKED_CAST")
      elementSerializer = serializer as SerializationStrategy<Any?>
    }
    elements.add(value)
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    level--
    require(level >= 0) { "Unbalanced structure" }
    if (level == 0) {
      // Encode all buffered elements using the sequence encoder logic
      if (elementSerializer != null && elements.isNotEmpty()) {
        val sequenceEncoder = DsvSequenceEncoder(sink, format, dsvWriter, recordDescriptor)
        sequenceEncoder.encodeSequence(elementSerializer!!, elements.asSequence())
      }
      sink.close()
    }
  }
}
