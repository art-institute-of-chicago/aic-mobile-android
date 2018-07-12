package edu.artic.db.progress


/**
 * @author Jobin Lawrance
 * @version 1.0.0
 */
interface DownloadProgressListener {
    fun update(downloadIdentifier: String, bytesRead: Long, contentLength: Long, done: Boolean)
}