package edu.artic.search

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class SearchResultsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

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