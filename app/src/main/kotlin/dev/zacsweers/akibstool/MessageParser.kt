package dev.zacsweers.akibstool

import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class MessageParser(locale: Locale, zoneId: ZoneId) {
  companion object {
    /**
     * Long complicated regex to try to parse out the date and user from the message.
     *
     * First two groups try to parse the date and time, with optional surrounding brackets. First
     * group is the actual datetime and second is just an optional AM/PM suffix.
     * ```
     * [\[]?([0-9:,/\s]*)\s*(AM|PM)?[\]]?
     * ```
     *
     * Next bit skips the separator (either a space or with an optional dash)
     * ```
     * \s*-?\s*
     * ```
     *
     * Third group captures the user that sent the message
     * ```
     * ([-'a-zA-ZÀ-ÖØ-öø-ſ0-9\s]*):\s
     * ```
     *
     * And the rest just matches a space + remainder of the message.
     */
    private val MESSAGE_REGEX =
      Regex("[\\[]?([0-9:,/\\s]*)\\s*(AM|PM)?[\\]]?\\s*-?\\s*([-'a-zA-ZÀ-ÖØ-öø-ſ0-9\\s]*):\\s.*")
  }

  private data class Message(val timestamp: Instant, val user: String, val text: String)

  private val dateFieldOrder =
    when (dateFieldOrder(locale)) {
      "YMD" -> "yy/M/d"
      "DMY" -> "dd/MM/yyyy"
      "MDY" -> "M/d/yy"
      else -> error("Unrecognized date format")
    }

  // [3/21/21, 7:40:09 PM] Joe Smith: Wrong chat @xxx. Try the other chat
  private val format12 =
    DateTimeFormatter.ofPattern("$dateFieldOrder, h:mm:ss", locale).withZone(zoneId)

  // 23/04/2022, 07:47 - Joe Smith added you
  private val format24 =
    DateTimeFormatter.ofPattern("$dateFieldOrder, hh:mm", locale).withZone(zoneId)

  // Returns YMD, DMY, or MDY
  private fun dateFieldOrder(locale: Locale): String {
    val fmt: SimpleDateFormat =
      DateFormat.getDateInstance(DateFormat.SHORT, locale) as SimpleDateFormat
    return fmt.toPattern().replace("[^yMd]|(?<=(.))\\1".toRegex(), "").uppercase()
  }

  fun parse(inputStream: InputStream, startTime: Instant): List<Pair<String, Int>> {
    return inputStream.bufferedReader().useLines { lines -> parse(lines, startTime) }
  }

  fun parse(
    lines: Sequence<String>,
    startTime: Instant,
  ): List<Pair<String, Int>> {
    val linesList = lines.toList()
    return linesList
      .mapNotNull { line ->
        try {
          val result = MESSAGE_REGEX.find(line)
          val (timestamp, amPm, user) = result!!.destructured
          val hasAmPm = amPm.isNotBlank()
          val format = if (hasAmPm) format12 else format24
          val instant =
            LocalDate.from(format.parse(timestamp.trimEnd()))
              .atStartOfDay()
              .toInstant(ZoneOffset.UTC)
          if (instant.isBefore(startTime)) {
            return@mapNotNull null
          }
          Message(instant, user, "")
        } catch (e: Exception) {
          println(e.message)
          null
        }
      }
      .groupBy { it.user }
      .mapValues { (_, messages) -> messages.size }
      .entries
      .sortedByDescending { it.value }
      .map { (k, v) -> k to v }
  }
}
