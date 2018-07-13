package edu.artic.db

import android.arch.persistence.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import edu.artic.db.models.*
import java.util.Collections.emptyList
import javax.inject.Inject

class AppConverters {

    private val moshi = getMoshi()

    private var stringListAdapter: JsonAdapter<List<String>>? = null
    private var audioTranslationListAdapter : JsonAdapter<List<ArticAudioFile.Translation>>? = null
    private var generalInfoTranslationListAdapter : JsonAdapter<List<ArticGeneralInfo.Translation>>? = null
    private var tourTranslationListAdapter : JsonAdapter<List<ArticTour.Translation>>? = null
    private var audioCommentaryObjectListAdapter : JsonAdapter<List<AudioCommentaryObject>>? = null
    private var tourStopListAdapter : JsonAdapter<List<ArticTour.TourStop>>? = null
    private var tourCategoryListAdapter : JsonAdapter<List<ArticTourCategory>>? = null


    private fun getStringListAdapter() : JsonAdapter<List<String>> {
        return stringListAdapter.let {
            if(it == null) {
                stringListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                String::class.java
                        )
                )
            }
            return@let stringListAdapter!!
        }

    }

    private fun getAudioTranslationListAdapter() : JsonAdapter<List<ArticAudioFile.Translation>>{
        return audioTranslationListAdapter.let {
            if(it == null) {
                audioTranslationListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                ArticAudioFile.Translation::class.java
                        )
                )
            }
            return@let audioTranslationListAdapter!!
        }
    }

    private fun getGeneralInfoTranslationListAdapter() : JsonAdapter<List<ArticGeneralInfo.Translation>>{
        return generalInfoTranslationListAdapter.let {
            if(it == null) {
                generalInfoTranslationListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                ArticGeneralInfo.Translation::class.java
                        )
                )
            }
            return@let generalInfoTranslationListAdapter!!
        }
    }

    private fun getAudioComentaryListAdapter() : JsonAdapter<List<AudioCommentaryObject>>{
        return audioCommentaryObjectListAdapter.let {
            if(it == null) {
                audioCommentaryObjectListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                AudioCommentaryObject::class.java
                        )
                )
            }
            return@let audioCommentaryObjectListAdapter!!
        }
    }

    private fun getTourTranslationListAdapter() : JsonAdapter<List<ArticTour.Translation>>{
        return tourTranslationListAdapter.let {
            if(it == null) {
                tourTranslationListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                ArticTour.Translation::class.java
                        )
                )
            }
            return@let tourTranslationListAdapter!!
        }
    }

    private fun getTourStopListAdapter() : JsonAdapter<List<ArticTour.TourStop>>{
        return tourStopListAdapter.let {
            if(it == null) {
                tourStopListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                ArticTour.TourStop::class.java
                        )
                )
            }
            return@let tourStopListAdapter!!
        }
    }

    private fun getTourCategoryListAdapter() : JsonAdapter<List<ArticTourCategory>>{
        return tourCategoryListAdapter.let {
            if(it == null) {
                tourCategoryListAdapter = moshi.adapter(
                        Types.newParameterizedType(
                                List::class.java,
                                ArticTourCategory::class.java
                        )
                )
            }
            return@let tourCategoryListAdapter!!
        }
    }

    @TypeConverter
    fun stringToList(data: String?): List<String> {
        return data.let {
            if (it == null) {
                emptyList()
            } else {
                getStringListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }

    @TypeConverter
    fun listToString(someObjects: List<String>): String {
        return getStringListAdapter().toJson(someObjects)
    }

    @TypeConverter
    fun audioTranslationListToString(objects : List<ArticAudioFile.Translation>) : String {
        return getAudioTranslationListAdapter().toJson(objects)
    }

    @TypeConverter
    fun stringToAudioTranslationList(json : String?) : List<ArticAudioFile.Translation> {
        return json.let {
            if (it == null) {
                emptyList()
            } else {
                getAudioTranslationListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }

    @TypeConverter
    fun generalInfoTranslationListToString(objects : List<ArticGeneralInfo.Translation>) : String {
        return getGeneralInfoTranslationListAdapter().toJson(objects)
    }

    @TypeConverter
    fun stringToGeneralInfoTranslationList(json : String?) : List<ArticGeneralInfo.Translation> {
        return json.let {
            if (it == null) {
                emptyList()
            } else {
                getGeneralInfoTranslationListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }

    @TypeConverter
    fun audioCommentaryObjectListToString(objects : List<AudioCommentaryObject>) : String {
        return getAudioComentaryListAdapter().toJson(objects)
    }

    @TypeConverter
    fun stringToAudioCommentaryObjectList(json: String?) : List<AudioCommentaryObject> {
        return json.let {
            if (it == null) {
                emptyList()
            } else {
                getAudioComentaryListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }

    @TypeConverter
    fun tourTranslationListToString(objects : List<ArticTour.Translation>) : String {
        return getTourTranslationListAdapter().toJson(objects)
    }

    @TypeConverter
    fun stringToTourTranslationList(json: String?) : List<ArticTour.Translation> {
        return json.let {
            if (it == null) {
                emptyList()
            } else {
                getTourTranslationListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }

    @TypeConverter
    fun tourStopListToString(objects : List<ArticTour.TourStop>) : String {
        return getTourStopListAdapter().toJson(objects)
    }

    @TypeConverter
    fun stringToTourStopList(json: String?) : List<ArticTour.TourStop> {
        return json.let {
            if (it == null) {
                emptyList()
            } else {
                getTourStopListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }


    @TypeConverter
    fun tourCategoryListToString(objects : List<ArticTourCategory>) : String {
        return getTourCategoryListAdapter().toJson(objects)
    }

    @TypeConverter
    fun stringToTourCategoryList(json: String?) : List<ArticTourCategory> {
        return json.let {
            if (it == null) {
                emptyList()
            } else {
                getTourCategoryListAdapter().fromJson(it) ?: emptyList()
            }
        }
    }

}