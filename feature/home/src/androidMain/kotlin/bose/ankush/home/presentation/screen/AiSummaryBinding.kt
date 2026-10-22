package bose.ankush.home.presentation.screen

import bose.ankush.home.presentation.ai.AiSummaryUiState

/** What the summary card and sheet need from [bose.ankush.home.presentation.ai.AiSummaryViewModel]. */
internal class AiSummaryBinding(
    val state: AiSummaryUiState = AiSummaryUiState(),
    /** False hides the home card entirely: no model on this device, or no forecast to summarize yet. */
    val isAvailable: Boolean = false,
    /** The sheet just opened: start generating. */
    val onOpen: () -> Unit = {},
    /** The user tapped "Try again" after a failure. */
    val onRetry: () -> Unit = {},
    /** The sheet closed: stop generating. */
    val onDismiss: () -> Unit = {},
)
