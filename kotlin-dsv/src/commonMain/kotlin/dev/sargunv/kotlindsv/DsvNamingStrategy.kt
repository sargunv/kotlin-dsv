package dev.sargunv.kotlindsv

public interface DsvNamingStrategy {
  public fun toDsvName(name: String): String

  public fun fromDsvName(name: String): String

  public fun reversed(): DsvNamingStrategy =
    object : DsvNamingStrategy {
      override fun toDsvName(name: String) = this@DsvNamingStrategy.fromDsvName(name)

      override fun fromDsvName(name: String) = this@DsvNamingStrategy.toDsvName(name)
    }

  public data object Identity : DsvNamingStrategy {
    override fun toDsvName(name: String): String = name

    override fun fromDsvName(name: String): String = name
  }

  public data object SnakeCase : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name.replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }

    override fun fromDsvName(name: String): String =
      name.replace(Regex("_([a-z])")) { it.groupValues[1].uppercase() }
  }

  public data object KebabCase : DsvNamingStrategy {
    override fun toDsvName(name: String): String =
      name.replace(Regex("[A-Z]")) { "-${it.value.lowercase()}" }

    override fun fromDsvName(name: String): String =
      name.replace(Regex("-([a-z])")) { it.groupValues[1].uppercase() }
  }

  public class Composite(private val strategies: List<DsvNamingStrategy>) : DsvNamingStrategy {
    public constructor(vararg strategies: DsvNamingStrategy) : this(strategies.toList())

    override fun toDsvName(name: String): String =
      strategies.fold(name) { acc, strategy -> strategy.toDsvName(acc) }

    override fun fromDsvName(name: String): String =
      strategies.reversed().fold(name) { acc, strategy -> strategy.fromDsvName(acc) }
  }
}
