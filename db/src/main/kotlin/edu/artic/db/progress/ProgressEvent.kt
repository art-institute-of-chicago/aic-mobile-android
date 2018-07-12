package edu.artic.db.progress

/**
 * @author Jobin Lawrance
 * @version 1.0.0
 */
data class ProgressEvent private constructor(val progress: Int,
                                             val contentLength: Long,
                                             val downloadIdentifier: String,
                                             val bytesRead: Long,
                                             val percentIsAvailable: Boolean) {

    constructor(downloadIdentifier: String, contentLength: Long, bytesRead: Long) :
            this(
                    progress = (bytesRead / (contentLength / 100f)).toInt(), //shown in percent
                    contentLength = contentLength,
                    downloadIdentifier = downloadIdentifier,
                    bytesRead = bytesRead,
                    percentIsAvailable = contentLength > 0)
}