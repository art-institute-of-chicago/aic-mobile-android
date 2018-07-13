package edu.artic.db

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi


inline fun getMoshi(configureBlock: (Moshi.Builder.() -> Unit) = {}): Moshi =
        Moshi.Builder()
                .registerAdapters()
                .apply { configureBlock(this) }
                .add(KotlinJsonAdapterFactory())
                .build()

/**
 * Used to explicitly reference without needing to inject it into [SCError] for convenience.
 */
internal val apiErrorMoshi: Moshi = getMoshi()

fun Moshi.Builder.registerAdapters() = apply {
    add(ApiJsonAdapterFactory.INSTANCE)
}