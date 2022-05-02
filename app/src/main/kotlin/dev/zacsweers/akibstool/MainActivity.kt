package dev.zacsweers.akibstool

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.zacsweers.akibstool.ui.theme.AkibsToolTheme
import java.time.Instant
import java.time.ZoneId

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val intentUri: Uri? =
      intent?.clipData?.let { clipData ->
        // WhatsApp sends all these through clipData (?!?!?)
        (0 until clipData.itemCount)
          .asSequence()
          .map { clipData.getItemAt(it).uri }
          .singleOrNull { !it.path.orEmpty().endsWith(".vcf") }
          ?: run {
            Toast.makeText(this, "No text file found", Toast.LENGTH_SHORT).show()
            null
          }
      }
    val messageParser = MessageParser(resources.configuration.locales[0], ZoneId.systemDefault())
    setContent {
      AkibsToolTheme {
        AkibsTool(
          startLoading = intentUri != null
        ) { startDateMillis ->
          val startInstant = Instant.ofEpochMilli(startDateMillis)
          val uri =
            intentUri!!.let { contentResolver.openInputStream(it)!! }
          messageParser.parse(uri, startInstant)
        }
      }
    }
  }
}
