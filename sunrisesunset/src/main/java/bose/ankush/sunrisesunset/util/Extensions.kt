package bose.ankush.sunrisesunset.util

import android.annotation.SuppressLint
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
fun Long.getFormattedDate(format:String):String{
    val timeStamp = Timestamp(this)
    val date = Date(timeStamp.time)

    return SimpleDateFormat(format).format(date)
}