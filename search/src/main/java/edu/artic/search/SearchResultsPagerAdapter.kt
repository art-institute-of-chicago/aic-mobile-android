package edu.artic.search

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class SearchResultsPagerAdapter(fm: androidx.fragment.app.FragmentManager, var context: Context) : androidx.fragment.app.FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        return when (position) {
            1 -> {
                SearchArtworkFragment()
            }
            2 -> {
                SearchToursFragment()
            }
            3 -> {
                SearchExhibitionsFragment()
            }
            else -> {
                SearchSuggestedFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 4
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            1 -> {
                context.resources.getString(R.string.search_artworks_header)
            }
            2 -> {
                context.resources.getString(R.string.welcome_tours_header)
            }
            3 -> {
                context.resources.getString(R.string.search_exhibitions_header)
            }
            else -> {
                context.resources.getString(R.string.search_suggested)
            }
        }
    }
}