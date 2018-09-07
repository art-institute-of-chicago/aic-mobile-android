package artic.edu.search

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class SearchResultsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            1 -> {
                SearchResultsSuggestedFragment()
            }
            2 -> {
                SearchResultsSuggestedFragment()
            }
            3 -> {
                SearchResultsExhibitionsFragment()
            }
            else -> {
                SearchResultsSuggestedFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 4
    }
}