package edu.artic.db

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import javax.inject.Qualifier


@Target(allowedTargets = [AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER])
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class AdapterFactory

// TODO: Add javadoc, rename file. Where are the `creators` defined?
class JsonAdapterFactory(private val creators: Set<JsonAdapter.Factory>) {

    fun applyTo(moshi: Moshi.Builder) {
        // TODO: Can we inline these calls?
        creators.forEach { moshi.add(it) }
    }
}