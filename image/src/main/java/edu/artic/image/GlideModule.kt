package edu.artic.image

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream


/**
 * This is the app's implementation of [our image-loading library's contract][AppGlideModule].
 *
 * Whenever code needs to load an image from a remote server, it should do so by
 * calling [one of the Glide.with() overloads][com.bumptech.glide.Glide.with]. This
 * file defines default values for those loading operations via [applyOptions].
 *
 * Note: the [GlideModule] annotation on this class is used by Glide's annotation
 * processor during the build. There is no need for other source files to reference
 * this file.
 *
 * @author Sameer Dhakal (Fuzz)
 */
@GlideModule
@Suppress("unused")
class GlideModule : AppGlideModule() {

    // NB: 'appcontext' and 'builder' are guaranteed non-null, based on analysis of sources for Glide 4.4.0
    override fun applyOptions(appContext: Context, builder: GlideBuilder) {
        if (BuildConfig.DEBUG) {
            // 'INFO' includes 'ASSERT', 'ERROR', 'WARN' (call failed), and 'INFO' (call stacktrace) messages
            builder.setLogLevel(Log.INFO)
        } else {
            builder.setLogLevel(Log.ERROR)
        }

        builder.setDefaultRequestOptions(
                RequestOptions()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .downsample(MemoryOptimizedDownsampleStrategy)
        )

    }

    override fun registerComponents(context: Context, glide: Glide?, registry: Registry?) {
        /**
         * Configuration to use OkHttp client as the network client for Glide.
         * OkHttp is also configured here to cache the image.
         * Glide doesn't show the latest image if its from the same URL.
         * OkHttp solves this problem by caching the image considering E-Tag and Last-Modified headers.
         *
         * @see https://github.com/bumptech/glide/issues/1847
         */
        val cacheSize: Long = 10 * 1024 * 1024 // 10 MiB
        val clientBuilder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
        }
        val client = clientBuilder.cache(Cache(context.cacheDir, cacheSize)).build()
        val okHttpFactory = OkHttpUrlLoader.Factory(client)

        registry?.replace(GlideUrl::class.java, InputStream::class.java, okHttpFactory)

    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}


/**
 * A simple memory-aware strategy based on 'center outside'.
 *
 * Note this strategy only comes into effect if the source image and target
 * ImageView are different sizes. To change the resolution of properly-sized
 * resources, you'll need to use a new [DownsampleStrategy] and implement
 * [DownsampleStrategy.getScaleFactor] accordingly.
 *
 * [MemoryOptimizedDownsampleStrategy.getScaleFactor] simply calls the method
 * of the same name on [CENTER_OUTSIDE][DownsampleStrategy.CENTER_OUTSIDE]. The
 * intended effect is for each image to be cropped in the larger dimension, so
 * that every part of the [load target][SimpleTarget] is covered by some part of
 * the image.
 *
 * As of Glide 4.4, the [default strategy][DownsampleStrategy.DEFAULT] was set to
 * `CENTER_OUTSIDE` anyway, so our only distinction in that version is the use of
 * [DownsampleStrategy.SampleSizeRounding.MEMORY].
 */
object MemoryOptimizedDownsampleStrategy : DownsampleStrategy() {
    override fun getScaleFactor(sourceWidth: Int, sourceHeight: Int, requestedWidth: Int, requestedHeight: Int): Float {
        return DownsampleStrategy.CENTER_OUTSIDE.getScaleFactor(sourceWidth, sourceHeight, requestedWidth, requestedHeight)
    }

    override fun getSampleSizeRounding(sourceWidth: Int, sourceHeight: Int, requestedWidth: Int, requestedHeight: Int): SampleSizeRounding {
        return SampleSizeRounding.MEMORY
    }
}
