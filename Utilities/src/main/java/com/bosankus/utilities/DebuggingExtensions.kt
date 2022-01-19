package com.bosankus.utilities

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

object DebuggingExtensions {

    fun logMessage(message: String, tag: String = "DEBUGGING") {
        Log.d(tag, message)
    }

    fun Context.showShortToast(toastMessage: String) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    fun Context.showLongToast(toastMessage: String) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
    }

    fun View.showSnack(message: String) {
        Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
    }
}