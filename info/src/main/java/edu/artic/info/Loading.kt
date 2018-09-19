package edu.artic.info

/**
 * @author Sameer Dhakal (Fuzz)
 */
sealed class LoadStatus {
    object Loading : LoadStatus()
    class Error(val error: Throwable) : LoadStatus()
    object None : LoadStatus()
}

