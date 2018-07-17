package edu.artic.db

sealed class ProgressDataState {
    class Downloading(val progress: Float) : ProgressDataState()
    class Done<T>(val result: T, val headers: Map<String, List<String>> = mapOf()) : ProgressDataState()
    object Empty : ProgressDataState()
}