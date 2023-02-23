package edu.artic.details

import android.os.Bundle
import edu.artic.details.databinding.ActivityDetailsBinding
import edu.artic.events.EventDetailFragment
import edu.artic.tours.TourDetailsFragment
import edu.artic.ui.BaseActivity

class DetailsActivity : BaseActivity<ActivityDetailsBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (navController.currentDestination?.id == navController.graph.startDestinationId) {
            when {
                intent.hasExtra(EventDetailFragment.ARG_EVENT) -> {
                    navController.navigate(R.id.goToEventDetails)
                }
                intent.hasExtra(TourDetailsFragment.ARG_TOUR) -> {
                    navController.navigate(R.id.goToTourDetails)
                }
                else -> {
                    navController.navigate(R.id.goToExhibitionDetails)
                }
            }

        }
    }
}