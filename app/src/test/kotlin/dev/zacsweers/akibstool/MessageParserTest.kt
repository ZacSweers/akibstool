package dev.zacsweers.akibstool

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Test

class MessageParserTest {
  @Test
  fun smokeTest_UK() {
    val parser = MessageParser(Locale.UK, ZoneOffset.UTC)
    val parsed =
      parser.parse(
        listOf(
            "23/04/2022, 07:52 - Joe Smith: It was really nice",
            "23/04/2022, 07:52 - Joe Smith: Also what is up",
            "23/04/2022, 07:52 - Joe-0Smith: It was really nice",
            "23/04/2021, 07:52 - Joe-0Smith: Old message",
          )
          .asSequence(),
        LocalDate.of(2022, 5, 2).minusMonths(6).atStartOfDay().toInstant(ZoneOffset.UTC)
      )
    assertThat(parsed)
      .containsExactly(
        "Joe Smith" to 2,
        "Joe-0Smith" to 1,
      )
  }

  @Test
  fun smokeTest_US() {
    val parser = MessageParser(Locale.US, ZoneId.of("America/New_York"))
    val parsed =
      parser.parse(
        listOf(
            "[3/21/22, 7:40:09 PM] Joe Smith: Wrong chat",
            "[3/21/22, 7:40:09 PM] Joe Smith: Wrong chat2",
            "[3/21/22, 7:40:09 PM] Joe-0Smith: Wrong chat",
            "[3/21/21, 7:40:09 PM] Joe-0Smith: Old message",
          )
          .asSequence(),
        LocalDate.now().minusMonths(6).atStartOfDay().toInstant(ZoneOffset.UTC)
      )
    assertThat(parsed)
      .containsExactly(
        "Joe Smith" to 2,
        "Joe-0Smith" to 1,
      )
  }
}
