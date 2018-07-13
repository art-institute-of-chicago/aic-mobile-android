package edu.artic.db

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


inline fun getMoshi(configureBlock: (Moshi.Builder.() -> Unit) = {}): Moshi =
        Moshi.Builder()
                .registerAdapters()
                .apply { configureBlock(this) }
                .build()

/**
 * Used to explicitly reference without needing to inject it into [SCError] for convenience.
 */
internal val apiErrorMoshi: Moshi = getMoshi()

fun Moshi.Builder.registerAdapters() = apply {
//    add(ApiJsonAdapterFactory.INSTANCE)
    add(KotlinJsonAdapterFactory())
}