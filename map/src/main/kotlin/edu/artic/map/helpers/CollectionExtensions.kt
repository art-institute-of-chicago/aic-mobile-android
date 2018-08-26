package edu.artic.map.helpers

/**
 * Simple inversion of [Iterable.contains].
 */
fun <T> Iterable<T>.doesNotContain(element: T): Boolean {
    return !this.contains(element)
}

/**
 * Copy of [java.util.Collection.removeIf]. Functionality is identical.
 */
fun <E> MutableCollection<E>.modifyThenRemoveIf(filter: (E) -> Boolean) : Boolean {
    var removed = false
    val each = iterator()
    while (each.hasNext()) {
        if (filter(each.next())) {
            each.remove()
            removed = true
        }
    }
    return removed
}