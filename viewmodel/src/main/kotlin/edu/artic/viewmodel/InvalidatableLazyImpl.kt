package edu.artic.viewmodel

import java.io.Serializable
import kotlin.reflect.KProperty

private object UNINITIALIZED_VALUE

/**
 * Adds the ability to clear out the lazy implementation when needed.
 */
@Suppress("UNCHECKED_CAST")
class InvalidatableLazyImpl<T>(private val initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = lock ?: this
    fun invalidate() {
        _value = UNINITIALIZED_VALUE
    }

    override val value: T
        get() {
            val v = _value
            return if (v !== UNINITIALIZED_VALUE) {
                v as T
            } else synchronized(lock) {
                val synchronizedV = _value
                if (synchronizedV !== UNINITIALIZED_VALUE) {
                    synchronizedV as T
                } else {
                    val typedValue = initializer()
                    _value = typedValue
                    typedValue
                }
            }
        }


    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        _value = t
    }
}

fun <T> invalidatableLazy(initializer: () -> T) = InvalidatableLazyImpl(initializer)