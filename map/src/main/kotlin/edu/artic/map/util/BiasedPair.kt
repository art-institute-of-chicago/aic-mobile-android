package edu.artic.map.util

/**
 * Like [Pair], except this class is biased towards its first value.
 *
 * Much like a [Map], you could say.
 *
 * For best results, [first] should be a data class too.
 */
data class BiasedPair<T, U>(val first : T, val second : U) {
    override fun equals(other: Any?): Boolean {
        return other is BiasedPair<*,*> && first == other.first
    }

    override fun hashCode(): Int {
        return first?.hashCode() ?: 0
    }
}
