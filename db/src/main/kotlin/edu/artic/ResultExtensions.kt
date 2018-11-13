package edu.artic

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.base.utils.orIfNullOrBlank
import org.json.JSONObject

/**
 * This file houses the extension functions for [com.jakewharton.retrofit2.adapter.rxjava2.Result]
 * @author Sameer Dhakal (Fuzz)
 */


/**
 * Extension method to parse the error message from the JSON.
 * Only works iff the response body can be converted to JSONObject.
 * Looks for error, message and detail keys in order.
 */
fun Result<*>.getErrorMessage(): String? {
    var errorMessage: String? = null
    try {
        this.response().errorBody()?.also { errorBody ->
            val errorJSON = JSONObject(errorBody.string())
            errorMessage = errorJSON.optString("error")
                    .orIfNullOrBlank(errorJSON.optString("message"))
                    .orIfNullOrBlank(errorJSON.optString("Message"))
                    .orIfNullOrBlank(errorJSON.optString("detail"))
                    .orIfNullOrBlank("")
        }
    } catch (exception: Exception) {
        exception.printStackTrace()
    }
    return errorMessage
}

/**
 * Throws the response exception.
 */
fun Result<*>.throwIfError(): Result<*> {
    return if (this.isError) {
        throw this.error()
    } else {
        this
    }
}
