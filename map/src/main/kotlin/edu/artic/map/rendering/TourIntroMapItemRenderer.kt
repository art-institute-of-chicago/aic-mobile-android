package edu.artic.map.rendering

import com.bumptech.glide.Glide
import com.fuzz.rx.asFlowable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import edu.artic.db.INTRO_TOUR_STOP_OBJECT_ID
import edu.artic.db.debug
import edu.artic.db.models.ArticTour
import edu.artic.image.asRequestObservable
import edu.artic.map.ArticObjectMarkerGenerator
import edu.artic.map.MapDisplayMode
import edu.artic.map.MapFocus
import edu.artic.map.helpers.toLatLng
import edu.artic.ui.util.asCDNUri
import io.reactivex.Flowable
import io.reactivex.Observable

class TourIntroMapItemRenderer : MapItemRenderer<ArticTour>(useBitmapQueue = true) {

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> = when (displayMode) {
        is MapDisplayMode.Tour -> MapFocus.values().toSet()
        else -> setOf()
    }

    override val zIndex: Float = 2.0f

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticTour>> = when (displayMode) {
        is MapDisplayMode.Tour -> listOf(displayMode.tour).asFlowable()
        else -> listOf<ArticTour>().asFlowable()
    }

    override fun getBitmapFetcher(item: ArticTour, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? =
            Glide.with(context)
                    .asBitmap()
                    .load(item.thumbUrl)
                    .asRequestObservable(context)
                    .debug("Glide loading: ${item.title}")
                    .map { bitmap ->
                        BitmapDescriptorFactory.fromBitmap(
                                articObjectMarkerGenerator.makeIcon(bitmap))
                    }


    override fun getLocationFromItem(item: ArticTour): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticTour): String = INTRO_TOUR_STOP_OBJECT_ID

    override fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: ArticTour): Float {
        // on tour, set the alpha depending on current floor.
        return if (mapDisplayMode is MapDisplayMode.Tour) {
            if (item.floorAsInt == floor) ALPHA_VISIBLE else ALPHA_DIMMED
        } else {
            ALPHA_VISIBLE
        }
    }
}