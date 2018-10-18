package edu.artic.map

import android.support.annotation.DrawableRes
import edu.artic.db.models.ArticMapAmenityType
import edu.artic.db.models.ArticObject
import timber.log.Timber

/**
 * Description: Returns a drawable resource for the type of [ArticMapAmenityType] specified. If one
 * is not matched up, we return a 0 and log the unknown type.
 */
@DrawableRes
internal fun amenityIconForAmenityType(type: String?): Int {
    return when (type) {
        ArticMapAmenityType.WOMANS_ROOM -> R.drawable.icon_amenity_map_womens_room_blue
        ArticMapAmenityType.MENS_ROOM -> R.drawable.icon_amenity_map_mens_room_blue
        ArticMapAmenityType.ELEVATOR -> R.drawable.icon_amenity_map_elevator_blue
        ArticMapAmenityType.GIFT_SHOP -> R.drawable.icon_amenity_map_shop_blue
        ArticMapAmenityType.TICKETS -> R.drawable.icon_amenity_map_tickets_blue
        ArticMapAmenityType.INFORMATION -> R.drawable.icon_amenity_map_information_blue
        ArticMapAmenityType.CHECK_ROOM -> R.drawable.icon_amenity_map_check_room_blue
        ArticMapAmenityType.AUDIO_GUIDE -> R.drawable.icon_amenity_map_audio_guide_blue
        ArticMapAmenityType.WHEELCHAIR_RAMP -> R.drawable.icon_amenity_map_wheelchair_ramp_blue
        ArticMapAmenityType.DINING -> R.drawable.icon_amenity_map_restaurant_blue
        ArticMapAmenityType.FAMILY_RESTROOM -> R.drawable.icon_amenity_map_family_restroom_blue
        ArticMapAmenityType.MEMBERS_LOUNGE -> R.drawable.icon_amenity_map_cafe_blue
        else -> {
            Timber.d("unknownAmenityType: $type")
            0
        }
    }
}

/**
 * If the passed [displayMode] is [MapDisplayMode.Tour] then we extract what place the [ArticObject]
 * resides in the tourStops. Returns null if not in [MapDisplayMode.Tour] or not found as a stop on the tour.
 */
fun ArticObject.getTourOrderNumberBasedOnDisplayMode(displayMode: MapDisplayMode): String? {
    var order: String? = null
    if (displayMode is MapDisplayMode.Tour) {
        /**
         * If map's display mode is Tour, get the order number of the stop.
         */
        val index = displayMode.tour
                .tourStops
                .indexOfFirst { it.objectId == nid }

        if (index > -1) {
            order = (index + 1).toString()
        }
    }
    return order
}
