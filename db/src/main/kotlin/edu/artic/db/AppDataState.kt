package edu.artic.db

sealed class ProgressDataState {
    /**
     * Use this for serious exceptions, like [java.io.IOException] or [IllegalStateException].
     */
    class Interrupted(val error: Throwable) : ProgressDataState()
    class Downloading(val progress: Float) : ProgressDataState()
    class Done<T>(val result: T, val headers: Map<String, List<String>> = mapOf()) : ProgressDataState()
    object Empty : ProgressDataState()
}