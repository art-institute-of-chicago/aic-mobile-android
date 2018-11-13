package edu.artic.base

/**
 * @author Sameer Dhakal (Fuzz)
 */
class NetworkException(
        override var message: String,
        override var cause: Throwable? = null
) : PermissibleError(message, cause)

/**
 * Exceptions that do not create the app in an unstable state.
 * E.g. Network exceptions are PermissibleError.
 * If this exception is encountered during data loading process, user is allowed to use
 * the app (i.e. app is not stuck at splash screen). All other exceptions are not permitted.
 *
 * @see [edu.artic.splash.SplashActivity]
 */
open class PermissibleError(
        override var message: String,
        override var cause: Throwable? = null) : Exception(message, cause)