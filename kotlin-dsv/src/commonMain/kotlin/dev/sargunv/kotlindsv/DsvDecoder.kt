package dev.sargunv.kotlindsv

import kotlinx.io.Source
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder

/**
 * Internal decoder for DSV that wraps [DsvSequenceDecoder] to decode lists.
 *
 * This decoder uses the sequence decoder to decode individual records and collects them into a
 * list.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DsvDecoder(source: Source, private val format: DsvFormat) : AbstractDecoder() {

  private val sequenceDecoder = DsvSequenceDecoder(source, format)
  private var elements: List<Any?>? = null
  private var currentIndex = 0
  private var level = 0
  private var initialized = false

  override val serializersModule = format.serializersModule

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
    when (level) {
      0 -> {
        require(descriptor.kind == StructureKind.LIST) {
          "Top-level structure must be a list (got ${descriptor.kind})"
        }
        
        // Validate element type
        val elementDescriptor = descriptor.elementDescriptors.first()
        require(elementDescriptor.kind == StructureKind.CLASS) {
          "List elements must be classes (got ${elementDescriptor.kind})"
        }
        
        level++
        this
      }
      else -> throw SerializationException("Nested structures not supported at this level")
    }

  override fun endStructure(descriptor: SerialDescriptor) {
    level--
    if (level < 0) throw SerializationException("Unbalanced structure")
  }

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    if (!initialized) {
      // Mark as initialized so we don't keep returning 0
      initialized = true
      // Check if there are any records to decode
      if (!sequenceDecoder.hasRecords()) {
        return CompositeDecoder.DECODE_DONE
      }
      // Return 0 to allow decodeSerializableElement to be called
      return 0
    }
    
    // elements will be initialized after first decodeSerializableElement call
    if (elements == null) {
      return CompositeDecoder.DECODE_DONE
    }

    return if (currentIndex < elements!!.size) currentIndex else CompositeDecoder.DECODE_DONE
  }

  override fun <T> decodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    deserializer: kotlinx.serialization.DeserializationStrategy<T>,
    previousValue: T?,
  ): T {
    if (elements == null) {
      // First call - decode all elements
      @Suppress("UNCHECKED_CAST")
      val typedDeserializer = deserializer as kotlinx.serialization.DeserializationStrategy<Any?>
      elements = sequenceDecoder.decodeSequence(typedDeserializer).toList()
      currentIndex = 0
    }
    
    @Suppress("UNCHECKED_CAST")
    return elements!![currentIndex++] as T
  }
}
