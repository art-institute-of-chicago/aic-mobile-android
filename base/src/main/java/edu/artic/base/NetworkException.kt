package edu.artic.base

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