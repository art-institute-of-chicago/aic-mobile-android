package edu.artic.db.progress


import edu.artic.db.progress.ApiConstants.Companion.DOWNLOAD_IDENTIFIER_HEADER
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * @author Jobin Lawrance
 * @version 1.0.0
 */
class DownloadProgressInterceptor(val progressEventBus: ProgressEventBus) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val responseBuilder = originalResponse.newBuilder()

        val downloadIdentifier: String? = originalResponse.request().header(DOWNLOAD_IDENTIFIER_HEADER)

        if (!downloadIdentifier.isNullOrEmpty()) {

            val downloadProgressListener = object : DownloadProgressListener {
                override fun update(downloadIdentifier: String, bytesRead: Long, contentLength: Long, done: Boolean) {
                    progressEventBus.post(ProgressEvent(downloadIdentifier, contentLength, bytesRead))
                }
            }

            val downloadResponseBody = DownloadProgressResponseBody(downloadIdentifier!!, originalResponse.body()!!, downloadProgressListener)

            responseBuilder.body(downloadResponseBody)
        }

        return responseBuilder.build()
    }
}