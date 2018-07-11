package edu.artic.db

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import javax.inject.Qualifier


@Target(allowedTargets = [AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER])
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class AdapterFactory

class JsonAdapterFactory(private val creators: Set<JsonAdapter.Factory>) {

    fun applyTo(moshi: Moshi.Builder) {
        creators.forEach { moshi.add(it) }
    }
}