package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticMessage(
        @PrimaryKey var nid: String = "",
        @Json(name = "title") val title: String?,
        @Json(name = "message_type") val messageType: String?,
        @Json(name = "expiration_threshold") val expirationThreshold: Int?,
        @Json(name = "tour_exit") val tourExit: String?,
        @Json(name = "persistent") val isPersistent: Boolean?,
        @Json(name = "message") val message: String?,
        @Json(name = "action") val action: String?,
        @Json(name = "action_title") val actionTitle: String?
) : Parcelable
