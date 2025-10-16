package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

class EncoderDecoderTest {

  @Serializable
  enum class Status {
    ACTIVE,
    INACTIVE,
    PENDING,
  }

  @Serializable
  data class Sample(
    val id: Int,
    val name: String,
    val price: Double,
    val count: Long?,
    val active: Boolean,
    val status: Status,
    val description: String?,
  )

  private val format = DsvFormat(DsvScheme(delimiter = ',', writeCrlf = false))

  @Test
  fun encodeBasicList() {
    val samples =
      listOf(
        Sample(1, "Item A", 19.99, 100, true, Status.ACTIVE, "First item"),
        Sample(2, "Item B", 29.99, null, false, Status.INACTIVE, null),
        Sample(3, "Item C", 39.99, 50, true, Status.PENDING, "Third item"),
      )

    val result = format.encodeToString(samples)

    val expected =
      """
      id,name,price,count,active,status,description
      1,Item A,19.99,100,true,ACTIVE,First item
      2,Item B,29.99,,false,INACTIVE,
      3,Item C,39.99,50,true,PENDING,Third item

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = format.decodeFromString<List<Sample>>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun encodeEmptyList() {
    val samples = emptyList<Sample>()
    val result = format.encodeToString(samples)
    assertEquals("id,name,price,count,active,status,description\n", result)

    val decoded = format.decodeFromString<List<Sample>>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun encodeSingleItem() {
    val samples = listOf(Sample(1, "Solo", 9.99, null, false, Status.ACTIVE, null))

    val result = format.encodeToString(samples)

    val expected =
      """
      id,name,price,count,active,status,description
      1,Solo,9.99,,false,ACTIVE,

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = format.decodeFromString<List<Sample>>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun encodeWithSpecialCharacters() {
    val samples =
      listOf(
        Sample(1, "Item, with comma", 19.99, null, true, Status.ACTIVE, "Quote: \"test\""),
        Sample(2, "Item\nwith newline", 29.99, null, false, Status.INACTIVE, "Normal"),
      )

    val result = format.encodeToString(samples)

    val expected =
      """
      id,name,price,count,active,status,description
      1,"Item, with comma",19.99,,true,ACTIVE,"Quote: ""test""${""}"
      2,"Item
      with newline",29.99,,false,INACTIVE,Normal

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = format.decodeFromString<List<Sample>>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun encodeEnumsByOrdinal() {
    val formatByOrdinal =
      DsvFormat(DsvScheme(delimiter = ',', writeCrlf = false), writeEnumsByName = false)
    val samples = listOf(Sample(1, "Item", 9.99, null, true, Status.PENDING, null))

    val result = formatByOrdinal.encodeToString(samples)

    val expected =
      """
      id,name,price,count,active,status,description
      1,Item,9.99,,true,2,

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = formatByOrdinal.decodeFromString<List<Sample>>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun encodeWithSnakeCaseNaming() {
    val formatSnakeCase =
      DsvFormat(
        scheme = DsvScheme(delimiter = ',', writeCrlf = false),
        namingStrategy = DsvNamingStrategy.SnakeCase,
      )

    @Serializable
    data class CamelCaseSample(
      val firstName: String,
      val lastName: String,
      val emailAddress: String,
      val phoneNumber: String?,
    )

    val samples =
      listOf(
        CamelCaseSample("John", "Doe", "john@example.com", "555-1234"),
        CamelCaseSample("Jane", "Smith", "jane@example.com", null),
      )

    val result = formatSnakeCase.encodeToString(samples)

    val expected =
      """
      first_name,last_name,email_address,phone_number
      John,Doe,john@example.com,555-1234
      Jane,Smith,jane@example.com,

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = formatSnakeCase.decodeFromString<List<CamelCaseSample>>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun treatMissingColumnsAsNull() {
    val formatWithOption =
      DsvFormat(
        scheme = DsvScheme(delimiter = ',', writeCrlf = false),
        treatMissingColumnsAsNull = true,
      )

    @Serializable
    data class PartialSample(
      val id: Int?,
      val name: String?,
      val age: Byte,
      val quantity: Short,
      val rating: Float,
      val grade: Char,
    )

    val csvWithMissingColumns =
      """
      age,quantity,rating,grade
      25,100,4.5,A
      30,200,3.8,B
      """
        .trimIndent()

    val decoded = formatWithOption.decodeFromString<List<PartialSample>>(csvWithMissingColumns)
    val expected =
      listOf(
        PartialSample(null, null, 25, 100, 4.5f, 'A'),
        PartialSample(null, null, 30, 200, 3.8f, 'B'),
      )
    assertEquals(expected, decoded)

    val encoded = formatWithOption.encodeToString(decoded)
    val expectedEncoded =
      """
      id,name,age,quantity,rating,grade
      ,,25,100,4.5,A
      ,,30,200,3.8,B

      """
        .trimIndent()
    assertEquals(expectedEncoded, encoded)
  }

  @Test
  fun encodeNonListFails() {
    val sample = Sample(1, "Item", 9.99, null, true, Status.ACTIVE, null)
    assertFailsWith<IllegalArgumentException> { format.encodeToString(sample) }
    assertFailsWith<IllegalArgumentException> { format.decodeFromString<Sample>("") }
  }

  @Test
  fun encodeListOfMapsFails() {
    val maps = listOf(mapOf("a" to "1", "b" to "2"), mapOf("a" to "3", "b" to "4"))
    assertFailsWith<IllegalArgumentException> { format.encodeToString(maps) }
    assertFailsWith<IllegalArgumentException> {
      format.decodeFromString<List<Map<String, String>>>("a,b\n1,2")
    }
  }

  @Test
  fun encodeListOfListsFails() {
    val lists = listOf(listOf("a", "b"), listOf("c", "d"))
    assertFailsWith<IllegalArgumentException> { format.encodeToString(lists) }
    assertFailsWith<IllegalArgumentException> {
      format.decodeFromString<List<List<String>>>("a,b\nc,d")
    }
  }

  @Test
  fun encodeListWithNestedObjectsFails() {
    @Serializable data class Address(val street: String, val city: String)

    @Serializable data class Person(val name: String, val address: Address)

    val people = listOf(Person("John", Address("Main St", "NYC")))
    assertFailsWith<IllegalArgumentException> { format.encodeToString(people) }
    assertFailsWith<IllegalArgumentException> {
      format.decodeFromString<List<Person>>("name,address\nJohn,Main St")
    }
  }

  @Test
  fun encodeListWithNestedListsFails() {
    @Serializable data class Order(val id: Int, val items: List<String>)

    val orders = listOf(Order(1, listOf("item1", "item2")))
    assertFailsWith<IllegalArgumentException> { format.encodeToString(orders) }
    assertFailsWith<IllegalArgumentException> {
      format.decodeFromString<List<Order>>("id,items\n1,item1")
    }
  }
}
