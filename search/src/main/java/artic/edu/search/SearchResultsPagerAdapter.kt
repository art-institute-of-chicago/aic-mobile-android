package artic.edu.search

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class SearchResultsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            1 -> {
                SearchResultsArtworkFragment()
            }
            2 -> {
                SearchResultsToursFragment()
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

    ;
    override fun getPageTitle(position: Int): CharSequence? {
        return when(position) {
            1 -> {
                "Artworks"
            }
            2 -> {
                "Tours"
            }
            3 -> {
                "Exhibitions"
            }
            else -> {
                "Suggested"
            }
        }
    }
}