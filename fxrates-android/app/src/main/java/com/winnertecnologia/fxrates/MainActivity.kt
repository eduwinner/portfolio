package com.winnertecnologia.fxrates

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winnertecnologia.fxrates.ui.PairDetailScreen
import com.winnertecnologia.fxrates.ui.RatesScreen
import com.winnertecnologia.fxrates.ui.RatesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FXRatesTheme { FXRatesApp() } }
    }
}

@Composable
private fun FXRatesApp(model: RatesViewModel = viewModel()) {
    var selectedQuote by rememberSaveable { mutableStateOf<String?>(null) }

    val quote = selectedQuote
    if (quote == null) {
        RatesScreen(model, onQuoteClick = { selectedQuote = it })
    } else {
        BackHandler { selectedQuote = null }
        PairDetailScreen(model, quote, onBack = { selectedQuote = null })
    }
}

@Composable
private fun FXRatesTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
