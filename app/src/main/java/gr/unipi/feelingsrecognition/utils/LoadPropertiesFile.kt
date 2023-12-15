package gr.unipi.feelingsrecognition.utils

import android.content.Context
import java.io.IOException
import java.util.*

object LoadPropertiesFile {

    fun loadApiKey(context: Context): String {
        val properties = Properties()

        try {
            val inputStream = context.assets.open("credentials.properties")
            properties.load(inputStream)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return properties.getProperty("youtube.api.key", "")
    }
}