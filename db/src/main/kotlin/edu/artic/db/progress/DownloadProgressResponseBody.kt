package edu.artic.db.progress


import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import java.io.IOException


/**
 * @author Jobin Lawrance
 * @version 1.0.0
 */
class DownloadProgressResponseBody(val downloadIdentifier: String,
                                   val responseBody: ResponseBody,
                                   val downloadProgressListener: DownloadProgressListener) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentLength(): Long = responseBody.contentLength()

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun source(): BufferedSource {
        if (bufferedSource == null)
            bufferedSource = Okio.buffer(getforwardSource(responseBody.source()))
        return bufferedSource!!
    }

    private fun getforwardSource(source: Source): Source =
            object : ForwardingSource(source) {
                var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    downloadProgressListener.update(downloadIdentifier, totalBytesRead, responseBody.contentLength(), bytesRead == -1L)
                    return bytesRead
                }
            }
}