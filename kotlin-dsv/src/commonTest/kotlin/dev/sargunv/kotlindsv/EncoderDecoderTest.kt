package dev.sargunv.kotlindsv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

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

  object PipeListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("PipeList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<String>) {
      encoder.encodeString(value.joinToString("|"))
    }

    override fun deserialize(decoder: Decoder): List<String> {
      val raw = decoder.decodeString()
      return if (raw.isEmpty()) emptyList() else raw.split("|")
    }
  }

  object PipeMapSerializer : KSerializer<Map<String, String>> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("PipeMap", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Map<String, String>) {
      encoder.encodeString(value.entries.joinToString("|") { "${it.key}=${it.value}" })
    }

    override fun deserialize(decoder: Decoder): Map<String, String> {
      val raw = decoder.decodeString()
      if (raw.isEmpty()) return emptyMap()
      return raw.split("|").associate {
        val parts = it.split("=", limit = 2)
        parts[0] to parts.getOrElse(1) { "" }
      }
    }
  }

  data class Token(val value: String)

  object TokenAsStringSerializer : KSerializer<Token> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("Token", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Token) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Token = Token(decoder.decodeString())
  }

  @Serializable data class NestedToken(val kind: String)

  object NestedTokenSerializer : KSerializer<NestedToken> by NestedToken.serializer()

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

    val decoded = format.decodeFromString<Sample>(result)
    assertEquals(samples, decoded)
  }

  @Test
  fun encodeEmptyList() {
    val samples = emptyList<Sample>()
    val result = format.encodeToString(samples)
    assertEquals("id,name,price,count,active,status,description\n", result)

    val decoded = format.decodeFromString<Sample>(result)
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

    val decoded = format.decodeFromString<Sample>(result)
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

    val decoded = format.decodeFromString<Sample>(result)
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

    val decoded = formatByOrdinal.decodeFromString<Sample>(result)
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

    val decoded = formatSnakeCase.decodeFromString<CamelCaseSample>(result)
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

    val decoded = formatWithOption.decodeFromString<PartialSample>(csvWithMissingColumns)
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
  fun decodeLeadingUtf8Bom() {
    @Serializable data class Row(@SerialName("agency_id") val agencyId: String)

    val csv = "\uFEFFagency_id\nDTA\n"
    assertEquals(listOf(Row("DTA")), format.decodeFromString<Row>(csv))
  }

  @Test
  fun jaggedRowPolicyNormalize() {
    val format =
      DsvFormat(
        scheme =
          DsvScheme(delimiter = ',', writeCrlf = false, jaggedRowPolicy = JaggedRowPolicy.Normalize)
      )

    @Serializable data class Person(val name: String, val age: Int, val city: String?)

    val csv =
      """
      name,age,city
      Alice,30,NYC
      Bob,25
      Charlie,35,LA,Extra
      """
        .trimIndent()

    val decoded = format.decodeFromString<Person>(csv)
    assertEquals(
      listOf(Person("Alice", 30, "NYC"), Person("Bob", 25, null), Person("Charlie", 35, "LA")),
      decoded,
    )
  }

  @Test
  fun jaggedRowPolicySkip() {
    val format =
      DsvFormat(
        scheme =
          DsvScheme(delimiter = ',', writeCrlf = false, jaggedRowPolicy = JaggedRowPolicy.Skip)
      )

    @Serializable data class Person(val name: String, val age: Int, val city: String?)

    val csv =
      """
      name,age,city
      Alice,30,NYC
      Bob,25
      Charlie,35,LA,Extra
      Dana,40,SF
      """
        .trimIndent()

    val decoded = format.decodeFromString<Person>(csv)
    assertEquals(listOf(Person("Alice", 30, "NYC"), Person("Dana", 40, "SF")), decoded)
  }

  @Test
  fun jaggedRowPolicyReject() {
    @Serializable data class Person(val name: String, val age: Int, val city: String?)

    val csv =
      """
      name,age,city
      Alice,30,NYC
      Bob,25
      """
        .trimIndent()

    assertFailsWith<DsvParseException> { format.decodeFromString<Person>(csv) }
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
      format.decodeFromString<Person>("name,address\nJohn,Main St")
    }
  }

  @Test
  fun encodeListWithNestedListsFails() {
    @Serializable data class Order(val id: Int, val items: List<String>)

    val orders = listOf(Order(1, listOf("item1", "item2")))
    assertFailsWith<IllegalArgumentException> { format.encodeToString(orders) }
    assertFailsWith<IllegalArgumentException> {
      format.decodeFromString<Order>("id,items\n1,item1")
    }
  }

  @Test
  fun customListSerializerRoundTrip() {
    @Serializable
    data class Order(
      val id: Int,
      @Serializable(with = PipeListSerializer::class) val items: List<String>,
    )

    val orders = listOf(Order(1, listOf("item1", "item2")))
    val result = format.encodeToString(orders)

    val expected =
      """
      id,items
      1,item1|item2

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = format.decodeFromString<Order>(result)
    assertEquals(orders, decoded)
  }

  @Test
  fun customMapSerializerRoundTrip() {
    @Serializable
    data class Attrs(
      val id: Int,
      @Serializable(with = PipeMapSerializer::class) val attrs: Map<String, String>,
    )

    val rows = listOf(Attrs(1, mapOf("k" to "v", "k2" to "v2")))
    val result = format.encodeToString(rows)

    val expected =
      """
      id,attrs
      1,k=v|k2=v2

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = format.decodeFromString<Attrs>(result)
    assertEquals(rows, decoded)
  }

  @Test
  fun contextualPrimitiveLike() {
    val format =
      DsvFormat(
        scheme = DsvScheme(delimiter = ',', writeCrlf = false),
        serializersModule = SerializersModule { contextual(TokenAsStringSerializer) },
      )

    @Serializable data class ContextualRow(@Contextual val token: Token)

    val rows = listOf(ContextualRow(Token("abc")))
    val result = format.encodeToString(rows)

    val expected =
      """
      token
      abc

      """
        .trimIndent()

    assertEquals(expected, result)

    val decoded = format.decodeFromString<ContextualRow>(result)
    assertEquals(rows, decoded)
  }

  @Test
  fun contextualNestedClassFails() {
    val format =
      DsvFormat(
        scheme = DsvScheme(delimiter = ',', writeCrlf = false),
        serializersModule = SerializersModule { contextual(NestedTokenSerializer) },
      )

    @Serializable data class ContextualNestedRow(@Contextual val token: NestedToken)

    assertFailsWith<IllegalArgumentException> {
      format.encodeToString(listOf(ContextualNestedRow(NestedToken("x"))))
    }
  }

  @Test
  fun emptyCustomListOnNullableIsNull() {
    @Serializable
    data class MaybeItems(
      val id: Int,
      @Serializable(with = PipeListSerializer::class) val items: List<String>?,
    )

    val decoded = format.decodeFromString<MaybeItems>("id,items\n1,\n")
    assertEquals(listOf(MaybeItems(1, null)), decoded)
  }
}
