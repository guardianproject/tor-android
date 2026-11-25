package org.torproject.jni

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

@Suppress("unused")
object GeoipUtils {

    /**
     * Installs geoip and geoip6 files from the version of tor that this version of tor-android is
     * built with. These are installed into the app's private internal directory. This overwrites any
     * existing geoip/geoip6 files, so it's recommended to only run this method on app upgrade or if
     * the files are missing.
     */
    @Suppress("unused")
    @JvmStatic
    @Throws(IOException::class)
    fun overwriteGeoIPFiles(context: Context) {
        Log.d(TorService.TAG, "Installing geoip configuration for tor-android ${BuildConfig.VERSION_NAME}")
        writeRawResourceToFile(context, FILENAME_GEOIP)
        writeRawResourceToFile(context, FILE_NAME_GEOIP6)
    }

    // geoip files are packaged as R.raw resources, instead of assets. When using assets, other libraries
    // or the existing app may overwrite identically named files. We need to write them to disk in such
    // a way that tor can open a file descriptor to them in C
    private fun writeRawResourceToFile(context: Context, filename: String) {
        val resId = if (filename == FILENAME_GEOIP) R.raw.geoip else R.raw.geoip6
        context.resources.openRawResource(resId).use { inputStream ->
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * Returns a File object for geoip
     */
    @JvmStatic
    fun getGeoIPFile(context: Context) : File =
        File(context.filesDir, FILENAME_GEOIP)

    /**
     * Returns a file object for geoip6
     */
    @JvmStatic
    fun getGeoIP6File(context: Context): File =
        File(context.filesDir, FILE_NAME_GEOIP6)

    /**
     * Returns a string that can be used in torrc telling tor to use geoip
     */
    @Suppress("unused")
    @JvmStatic
    fun getGeoIPTorrcString(context: Context) : String  =
        "$TORRC_GEOIP ${getGeoIPFile(context).canonicalPath}"

    /**
     * Returns a string that can be used in torrc telling tor to use geoip6
     */
    @Suppress("unused")
    @JvmStatic
    fun getGeoIP6TorrcString(context: Context) : String =
        "$TORRC_GEOIP6 ${getGeoIP6File(context).canonicalPath}"

    private const val FILENAME_GEOIP = "geoip"
    private const val FILE_NAME_GEOIP6 = "geoip6"
    private const val TORRC_GEOIP = "GeoIPFile"
    private const val TORRC_GEOIP6 = "GeoIPv6File"

}
