package edu.artic.viewmodel

sealed class Navigate<out T> {
    class Forward<out T>(val endpoint: T) : Navigate<T>()
    class Back<out T> : Navigate<T>()

}