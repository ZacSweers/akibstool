package dev.zacsweers.akibstool

import android.view.ContextThemeWrapper
import android.widget.CalendarView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.zacsweers.akibstool.ui.theme.AkibsToolTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AkibsTool(startLoading: Boolean, loadData: (startDateMillis: Long) -> List<Pair<String, Int>>) {
  Surface(modifier = Modifier.fillMaxSize()) {
    var showDialog by remember { mutableStateOf(startLoading) }
    var startDateMillis by remember { mutableStateOf(0L) }
    var loading by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<Pair<String, Int>>() }

    BackHandler(enabled = showDialog || loading || results.isNotEmpty()) {
      loading = false
      showDialog = false
      startDateMillis = 0L
      if (results.isNotEmpty()) {
        results.clear()
      }
    }

    when {
      results.isNotEmpty() -> {
        SelectionContainer {
          LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(results.size, key = { index -> results[index].first }) { i ->
              Text("${results[i].first} – ${results[i].second}")
            }
          }
        }
      }
      loading -> {
        CircularProgressIndicator(modifier = Modifier.wrapContentSize())
        results.clear()
        LaunchedEffect(key1 = null) {
          val newResults = withContext(Dispatchers.Default) { loadData(startDateMillis) }
          loading = false
          results += newResults
        }
      }
      showDialog -> {
        DatePicker(
          onDateSelected = { date ->
            startDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            showDialog = false
            loading = true
          },
          onDismissRequest = { showDialog = false }
        )
      }
      else -> {
        PromptScreen()
      }
    }
  }
}

@Composable
fun PromptScreen() {
  Text(
    text = "Share an export from Whatsapp",
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.headlineLarge,
  )
}

// Borrowed from https://stackoverflow.com/a/67496934
@Composable
fun DatePicker(onDateSelected: (LocalDate) -> Unit, onDismissRequest: () -> Unit) {
  val selDate = remember { mutableStateOf(LocalDate.now().minusMonths(6)) }

  Dialog(onDismissRequest = { onDismissRequest() }, properties = DialogProperties()) {
    Column(
      modifier =
        Modifier.wrapContentSize()
          .background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(size = 16.dp)
          )
    ) {
      Column(
        Modifier.defaultMinSize(minHeight = 72.dp)
          .fillMaxWidth()
          .background(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
          )
          .padding(16.dp)
      ) {
        Text(
          text = "Start Date\n(default is 6mo ago)",
          // style = MaterialTheme.typography.caption,
          color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.size(24.dp))

        Text(
          text = selDate.value.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
          // style = MaterialTheme.typography.h4,
          color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.size(16.dp))
      }

      CustomCalendarView(
        startDateMillis = selDate.value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        onDateSelected = { selDate.value = it }
      )

      Spacer(modifier = Modifier.size(8.dp))

      Row(
        modifier = Modifier.align(Alignment.End).padding(bottom = 16.dp, end = 16.dp),
      ) {
        TextButton(onClick = onDismissRequest) {
          Text(
            text = stringResource(id = android.R.string.cancel),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        TextButton(
          onClick = {
            onDateSelected(selDate.value)
            onDismissRequest()
          }
        ) {
          Text(
            text = stringResource(id = android.R.string.ok),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun CustomCalendarView(startDateMillis: Long, onDateSelected: (LocalDate) -> Unit) {
  AndroidView(
    modifier = Modifier.wrapContentSize(),
    factory = { context -> CalendarView(ContextThemeWrapper(context, R.style.CalenderViewCustom)) },
    update = { view ->
      view.date = startDateMillis
      view.maxDate = Instant.now().toEpochMilli()
      view.setOnDateChangeListener { _, year, month, dayOfMonth ->
        onDateSelected(
          LocalDate.now().withMonth(month + 1).withYear(year).withDayOfMonth(dayOfMonth)
        )
      }
    }
  )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  AkibsToolTheme { PromptScreen() }
}
