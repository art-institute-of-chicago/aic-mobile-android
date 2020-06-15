package edu.artic.search

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class SearchResultsPagerAdapter(fm: FragmentManager, var context: Context) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
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