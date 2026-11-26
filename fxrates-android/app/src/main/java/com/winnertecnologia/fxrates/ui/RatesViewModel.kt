package com.winnertecnologia.fxrates.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnertecnologia.fxrates.data.Currencies
import com.winnertecnologia.fxrates.data.FrankfurterApi
import com.winnertecnologia.fxrates.data.Quote
import com.winnertecnologia.fxrates.data.RatePoint
import com.winnertecnologia.fxrates.data.RatesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalTime

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Failure(val message: String) : LoadState<Nothing>
}

class RatesViewModel(
    private val repository: RatesRepository = FrankfurterApi(),
) : ViewModel() {

    var base by mutableStateOf("USD")
        private set
    var quotes by mutableStateOf<LoadState<List<Quote>>>(LoadState.Loading)
        private set
    var updatedAt by mutableStateOf<LocalTime?>(null)
        private set

    var history by mutableStateOf<LoadState<List<RatePoint>>>(LoadState.Loading)
        private set
    var days by mutableStateOf(30)
        private set

    private var historyJob: Job? = null

    init {
        loadLatest()
    }

    fun selectBase(newBase: String) {
        if (newBase == base) return
        base = newBase
        loadLatest()
    }

    fun loadLatest() {
        quotes = LoadState.Loading
        viewModelScope.launch {
            quotes = try {
                val symbols = Currencies.tracked.filter { it != base }
                LoadState.Success(repository.latest(base, symbols)).also {
                    updatedAt = LocalTime.now()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.Failure(e.message ?: "Network error")
            }
        }
    }

    fun loadHistory(quote: String, period: Int = days) {
        days = period
        history = LoadState.Loading
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            history = try {
                LoadState.Success(repository.history(base, quote, period))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.Failure(e.message ?: "Network error")
            }
        }
    }
}
