package bose.ankush.weatherify.presentation.run

import androidx.lifecycle.ViewModel
import bose.ankush.weatherify.domain.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RunViewModel @Inject constructor(
    private val runRepository: RunRepository
): ViewModel() {

    // TODO: operations related to user's run
}