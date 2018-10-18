package edu.artic.search

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class SearchResultsPagerAdapter(fm: FragmentManager, var context: Context) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
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
        return when(position) {
            1 -> {
                context.resources.getString(R.string.artworks)
            }
            2 -> {
                context.resources.getString(R.string.tours)
            }
            3 -> {
                context.resources.getString(R.string.exhibitions)
            }
            else -> {
                context.resources.getString(R.string.suggested)
            }
        }
    }
}