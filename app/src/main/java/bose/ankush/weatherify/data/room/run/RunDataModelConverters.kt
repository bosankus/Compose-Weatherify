package bose.ankush.weatherify.data.room.run

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

/**
 * Created by Androidplay
 * Author: Ankush
 * On: 8/14/2023, 01:23 AM
 */
class RunDataModelConverters {

    @TypeConverter
    fun toBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    @TypeConverter
    fun fromBitmap(bmpImage: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmpImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}