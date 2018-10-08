package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.db.Accessiblable
import edu.artic.db.Floor
import edu.artic.ui.util.asCDNUri

@JsonClass(generateAdapter = true)
@Entity
data class ArticMapAnnotation(
        @Json(name = "title") val title: String?,
        @Json(name = "status") val status: String?,
        @Json(name = "nid") @PrimaryKey val nid: String,
        @Json(name = "type") val type: String?,
//        @Json(name = "translations") val translations: List<Any>, TODO: add when the API stops returning empty arrays for this field
        @Json(name = "location") val location: String?,
        @Json(name = "latitude") val latitude: String?,
        @Json(name = "longitude") val longitude: String?,
        @Floor @Json(name = "floor") val floor: Int?,
        @Json(name = "description") val description: String?,
        @Json(name = "label") val label: String?,
        @Json(name = "annotation_type") val annotationType: String?,
        @Json(name = "text_type") val textType: String?,
        @Json(name = "amenity_type") val amenityType: String?,
        @Json(name = "image_filename") val imageFilename: String?,
        @Json(name = "image_url") val imageUrl: String?,
        @Json(name = "image_filemime") val imageFileMime: String?,
        @Json(name = "image_filesize") val imageFileSize: String?,
        @Json(name = "image_width") val imageWidth: String?,
        @Json(name = "image_height") val imageHeight: String?
) : Accessiblable {

    override fun getAccessiblableTitle(): String {
        return title.orEmpty()
    }

    /**
     * Alias for [imageUrl], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val standardImageUrl: String?
        get() {
            return imageUrl?.asCDNUri()
        }
}

class ArticMapAnnotationType {
    companion object {
        const val TEXT = "Text"
        const val AMENITY = "Amenity"
        const val DEPARTMENT = "Department"
    }
}

class ArticMapTextType {
    companion object {
        const val LANDMARK = "Landmark"
        const val SPACE = "Space"
    }
}

class ArticMapAmenityType {
    companion object {
        const val WOMANS_ROOM = "Women's Room"
        const val MENS_ROOM = "Men's Room"
        const val ELEVATOR = "Elevator"
        const val GIFT_SHOP = "Gift Shop"
        const val TICKETS = "Tickets"
        const val INFORMATION = "Information"
        const val CHECK_ROOM = "Check Room"
        const val AUDIO_GUIDE = "Audio Guide"
        const val WHEELCHAIR_RAMP = "Wheelchair Ramp"
        const val DINING = "Dining"
        const val FAMILY_RESTROOM = "Family Restroom"
        const val MEMBERS_LOUNGE = "Members Lounge"

        const val RESTROOMS = "Restrooms"

        fun getAmenityTypes(item: String): List<String> {
            return if (item == ArticMapAmenityType.RESTROOMS) {
                listOf(ArticMapAmenityType.MENS_ROOM, ArticMapAmenityType.WOMANS_ROOM, ArticMapAmenityType.FAMILY_RESTROOM)
            } else {
                listOf(item)
            }
        }


        fun titleFor(info: ArticGeneralInfo.Translation, amenityType: String): String {
            return when (amenityType) {
                GIFT_SHOP -> info.giftShopsTitle
                MEMBERS_LOUNGE -> info.membersLoungeTitle
                RESTROOMS -> info.restroomsTitle
                else -> amenityType
            }
        }

        fun textFor(info: ArticGeneralInfo.Translation, amenityType: String): String {
            return when (amenityType) {
                GIFT_SHOP -> info.giftShopsText
                MEMBERS_LOUNGE -> info.membersLoungeText
                RESTROOMS -> info.restroomsText
                // Hardcoded english default so we don't crash
                else -> "Close to explore the map."
            }
        }

    }
}

