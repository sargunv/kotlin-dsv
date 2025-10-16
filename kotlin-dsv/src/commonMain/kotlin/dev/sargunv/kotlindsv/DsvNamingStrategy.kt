package dev.sargunv.kotlindsv

/**
 * Strategy for transforming property names to/from [DSV][DsvFormat] column names.
 *
 * Used by [DsvFormat] to map between Kotlin property names and column headers in DSV data.
 */
public interface DsvNamingStrategy {
  /** Transforms a Kotlin property name to a DSV column name. */
  public fun toDsvName(name: String): String

  /** Transforms a [DSV][DsvFormat] column name to a Kotlin property name. */
  public fun fromDsvName(name: String): String

  /** Returns a reversed strategy that swaps [toDsvName] and [fromDsvName]. */
  public fun reversed(): DsvNamingStrategy =
    object : DsvNamingStrategy {
      override fun toDsvName(name: String) = this@DsvNamingStrategy.fromDsvName(name)

      override fun fromDsvName(name: String) = this@DsvNamingStrategy.toDsvName(name)
    }

  /** Identity strategy that performs no transformation. */
  public data object Identity : DsvNamingStrategy {
    override fun toDsvName(name: String): String = name

    override fun fromDsvName(name: String): String = name
  }

  /** Snake case strategy that converts camelCase to snake_case. */
  public data object SnakeCase : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name.replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }

    override fun fromDsvName(name: String): String =
      name.replace(Regex("_([a-z])")) { it.groupValues[1].uppercase() }
  }

  /** Kebab case strategy that converts camelCase to kebab-case. */
  public data object KebabCase : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name.replace(Regex("[A-Z]")) { "-${it.value.lowercase()}" }

    override fun fromDsvName(name: String): String =
      name.replace(Regex("-([a-z])")) { it.groupValues[1].uppercase() }
  }

  /** PascalCase strategy that converts camelCase to PascalCase. */
  public data object PascalCase : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      if (name.isEmpty()) name else name[0].uppercase() + name.substring(1)

    override fun fromDsvName(name: String): String =
      if (name.isEmpty()) name else name[0].lowercase() + name.substring(1)
  }

  /** Title case words strategy that converts camelCase to "Title Case Words". */
  public data object TitleCaseWords : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name
        .replace(Regex("([A-Z])")) { " ${it.value}" }
        .trim()
        .replaceFirstChar { it.uppercase() }

    override fun fromDsvName(name: String): String =
      name
        .split(Regex("\\s+"))
        .mapIndexed { index, word ->
          if (index == 0) word.lowercase() else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
  }

  /** Sentence case words strategy that converts camelCase to "Sentence case words". */
  public data object SentenceCaseWords : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name
        .replace(Regex("([A-Z])")) { " ${it.value.lowercase()}" }
        .trim()
        .replaceFirstChar { it.uppercase() }

    override fun fromDsvName(name: String): String =
      name
        .lowercase()
        .split(Regex("\\s+"))
        .mapIndexed { index, word ->
          if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
  }

  /** Lowercase words strategy that converts camelCase to "lowercase words". */
  public data object LowercaseWords : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name.replace(Regex("([A-Z])")) { " ${it.value.lowercase()}" }.trim()

    override fun fromDsvName(name: String): String =
      name
        .split(Regex("\\s+"))
        .mapIndexed { index, word ->
          if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
  }

  /** Uppercase words strategy that converts camelCase to "UPPERCASE WORDS". */
  public data object UppercaseWords : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name.replace(Regex("([A-Z])")) { " ${it.value}" }.trim().uppercase()

    override fun fromDsvName(name: String): String =
      name
        .lowercase()
        .split(Regex("\\s+"))
        .mapIndexed { index, word ->
          if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
  }

  /** Composite strategy that applies multiple strategies in sequence. */
  public class Composite(private val strategies: List<DsvNamingStrategy>) : DsvNamingStrategy {
    public constructor(vararg strategies: DsvNamingStrategy) : this(strategies.toList())

    override fun toDsvName(name: String): String =
      strategies.fold(name) { acc, strategy -> strategy.toDsvName(acc) }

    override fun fromDsvName(name: String): String =
      strategies.reversed().fold(name) { acc, strategy -> strategy.fromDsvName(acc) }
  }
}
