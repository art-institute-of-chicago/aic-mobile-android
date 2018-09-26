package edu.artic.map.rendering

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.map.MapDisplayMode
import edu.artic.map.getTourOrderNumberBasedOnDisplayMode
import edu.artic.map.helpers.toLatLng

class MapItemModel(
        val id: String,
        val floor: Int,
        val thumbURL: String,
        val imageURL: String,
        val backingObject: ArticObject?,
        val latLng: LatLng) {
    companion object {
        fun fromArticObject(item: ArticObject): MapItemModel {
            return MapItemModel(item.nid,
                    item.floor,
                    item.thumbUrl ?: "",
                    item.standardImageUrl ?: item.largeImageUrl ?: "",
                    item,
                    item.toLatLng())
        }

        fun fromExhibition(item: ArticExhibition): MapItemModel {
            return MapItemModel(item.id.toString(),
                    item.floor ?: 1,
                    item.legacy_image_mobile_url ?: "",
                    item.legacy_image_mobile_url ?: "",
                    null,
                    item.toLatLng())
        }
    }

    fun getTourOrderNumberBasedOnDisplayMode(mode: MapDisplayMode): String? {
        backingObject?.let {
            return backingObject.getTourOrderNumberBasedOnDisplayMode(mode)
        }
        return "1"
    }
}