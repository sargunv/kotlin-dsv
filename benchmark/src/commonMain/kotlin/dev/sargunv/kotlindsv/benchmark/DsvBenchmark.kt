package dev.sargunv.kotlindsv.benchmark

import dev.sargunv.kotlindsv.Csv
import kotlin.random.Random
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.serialization.Serializable

@Serializable
data class SampleRecord(
  val id: Int,
  val name: String,
  val email: String?,
  val age: Int,
  val salary: Double,
  val isActive: Boolean,
  val department: String,
  val status: Status,
  val rating: Double?,
  val yearsOfService: Int,
)

enum class Status {
  ACTIVE,
  INACTIVE,
  PENDING,
  SUSPENDED,
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
open class DsvBenchmark {
  private lateinit var records: List<SampleRecord>
  private lateinit var csvString: String

  private val random = Random(42)

  @Suppress("SameParameterValue")
  private fun generateRecords(count: Int): List<SampleRecord> {
    val names =
      listOf("Keiko", "Liam", "Mei", "Noah", "Olga", "Priya", "Quinn", "Rashid", "Sakura", "Tariq")
    val departments =
      listOf("Engineering", "Product Design", "Data Science", "Research & Development", "Security")
    val domains = listOf("example.com", "test.com", "demo.com")

    return List(count) { index ->
      SampleRecord(
        id = index + 1,
        name = names.random(random),
        email =
          if (random.nextBoolean()) "${names.random(random).lowercase()}@${domains.random(random)}"
          else null,
        age = random.nextInt(22, 65),
        salary = random.nextDouble(30000.0, 150000.0),
        isActive = random.nextBoolean(),
        department = departments.random(random),
        status = Status.entries.random(random),
        rating = if (random.nextBoolean()) random.nextDouble(1.0, 5.0) else null,
        yearsOfService = random.nextInt(0, 30),
      )
    }
  }

  @Setup
  fun setup() {
    records = generateRecords(10000)
    csvString = Csv.encodeToString(records)
  }

  @Benchmark
  fun serialization() {
    Csv.encodeToString(records)
  }

  @Benchmark
  fun deserialization() {
    Csv.decodeFromString<List<SampleRecord>>(csvString)
  }
}
