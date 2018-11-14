package edu.artic.base

import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * @author Sameer Dhakal (Fuzz)
 */
class NetworkException(
        override var message: String,
        override var cause: Throwable? = null
) : PermissibleError(message, cause)

/**
 * This is a non-fatal exception. E.g. Failure to refresh data
 *
 * If something non-fatal happens during data loading process, we throw this kind of error and
 * and it is caught in [edu.artic.splash.SplashActivity].
 */
open class PermissibleError(
        override var message: String,
        override var cause: Throwable? = null) : Exception(message, cause)

/**
 * Tries to convert into NetworkException, if fails returns itself.
 */
fun Throwable.asNetworkException(customErrorMessage: String) : Throwable {
    var exception = this
    if (this is UnknownHostException) {
        exception = NetworkException(customErrorMessage, this)
    } else if (this is SocketTimeoutException) {
        exception = NetworkException(customErrorMessage, this)
    }
    return exception
}