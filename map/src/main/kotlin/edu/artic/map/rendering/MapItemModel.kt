package edu.artic.map.rendering

import com.google.android.gms.maps.model.LatLng
import edu.artic.db.AccessibilityAware
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.map.MapDisplayMode
import edu.artic.map.getTourOrderNumberBasedOnDisplayMode
import edu.artic.map.helpers.toLatLng

class MapItemModel(
        val id: String,
        val title: String,
        val floor: Int,
        val thumbURL: String,
        val imageURL: String,
        val backingObject: ArticObject?,
        val latLng: LatLng) : AccessibilityAware {

    override fun getContentDescription(): String {
        return title
    }

    companion object {
        fun fromArticObject(item: ArticObject): MapItemModel {
            return MapItemModel(item.nid,
                    item.title,
                    item.floor,
                    item.thumbUrl ?: "",
                    item.standardImageUrl ?: item.largeImageUrl ?: "",
                    item,
                    item.toLatLng())
        }

        fun fromArticSearchArtwork(item: ArticSearchArtworkObject): MapItemModel {
            return MapItemModel(item.artworkId,
                    item.title,
                    item.floor,
                    item.thumbUrl ?: "",
                    item.largeImageUrl ?: "",
                    item.backingObject,
                    item.toLatLng())
        }

        fun fromExhibition(item: ArticExhibition): MapItemModel {
            return MapItemModel(item.id.toString(),
                    item.title,
                    item.floor ?: 1,
                    item.legacyImageUrl ?: "",
                    item.legacyImageUrl ?: "",
                    null,
                    item.toLatLng())
        }
    }

    fun getTourOrderNumberBasedOnDisplayMode(mode: MapDisplayMode): String? {
        backingObject?.let {
            return backingObject.getTourOrderNumberBasedOnDisplayMode(mode)
        }
        return null
    }

    fun isObject(articObject: ArticObject?): Boolean {
        return articObject?.nid == id
    }
}