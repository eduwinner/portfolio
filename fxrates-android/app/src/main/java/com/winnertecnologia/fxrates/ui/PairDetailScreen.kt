package com.winnertecnologia.fxrates.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winnertecnologia.fxrates.data.RatePoint
import java.util.Locale
import kotlin.math.abs

private val periods = listOf(7 to "7D", 30 to "30D", 90 to "90D", 365 to "1Y")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairDetailScreen(model: RatesViewModel, quote: String, onBack: () -> Unit) {
    LaunchedEffect(quote) { model.loadHistory(quote) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${model.base} → $quote") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, (days, label) ->
                    SegmentedButton(
                        selected = model.days == days,
                        onClick = { model.loadHistory(quote, days) },
                        shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                    ) { Text(label) }
                }
            }

            when (val state = model.history) {
                LoadState.Loading -> Centered { CircularProgressIndicator() }
                is LoadState.Failure -> Centered {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is LoadState.Success -> {
                    val points = state.data
                    if (points.size < 2) {
                        Centered { Text("No data for this period.") }
                    } else {
                        Summary(points, model.days)
                        RateChart(
                            points,
                            Modifier.fillMaxWidth().height(260.dp).semantics {
                                contentDescription = "Rate history chart for ${model.base} to $quote, " +
                                    "from ${fmt(points.minOf { it.rate })} to ${fmt(points.maxOf { it.rate })}"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(points: List<RatePoint>, days: Int) {
    val first = points.first().rate
    val last = points.last().rate
    val change = (last - first) / first * 100
    val color = if (change >= 0) Color(0xFF1B873F) else Color(0xFFC62828)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(fmt(last), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(
            "${if (change >= 0) "▲" else "▼"} ${String.format(Locale.getDefault(), "%.2f", abs(change))}% in $days days",
            color = color,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Low", fmt(points.minOf { it.rate }))
            Stat("High", fmt(points.maxOf { it.rate }))
            Stat("Points", points.size.toString())
        }
    }
}

@Composable
private fun Stat(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Dependency-free line chart drawn with Compose Canvas. */
@Composable
private fun RateChart(points: List<RatePoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val rates = points.map { it.rate }
        val low = rates.min()
        val high = rates.max()
        val span = (high - low).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (points.size - 1)

        fun y(rate: Double) = (size.height * (1 - (rate - low) / span)).toFloat()

        val line = Path()
        points.forEachIndexed { i, p ->
            val offset = Offset(i * stepX, y(p.rate))
            if (i == 0) line.moveTo(offset.x, offset.y) else line.lineTo(offset.x, offset.y)
        }

        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            area,
            Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f))),
        )
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}

private fun fmt(value: Double) = String.format(Locale.getDefault(), "%.4f", value)
