package edu.artic.base

/**
 * @author Sameer Dhakal (Fuzz)
 */
sealed class LoadStatus {
    object Loading : LoadStatus()
    class Error(val error: Throwable) : LoadStatus()
    object None : LoadStatus()
}

