package edu.artic.welcome

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions


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
        super.applyOptions(appContext, builder)
        builder.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))

    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
