package edu.artic.welcome

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions


/**
 * @author Sameer Dhakal (Fuzz)
 */
@GlideModule
class GlideModule : AppGlideModule() {

    override fun applyOptions(context: Context?, builder: GlideBuilder?) {
        super.applyOptions(context, builder)
        builder?.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))

    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
