package bose.ankush.home.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiUnavailableException
import bose.ankush.home.domain.ai.OnDeviceAiClient
import bose.ankush.home.domain.ai.SummarizeWeather
import bose.ankush.home.domain.ai.WeatherAdvice
import bose.ankush.home.domain.ai.WeatherSummary
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.ai_tip_jacket
import bose.ankush.home.generated.resources.ai_tip_sunscreen
import bose.ankush.home.generated.resources.ai_tip_umbrella
import bose.ankush.home.generated.resources.ai_tip_wind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** [isAvailable] gates the home card: false until the model is known to be usable on this device. */
internal data class AiSummaryScreenState(
    val isAvailable: Boolean = false,
    val summary: AiSummaryUiState = AiSummaryUiState(),
)

internal sealed interface AiSummaryIntent {
    /** The sheet opened for [forecast]. Does nothing if that same summary is already done or underway. */
    data class Open(
        val forecast: WeatherForecast,
        val place: String?,
    ) : AiSummaryIntent

    data object Retry : AiSummaryIntent

    /** The sheet closed: stop writing, so a closed sheet does not spend the model's quota. */
    data object Dismiss : AiSummaryIntent
}

/**
 * Drives the weather summary sheet on both platforms: checks once whether the model exists here,
 * runs [SummarizeWeather] when the sheet opens, and maps its results onto [AiSummaryUiState].
 * A finished summary is kept, so reopening the sheet for the same forecast costs nothing.
 */
internal class AiSummaryViewModel(
    private val client: OnDeviceAiClient,
    private val summarizeWeather: SummarizeWeather,
    private val tipLabel: suspend (WeatherAdvice) -> String,
) : ViewModel() {
    constructor(client: OnDeviceAiClient, summarizeWeather: SummarizeWeather) :
        this(client, summarizeWeather, ::localizedTipLabel)

    private val _state = MutableStateFlow(AiSummaryScreenState())
    val state: StateFlow<AiSummaryScreenState> = _state.asStateFlow()

    private var job: Job? = null
    private var request: Request? = null

    init {
        viewModelScope.launch {
            val availability = client.availability()
            _state.update { it.copy(isAvailable = availability != AiAvailability.Unavailable) }
        }
    }

    fun onIntent(intent: AiSummaryIntent) {
        when (intent) {
            is AiSummaryIntent.Open -> open(Request(intent.forecast, intent.place))
            AiSummaryIntent.Retry -> request?.let(::start)
            AiSummaryIntent.Dismiss -> job?.cancel()
        }
    }

    private fun open(next: Request) {
        val summary = _state.value.summary
        val alreadyDone = summary.phase == AiSummaryPhase.Ready && !summary.isStreaming
        if (next.sameAs(request) && (job?.isActive == true || alreadyDone)) return
        start(next)
    }

    private fun start(next: Request) {
        request = next
        job?.cancel()
        _state.update { it.copy(summary = AiSummaryUiState(place = next.place)) }
        job =
            viewModelScope.launch {
                summarizeWeather(next.forecast, next.place)
                    .map { it.toUi(next.place) }
                    .catch { failure -> onSummaryFailed(failure, next.place) }
                    .collect { ui -> _state.update { it.copy(summary = ui) } }
            }
    }

    /**
     * A summary that failed shows the retry state; [AiUnavailableException] also hides the card.
     * Cancellation and non-[Exception] throwables keep propagating.
     */
    private fun onSummaryFailed(
        failure: Throwable,
        place: String?,
    ) {
        if (failure is CancellationException || failure !is Exception) throw failure
        _state.update {
            if (failure is AiUnavailableException) {
                AiSummaryScreenState(isAvailable = false, summary = failed(place))
            } else {
                it.copy(summary = failed(place))
            }
        }
    }

    private fun failed(place: String?) = AiSummaryUiState(phase = AiSummaryPhase.Failed, place = place)

    private suspend fun WeatherSummary.toUi(place: String?) =
        AiSummaryUiState(
            phase = AiSummaryPhase.Ready,
            text = text,
            isStreaming = !isComplete,
            tips = findings.map { AiSummaryTip(it.advice.toKind(), tipLabel(it.advice)) },
            place = place,
        )

    private data class Request(
        val forecast: WeatherForecast,
        val place: String?,
    ) {
        /** The same weather for the same place. `lastUpdated` is a timestamp, not data, so it is ignored. */
        fun sameAs(other: Request?) =
            other != null && place == other.place && forecast.copy(lastUpdated = 0) ==
                other.forecast.copy(
                    lastUpdated = 0,
                )
    }
}

private fun WeatherAdvice.toKind(): AiSummaryTipKind =
    when (this) {
        WeatherAdvice.Umbrella -> AiSummaryTipKind.Umbrella
        WeatherAdvice.Sunscreen -> AiSummaryTipKind.Sunscreen
        WeatherAdvice.Wind -> AiSummaryTipKind.Wind
        WeatherAdvice.Jacket -> AiSummaryTipKind.Jacket
    }

private suspend fun localizedTipLabel(advice: WeatherAdvice): String =
    getString(
        when (advice) {
            WeatherAdvice.Umbrella -> Res.string.ai_tip_umbrella
            WeatherAdvice.Sunscreen -> Res.string.ai_tip_sunscreen
            WeatherAdvice.Wind -> Res.string.ai_tip_wind
            WeatherAdvice.Jacket -> Res.string.ai_tip_jacket
        },
    )
