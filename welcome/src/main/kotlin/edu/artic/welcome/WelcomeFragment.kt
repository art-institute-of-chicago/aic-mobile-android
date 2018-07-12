package edu.artic.welcome

import android.content.res.AssetManager
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.app_bar_layout.view.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {
    override val title: String
        get() = "Welcome"

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    @Inject
    lateinit var moshi: Moshi


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * TODO:: move this logic away into the app bar view class
         * TODO:: Make a Custom AppBar view that dynamically switches the toolbar type (collapsible and non collapsible)
         */
        val appBar = appBarLayout as AppBarLayout
        (appBarLayout as AppBarLayout).apply {
            addOnOffsetChangedListener { aBarLayout, verticalOffset ->
                val progress: Double = 1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
                appBar.searchIcon.background.alpha = (progress * 255).toInt()
                appBar.flagIcon.drawable.alpha = (progress * 255).toInt()
            }
        }

        val list = getTours()
        Log.d("test", "${list.size}")
        context?.let {
            tourSummaryRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            tourSummaryRecyclerView.adapter = ToursAdapter(list, it)
        }

    }


    private fun getTours(): List<ArticTour> {

        return activity.let {
            if (it == null) {
                emptyList<ArticTour>()
            } else {
                val toursJson = it.assets.fileAsString("json", "tours.json")
                val adapter: JsonAdapter<List<ArticTour>> = moshi.adapter(Types.newParameterizedType(List::class.java, ArticTour::class.java))
                return@let adapter.fromJson(toursJson) as List<ArticTour>
            }

        }


    }

}

fun AssetManager.fileAsString(subdirectory: String, filename: String): String {
    return open("$subdirectory/$filename").use {
        it.readBytes().toString(Charset.defaultCharset())
    }
}