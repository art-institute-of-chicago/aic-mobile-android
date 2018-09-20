package edu.artic.map.rendering

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.asFlowable
import com.fuzz.rx.disposedBy
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticObject
import edu.artic.image.asRequestObservable
import edu.artic.image.loadWithThumbnail
import edu.artic.map.*
import edu.artic.map.helpers.toLatLng
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao)
    : MapItemRenderer<ArticObject>(useBitmapQueue = true) {

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    // The rate we check for changes from the incoming stream. (it can update faster).
    private val visibleRegionSampleRate = 500L

    private val loadingBitmap by lazy {
        BitmapDescriptorFactory.fromBitmap(
                articObjectMarkerGenerator.makeIcon(null, scale = .7f))
    }
    private val scaledDot by lazy {
        BitmapDescriptorFactory.fromBitmap(
                articObjectMarkerGenerator.makeIcon(null, scale = .15f))
    }

    init {
        visibleRegionChanges
                .sample(visibleRegionSampleRate, TimeUnit.MILLISECONDS) // too many events, prevent flooding.
                .withLatestFrom(mapItems, mapChangeEvents)
                .filter { (_, _, mapEvent) -> mapEvent.displayMode !is MapDisplayMode.Tour }
                .toFlowable(BackpressureStrategy.LATEST) // if downstream can't keep up, let's pick latest.
                .subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (region, mapItems, mapChangeEvent) ->
                    Timber.d("Evaluating Markers For Visible Region Change $region")
                    mapItems.values.forEach { markerHolder ->
                        adjustVisibleMarker(markerHolder, region, mapChangeEvent)
                    }
                }
                .disposedBy(imageQueueDisposeBag)
    }


    /**
     * This method adjusts all visible [ArticObject] mapitems on screen based on [VisibleRegion] changes.
     *
     * If the marker [isCloseEnoughToCenter], we display a loading indicator and load the image on a separate thread.
     * We shouldn't hold the image in memory, so we must reload the image after it's been collapsed.
     */
    override fun adjustVisibleMarker(
            existing: MarkerHolder<ArticObject>,
            visibleRegion: VisibleRegion,
            mapChangeEvent: MapChangeEvent) {
        val position = getAdjustedLocationFromItem(existing.item)
        val meta = existing.marker.metaData()
                ?: MarkerMetaData(existing.item, loadedBitmap = false, requestDisposable = null)

        if (mapChangeEvent.displayMode !is MapDisplayMode.CurrentFloor
                || position.isCloseEnoughToCenter(visibleRegion.latLngBounds)) {
            if (!meta.loadedBitmap) {
                // show loading while its loading.
                existing.marker.setIcon(loadingBitmap)

                // enqueue replacement and add to marker meta.
                existing.marker.tag = meta.copy(
                        requestDisposable = enqueueBitmapFetch(item = existing.item, mapChangeEvent = mapChangeEvent))
            }
        } else {
            // reset loading state here.
            existing.marker.apply {
                meta.requestDisposable?.let { existing -> imageQueueDisposeBag.remove(existing) }
                tag = meta.copy(loadedBitmap = false, requestDisposable = null)
                setIcon(scaledDot)
            }
        }
    }

    override fun displayMarker(
            item: ArticObject,
            mapChangeEvent: MapChangeEvent,
            visibleRegion: VisibleRegion,
            displayMode: MapDisplayMode,
            map: GoogleMap,
            floor: Int,
            id: String
    ): MarkerHolder<ArticObject>? {
        val position = getAdjustedLocationFromItem(item)
        var requestDisposable: Disposable? = null

        // slightly different logic than the above method. If close enough to center or on tour, enqueue and show loading.
        // otherwise just show dot.
        val fastBitmap = if (displayMode !is MapDisplayMode.CurrentFloor ||
                position.isCloseEnoughToCenter(visibleRegion.latLngBounds)) {
            requestDisposable = enqueueBitmapFetch(item = item, mapChangeEvent = mapChangeEvent)
            loadingBitmap
        } else {
            scaledDot
        }

        // fast bitmap returns immediately.
        return constructAndAddMarkerHolder(
                item = item,
                bitmap = fastBitmap,
                displayMode = displayMode,
                map = map,
                floor = floor,
                id = id,
                // bitmap queue not used, means bitmap is considered loaded
                loadedBitmap = !useBitmapQueue,
                requestDisposable = requestDisposable)
    }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<ArticObject>> = when (displayMode) {
        is MapDisplayMode.CurrentFloor -> objectsDao.getObjectsByFloor(floor = floor)
        is MapDisplayMode.Tour -> objectsDao.getObjectsByIdList(displayMode.tour.tourStops.mapNotNull { it.objectId })
        is MapDisplayMode.Search.ObjectSearch -> {
            if (displayMode.item.backingObject != null) {
                listOf(displayMode.item.backingObject as ArticObject).asFlowable()
            } else {
                listOf<ArticObject>().asFlowable()
            }
        }
        is MapDisplayMode.Search.AmenitiesSearch -> listOf<List<ArticObject>>().toFlowable()
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            when (displayMode) {
                is MapDisplayMode.Tour -> MapFocus.values().toSet()
                is MapDisplayMode.Search<*> -> MapFocus.values().toSet()
                else -> setOf(MapFocus.Individual)
            }

    override fun getLocationFromItem(item: ArticObject): LatLng = item.toLatLng()

    override fun getIdFromItem(item: ArticObject): String = item.nid

    override fun getFastBitmap(item: ArticObject, displayMode: MapDisplayMode): BitmapDescriptor? =
            loadingBitmap

    override fun getBitmapFetcher(item: ArticObject, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? {
        val imageSize = context.resources.getDimension(R.dimen.artic_object_map_image_size).toInt()
        return Glide.with(context)
                .asBitmap()
                .apply(RequestOptions().disallowHardwareConfig())
                .loadWithThumbnail(
                        item.thumbUrl,
                        // Prefer standard 'image_url', fall back to 'large image' if necessary.
                        item.standardImageUrl ?: item.largeImageUrl
                )
                .asRequestObservable(context,
                        width = imageSize,
                        height = imageSize)
                .map { bitmap ->
                    val order: String? = item.getTourOrderNumberBasedOnDisplayMode(displayMode)
                    BitmapDescriptorFactory.fromBitmap(
                            articObjectMarkerGenerator.makeIcon(bitmap, overlay = order))
                }
    }

    override val zIndex: Float = 2.0f

    override fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: ArticObject): Float {
        // on tour, set the alpha depending on current floor.
        return if (mapDisplayMode is MapDisplayMode.Tour || mapDisplayMode is MapDisplayMode.Search.ObjectSearch) {
            if (item.floor == floor) ALPHA_VISIBLE else ALPHA_DIMMED
        } else {
            ALPHA_VISIBLE
        }
    }
}
