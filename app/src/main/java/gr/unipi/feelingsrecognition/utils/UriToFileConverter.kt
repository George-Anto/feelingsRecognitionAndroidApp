package gr.unipi.feelingsrecognition.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

//Object to convert a file uri to a File object
object UriToFileConverter {

    /**
     * Converts a Uri to a File by opening an InputStream from the Uri,
     * creating a temporary File, and copying the content of the InputStream to the File.
     *
     * @param context           The application context.
     * @param contentResolver   The ContentResolver to open an InputStream from the Uri.
     * @param uri               The Uri to convert to a File.
     * @param fileName          The name of the File object that will be created in the cache memory.
     * @return                  The created File containing the content of the Uri.
     * @throws IOException      If there's an error during InputStream or File operations.
     */
    @Throws(IOException::class)
    fun createFileFromUri(context: Context, contentResolver: ContentResolver, uri: Uri, fileName: String): File {
        //Open an InputStream from the Uri
        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Failed to open InputStream for Uri: $uri")

        //Create a temporary File in the app's cache directory with a unique filename
        val file = File(context.cacheDir, fileName)
        //Copy the contents of the InputStream to the File
        copyInputStreamToFile(inputStream, file)

        return file
    }

    /**
     * Copies the contents of an InputStream to a File.
     *
     * @param inputStream   The InputStream to copy content from.
     * @param file          The destination File to copy content to.
     * @throws IOException  If there's an error during InputStream or File operations.
     */
    @Throws(IOException::class)
    private fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(Constants.KILOBYTES_8)
        var length: Int

        try {
            //Read from the InputStream and write to the File
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            //Flush the OutputStream to ensure all data is written
            outputStream.flush()
        } finally {
            //Close both the InputStream and OutputStream
            inputStream.close()
            outputStream.close()
        }
    }
}