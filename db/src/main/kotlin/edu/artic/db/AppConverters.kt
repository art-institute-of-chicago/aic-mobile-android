package edu.artic.db

import android.arch.persistence.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.Collections.emptyList
import javax.inject.Inject

class AppConverters {

    private val moshi = getMoshi()

    private var stringListAdapter: JsonAdapter<List<String>>? = null


    private fun generateAdapter() {
        stringListAdapter.let {
            if(it == null) {
                stringListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                String::class.java
                        )
                )
            }
        }
    }
    @TypeConverter
    fun stringToList(data: String?): List<String> {
        generateAdapter()
        return data.let {
            if (it == null) {
                emptyList()
            } else {
                stringListAdapter?.fromJson(it) ?: emptyList()
            }
        }
//        return emptyList()
    }

    @TypeConverter
    fun listToString(someObjects: List<String>): String {
        generateAdapter()
        return stringListAdapter!!.toJson(someObjects)
    }
}