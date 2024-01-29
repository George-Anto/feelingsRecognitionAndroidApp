package gr.unipi.feelingsrecognition.utils

import android.content.Context
import java.io.IOException
import java.util.*

//Class that is in charge of loading the credentials.properties file in the program
object LoadPropertiesFile {

    //Returns the API key from the credentials.properties file
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