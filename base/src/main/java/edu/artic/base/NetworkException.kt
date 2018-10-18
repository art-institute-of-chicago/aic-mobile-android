package edu.artic.base

/**
 * @author Sameer Dhakal (Fuzz)
 */
class NetworkException(
        override var message: String,
        override var cause: Throwable? = null
) : Exception(message, cause)
