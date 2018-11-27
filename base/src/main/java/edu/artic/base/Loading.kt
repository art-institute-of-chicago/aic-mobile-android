package edu.artic.base

/**
 * TODO: Maybe move into separate module, along with progress interceptor
 *
 * @author Sameer Dhakal (Fuzz)
 */
sealed class LoadStatus {
    object Loading : LoadStatus()
    class Error(val error: Throwable) : LoadStatus()
    object None : LoadStatus()
}

