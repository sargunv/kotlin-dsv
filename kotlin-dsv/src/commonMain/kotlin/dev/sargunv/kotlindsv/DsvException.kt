package dev.sargunv.kotlindsv

import kotlinx.serialization.SerializationException

/**
 * Exception thrown when encoding, decoding, or parsing [DSV][DsvFormat] data fails.
 *
 * [DsvParseException] covers malformed input. [DsvDecodingException] and [DsvEncodingException]
 * cover failures while mapping between parsed records and Kotlin values.
 */
public sealed class DsvException : SerializationException {
  protected constructor(message: String) : super(message)

  protected constructor(message: String, cause: Throwable?) : super(message, cause)
}

/**
 * Exception thrown when a value cannot be decoded from [DSV][DsvFormat] input.
 *
 * Custom serializers and generated constructors that throw a non-[SerializationException] (for
 * example [IllegalArgumentException] from `require`, [NumberFormatException], or a date-time parse
 * error) are wrapped in this type. The message includes the record and column where decoding
 * failed, and [cause] is the original exception.
 *
 * [SerializationException] is rethrown unchanged, except
 * [kotlinx.serialization.MissingFieldException], which is wrapped in this type with the original as
 * cause so the record is included.
 *
 * @property record 1-based data record number. The first row after the header is 1.
 * @property columnName Column header as it appears in the input, if known.
 * @property columnIndex 0-based column index in the record, if known.
 */
public class DsvDecodingException(
  message: String,
  public val record: Int,
  public val columnName: String? = null,
  public val columnIndex: Int? = null,
  cause: Throwable? = null,
) : DsvException(message, cause)

/**
 * Exception thrown when a value cannot be encoded to [DSV][DsvFormat] output.
 *
 * Custom serializers that throw a non-[SerializationException] are wrapped in this type. The
 * message includes the record and column where encoding failed, and [cause] is the original
 * exception. [SerializationException] is rethrown unchanged.
 *
 * @property record 1-based data record number. The first encoded row after the header is 1.
 * @property columnName Column header as it would be written, if known.
 * @property columnIndex 0-based column index in the record, if known.
 */
public class DsvEncodingException(
  message: String,
  public val record: Int,
  public val columnName: String? = null,
  public val columnIndex: Int? = null,
  cause: Throwable? = null,
) : DsvException(message, cause)

internal fun dsvLocation(record: Int, columnName: String?, columnIndex: Int?): String =
  buildString {
    append(" at record ")
    append(record)
    if (columnName != null) {
      append(", column '")
      append(columnName)
      append("'")
    } else if (columnIndex != null) {
      append(", column ")
      append(columnIndex)
    }
  }

internal fun dsvExceptionMessage(
  cause: Exception,
  record: Int,
  columnName: String?,
  columnIndex: Int?,
): String {
  val detail = cause.message ?: cause::class.simpleName ?: "exception"
  return detail + dsvLocation(record, columnName, columnIndex)
}
