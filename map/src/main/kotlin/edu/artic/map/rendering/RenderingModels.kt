package edu.artic.map.rendering

import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import edu.artic.map.MapChangeEvent
import io.reactivex.disposables.Disposable

/**
 * Holds meta information related to [Marker].
 *
 * @param requestDisposable if non-null, the active request [Disposable] that we can cancel.
 */
data class MarkerMetaData<T>(val item: T,
                             val loadedBitmap: Boolean = false,
                             val requestDisposable: Disposable? = null)

/**
 * Useful extension that casts the [Marker.getTag] into a [MarkerMetaData]. Might be null.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Marker.metaData(): MarkerMetaData<T>? = tag as MarkerMetaData<T>?

/**
 * Holder class that keeps a reference to our [item], a unique [id], and the rendered [marker] so
 * we can clear it out later.
 */
data class MarkerHolder<T>(val id: String,
                           val item: T,
                           val marker: Marker)

/**
 * Holder class for change events, including [GoogleMap], [MapChangeEvent], and [items].
 */
data class MapItemRendererEvent<T>(val map: GoogleMap, val mapChangeEvent: MapChangeEvent, val items: List<T>)

/**
 * This class is used when we load [BitmapDescriptor] asynchronously, awaiting callbacks from [Glide].
 * We keep reference to the [mapChangeEvent] and [item] so we can construct the [MarkerOptions] correctly.
 */
data class DelayedMapItemRenderEvent<T>(val mapChangeEvent: MapChangeEvent,
                                        val item: T,
                                        val bitmap: BitmapDescriptor)