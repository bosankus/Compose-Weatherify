package bose.ankush.weatherify.logger

import java.io.File

class FileLogger {

    fun logError(message: String) {
        val file = File("error.txt")
        file.appendText(
            text = message
        )
    }
}