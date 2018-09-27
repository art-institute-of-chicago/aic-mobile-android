package edu.artic.details

import android.os.Bundle
import edu.artic.events.EventDetailFragment
import edu.artic.tours.TourDetailsFragment
import edu.artic.ui.BaseActivity

class DetailsActivity: BaseActivity() {
    override val layoutResId: Int
        get() = R.layout.activity_details

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent.extras[EventDetailFragment.ARG_EVENT] != null -> {
                navController.navigate(R.id.goToEventDetails)
            }
            intent.extras[TourDetailsFragment.ARG_TOUR] != null -> {
                navController.navigate(R.id.goToTourDetails)
            }
            else -> {
                navController.navigate(R.id.goToExhibitionDetails)
            }
        }
    }
}