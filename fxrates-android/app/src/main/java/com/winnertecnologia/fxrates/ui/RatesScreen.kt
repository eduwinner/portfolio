package com.winnertecnologia.fxrates.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.winnertecnologia.fxrates.data.Currencies
import com.winnertecnologia.fxrates.data.Quote
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(model: RatesViewModel, onQuoteClick: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FX Rates") },
                actions = { TextButton(onClick = model::loadLatest) { Text("Refresh") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Currencies.bases.forEachIndexed { index, code ->
                    SegmentedButton(
                        selected = model.base == code,
                        onClick = { model.selectBase(code) },
                        shape = SegmentedButtonDefaults.itemShape(index, Currencies.bases.size),
                    ) { Text("${Currencies.flag(code)} $code") }
                }
            }

            when (val state = model.quotes) {
                LoadState.Loading -> Centered { CircularProgressIndicator() }
                is LoadState.Failure -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = model::loadLatest) { Text("Try again") }
                    }
                }
                is LoadState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "1 ${model.base} equals",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp).semantics { heading() },
                        )
                    }
                    items(state.data, key = { it.code }) { quote ->
                        QuoteRow(model.base, quote) { onQuoteClick(quote.code) }
                        HorizontalDivider()
                    }
                    item {
                        val time = model.updatedAt?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "-"
                        Text(
                            "Updated $time · Source: Frankfurter (central bank reference rates)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteRow(base: String, quote: Quote, onClick: () -> Unit) {
    val rate = String.format(Locale.getDefault(), "%.4f", quote.rate)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clearAndSetSemantics {
                contentDescription = "1 $base equals $rate ${Currencies.name(quote.code)}. Open history."
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Currencies.flag(quote.code), style = MaterialTheme.typography.headlineMedium)
        Column(Modifier.weight(1f)) {
            Text(quote.code, style = MaterialTheme.typography.titleMedium)
            Text(
                Currencies.name(quote.code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(rate, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
