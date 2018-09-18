package edu.artic.info

/**
 * @author Sameer Dhakal (Fuzz)
 */
sealed class LoadStatus {
    class Loading : LoadStatus()
    class Error(val error: Throwable) : LoadStatus()
    class None : LoadStatus()
}

