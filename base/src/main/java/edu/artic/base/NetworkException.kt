package edu.artic.base

/**
 * @author Sameer Dhakal (Fuzz)
 */
class NetworkException(
        override var message: String? = null,
        override var cause: Throwable
) : PermissibleException(message, cause)

/**
 * Exceptions that do not create the app in an unstable state.
 * E.g. Network exceptions are PermissibleException.
 * If this exception is encountered during data loading process, user is allowed to use
 * the app (i.e. app is not stuck at splash screen). All other exceptions are not permitted.
 *
 * @see [edu.artic.splash.SplashActivity]
 */
open class PermissibleException(
        override var message: String? = null,
        override var cause: Throwable) : Exception(message, cause)