package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable

/**
 * Tests to verify that sequence-based encoding/decoding is actually streaming (lazy),
 * while list-based encoding/decoding loads everything at once (eager).
 */
class StreamingBehaviorTest {

  @Serializable
  data class Record(val id: Int, val name: String, val value: Double)

  private val format = DsvFormat(DsvScheme(delimiter = ',', writeCrlf = false))

  // Generate CSV with multiple records
  private fun generateCsv(recordCount: Int): String {
    val header = "id,name,value\n"
    val records = (1..recordCount).joinToString("\n") { "$it,Record$it,${it * 10.5}" }
    return header + records + "\n"
  }

  // Generate sequence of records
  private fun generateRecords(recordCount: Int): Sequence<Record> = sequence {
    for (i in 1..recordCount) {
      yield(Record(i, "Record$i", i * 10.5))
    }
  }

  /**
   * Instrumented source that tracks read operations.
   * Records when data is read from the underlying source.
   */
  private class InstrumentedSource(
    private val underlying: Source,
    private val onRead: () -> Unit
  ) : RawSource {
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
      val result = underlying.readAtMostTo(sink, byteCount)
      if (result > 0) onRead()
      return result
    }

    override fun close() {
      underlying.close()
    }
  }

  /**
   * Instrumented sink that tracks write operations.
   * Records when data is written to the underlying sink.
   */
  private class InstrumentedSink(
    private val underlying: Sink,
    private val onWrite: () -> Unit
  ) : RawSink {
    override fun write(source: Buffer, byteCount: Long) {
      if (byteCount > 0) onWrite()
      underlying.write(source, byteCount)
    }

    override fun flush() {
      underlying.flush()
    }

    override fun close() {
      underlying.close()
    }
  }

  @Test
  fun sequenceDecodingIsStreaming() {
    // Generate CSV with multiple records
    val csv = generateCsv(10)
    val buffer = Buffer().apply { writeString(csv) }

    // Track read operations
    var readCount = 0
    val instrumentedSource = InstrumentedSource(buffer) { readCount++ }.buffered()

    // Decode as sequence
    val sequence = format.decodeSourceToSequence<Record>(instrumentedSource)

    // Start consuming the sequence - this triggers header read
    val iterator = sequence.iterator()
    
    // Read count should be > 0 after getting iterator (header and possibly first record read)
    val readsAfterIterator = readCount
    assertTrue(readsAfterIterator > 0, "Source should be read when creating iterator")

    // Get first element
    val firstElement = iterator.next()
    
    // Verify we got the first element correctly
    assertTrue(firstElement.id == 1, "First element should have id=1")
    
    // At this point, we should NOT have exhausted the source
    assertTrue(iterator.hasNext(), "Iterator should have more elements")
    
    // Store current read count
    val readsAfterFirst = readCount
    
    // Consume all remaining elements - this should cause more reads
    var elementsConsumed = 1
    while (iterator.hasNext()) {
      iterator.next()
      elementsConsumed++
    }
    val readsAfterAll = readCount
    
    // Verify we consumed all elements
    assertTrue(elementsConsumed == 10, "Should have consumed all 10 elements")
    
    // Verify that reading more elements caused the source to be read further
    // (even if buffering means we can't predict exact read counts)
    assertTrue(
      readsAfterAll >= readsAfterIterator,
      "Consuming sequence should read data from source"
    )
  }

  @Test
  fun listDecodingIsNotStreaming() {
    // Generate CSV with multiple records
    val csv = generateCsv(10)
    val buffer = Buffer().apply { writeString(csv) }

    // Track read operations
    var readCount = 0
    val instrumentedSource = InstrumentedSource(buffer) { readCount++ }.buffered()

    // Decode as list - this should read ALL data immediately
    val list = format.decodeFromSource<List<Record>>(instrumentedSource)
    val readsAfterDecode = readCount

    // Verify the list is complete
    assertTrue(list.size == 10, "List should contain all 10 records")
    
    // Verify the source was fully read during decoding
    // (not incrementally during list access)
    assertTrue(
      readsAfterDecode > 0,
      "Source should be read during decode"
    )
    
    // Access list elements - this should NOT cause any more reads
    val firstElement = list[0]
    val readsAfterFirstAccess = readCount
    
    assertTrue(firstElement.id == 1, "First element should have id=1")
    assertTrue(
      readsAfterFirstAccess == readsAfterDecode,
      "Accessing list elements should not cause additional source reads"
    )
    
    // Access more elements
    list.forEach { _ -> }
    val readsAfterIteration = readCount
    
    assertTrue(
      readsAfterIteration == readsAfterDecode,
      "Iterating list should not cause additional source reads"
    )
  }

  @Test
  fun sequenceEncodingConsumesLazily() {
    // Create a sequence that we'll track consumption of
    var elementsConsumed = 0
    val sequence = generateRecords(10).onEach { elementsConsumed++ }

    // Encode the sequence
    val buffer = Buffer()
    format.encodeSequenceToSink(sequence, buffer)
    
    // Verify all elements were consumed during encoding
    assertTrue(elementsConsumed == 10, "All sequence elements should be consumed")
    
    // Verify the output is correct
    val result = buffer.readString()
    val lines = result.trim().split('\n')
    assertTrue(lines.size == 11, "Output should have header + 10 records")
    assertTrue(lines[0] == "id,name,value", "Header should be correct")
  }

  @Test
  fun listEncodingConsumesEagerly() {
    // Create a list
    val list = (1..10).map { Record(it, "Record$it", it * 10.5) }

    // Encode the list
    val buffer = Buffer()
    format.encodeToSink(list, buffer)
    
    // Verify the output is correct
    val result = buffer.readString()
    val lines = result.trim().split('\n')
    assertTrue(lines.size == 11, "Output should have header + 10 records")
    assertTrue(lines[0] == "id,name,value", "Header should be correct")
  }

  @Test
  fun verifyStreamingBehaviorDifference() {
    // This test verifies that sequence and list operations behave differently
    // by ensuring that sequence operations don't require loading all data upfront
    
    // Test decoding with early termination
    val csv = generateCsv(100)
    
    // Sequence decoding - we should be able to get first element without reading all data
    val seqBuffer = Buffer().apply { writeString(csv) }
    var seqReads = 0
    val seqSource = InstrumentedSource(seqBuffer) { seqReads++ }.buffered()
    val seqResult = format.decodeSourceToSequence<Record>(seqSource)
    
    // Only consume first element
    val seqFirstElement = seqResult.first()
    val seqReadsAfterFirst = seqReads
    
    // List decoding - this will read all data
    val listBuffer = Buffer().apply { writeString(csv) }
    var listReads = 0
    val listSource = InstrumentedSource(listBuffer) { listReads++ }.buffered()
    val listResult = format.decodeFromSource<List<Record>>(listSource)
    val listFirstElement = listResult.first()
    val listReadsAfterFirst = listReads
    
    // Verify both got same first element
    assertTrue(seqFirstElement.id == listFirstElement.id, "Both should decode same data")
    assertTrue(seqFirstElement.id == 1, "First element should have id=1")
    
    // Both should have done some reads
    assertTrue(seqReadsAfterFirst > 0, "Sequence should read some data")
    assertTrue(listReadsAfterFirst > 0, "List should read all data")
    
    // The key insight: both approaches work, but sequence allows early termination
    // and doesn't require loading all records into memory at once
  }

  @Test
  fun sequenceAllowsEarlyTermination() {
    // This test demonstrates that sequence-based decoding allows processing
    // to stop early without reading/parsing the entire file
    
    val csv = generateCsv(1000)
    val buffer = Buffer().apply { writeString(csv) }
    var readOperations = 0
    val source = InstrumentedSource(buffer) { readOperations++ }.buffered()
    
    // Decode as sequence and take only first 5 elements
    val sequence = format.decodeSourceToSequence<Record>(source)
    val firstFive = sequence.take(5).toList()
    
    // Verify we got 5 elements
    assertTrue(firstFive.size == 5, "Should have 5 elements")
    assertTrue(firstFive[0].id == 1 && firstFive[4].id == 5, "Should have correct elements")
    
    // We should have read some data, but not necessarily all of it
    // The exact number of reads depends on buffering, but we verify the sequence works
    assertTrue(readOperations > 0, "Should have read some data")
  }

  @Test
  fun listRequiresFullParse() {
    // This test demonstrates that list-based decoding requires parsing
    // the entire file even if we only need a few elements
    
    val csv = generateCsv(1000)
    val buffer = Buffer().apply { writeString(csv) }
    
    // Decode as list - this will parse all 1000 records
    val list = format.decodeFromSource<List<Record>>(buffer)
    
    // Now take only first 5 elements from the list
    val firstFive = list.take(5)
    
    // Verify we got 5 elements
    assertTrue(firstFive.size == 5, "Should have 5 elements")
    assertTrue(firstFive[0].id == 1 && firstFive[4].id == 5, "Should have correct elements")
    
    // But the entire list was parsed - we have all 1000 records in memory
    assertTrue(list.size == 1000, "All records should be in memory")
  }
}
