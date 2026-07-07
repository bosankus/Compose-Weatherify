package bose.ankush.home.presentation.util

import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.city_error_txt
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.network_timeout_txt
import bose.ankush.home.generated.resources.network_unavailable_txt
import bose.ankush.home.generated.resources.server_error_txt
import bose.ankush.home.generated.resources.unauthorised_access_txt
import bose.ankush.network.util.NetworkException
import org.jetbrains.compose.resources.getString

internal suspend fun errorMessageFromException(exception: Exception): String =
    when (exception) {
        is NetworkException -> errorMessageForCode(exception.errorCode)
        else -> getString(Res.string.general_error_txt)
    }

private suspend fun errorMessageForCode(errorCode: Int): String =
    when (errorCode) {
        NetworkException.BAD_REQUEST -> getString(Res.string.city_error_txt)
        NetworkException.UNAUTHORIZED -> getString(Res.string.unauthorised_access_txt)
        NetworkException.FORBIDDEN -> getString(Res.string.unauthorised_access_txt)
        NetworkException.NOT_FOUND -> getString(Res.string.city_error_txt)
        NetworkException.SERVER_ERROR -> getString(Res.string.server_error_txt)
        NetworkException.SERVICE_UNAVAILABLE -> getString(Res.string.server_error_txt)
        NetworkException.NETWORK_UNAVAILABLE -> getString(Res.string.network_unavailable_txt)
        NetworkException.TIMEOUT -> getString(Res.string.network_timeout_txt)
        NetworkException.UNKNOWN_HOST -> getString(Res.string.network_unavailable_txt)
        else -> getString(Res.string.general_error_txt)
    }
