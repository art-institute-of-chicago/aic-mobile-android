package edu.artic.db

import android.arch.persistence.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.models.*
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Collections.emptyList

class AppConverters {

    private val moshi = getMoshi()

    private val stringListAdapter: JsonAdapter<List<String>> by lazy {
        moshi.adapter<List<String>>(
                Types.newParameterizedType(List::class.java, String::class.java)
        )
    }
    private val audioTranslationListAdapter: JsonAdapter<List<ArticAudioFile.Translation>> by lazy {
        moshi.adapter<List<ArticAudioFile.Translation>>(
                Types.newParameterizedType(
                        List::class.java,
                        ArticAudioFile.Translation::class.java
                )
        )
    }
    private val generalInfoTranslationListAdapter: JsonAdapter<List<ArticGeneralInfo.Translation>> by lazy {
        moshi.adapter<List<ArticGeneralInfo.Translation>>(
                Types.newParameterizedType(
                        List::class.java,
                        ArticGeneralInfo.Translation::class.java
                )
        )
    }
    private val tourTranslationListAdapter: JsonAdapter<List<ArticTour.Translation>> by lazy {
        moshi.adapter<List<ArticTour.Translation>>(
                Types.newParameterizedType(
                        List::class.java,
                        ArticTour.Translation::class.java
                )
        )
    }

    private val audioCommentaryObjectListAdapter: JsonAdapter<List<AudioCommentaryObject>> by lazy {
        moshi.adapter<List<AudioCommentaryObject>>(
                Types.newParameterizedType(
                        List::class.java,
                        AudioCommentaryObject::class.java
                )
        )
    }
    private val tourStopListAdapter: JsonAdapter<List<ArticTour.TourStop>> by lazy {
        moshi.adapter<List<ArticTour.TourStop>>(
                Types.newParameterizedType(
                        List::class.java,
                        ArticTour.TourStop::class.java
                )
        )
    }

    private val tourCategoryListAdapter: JsonAdapter<List<ArticTourCategory>> by lazy {
        moshi.adapter<List<ArticTourCategory>>(
                Types.newParameterizedType(
                        List::class.java,
                        ArticTourCategory::class.java
                )
        )
    }

    private val dateTimeFormatte: DateTimeFormatter by lazy {
        DateTimeHelper.DEFAULT_FORMATTER
    }

    @TypeConverter
    fun stringToList(data: String?): List<String> {
        return safeList(data, stringListAdapter)
    }

    @TypeConverter
    fun listToString(someObjects: List<String>): String {
        return stringListAdapter.toJson(someObjects)
    }

    @TypeConverter
    fun audioTranslationListToString(objects: List<ArticAudioFile.Translation>): String {
        return audioTranslationListAdapter.toJson(objects)
    }

    @TypeConverter
    fun stringToAudioTranslationList(json: String?): List<ArticAudioFile.Translation> {
        return safeList(json, audioTranslationListAdapter)
    }

    @TypeConverter
    fun generalInfoTranslationListToString(objects: List<ArticGeneralInfo.Translation>): String {
        return generalInfoTranslationListAdapter.toJson(objects)
    }

    @TypeConverter
    fun stringToGeneralInfoTranslationList(json: String?): List<ArticGeneralInfo.Translation> {
        return safeList(json, generalInfoTranslationListAdapter)
    }

    @TypeConverter
    fun audioCommentaryObjectListToString(objects: List<AudioCommentaryObject>): String {
        return audioCommentaryObjectListAdapter.toJson(objects)
    }

    @TypeConverter
    fun stringToAudioCommentaryObjectList(json: String?): List<AudioCommentaryObject> {
        return safeList(json, audioCommentaryObjectListAdapter)
    }

    @TypeConverter
    fun tourTranslationListToString(objects: List<ArticTour.Translation>): String {
        return tourTranslationListAdapter.toJson(objects)
    }

    @TypeConverter
    fun stringToTourTranslationList(json: String?): List<ArticTour.Translation> {
        return safeList(json, tourTranslationListAdapter)
    }

    @TypeConverter
    fun tourStopListToString(objects: List<ArticTour.TourStop>): String {
        return tourStopListAdapter.toJson(objects)
    }

    @TypeConverter
    fun stringToTourStopList(json: String?): List<ArticTour.TourStop> {
        return safeList(json, tourStopListAdapter)

    }


    @TypeConverter
    fun tourCategoryListToString(objects: List<ArticTourCategory>): String {
        return tourCategoryListAdapter.toJson(objects)
    }

    @TypeConverter
    fun stringToTourCategoryList(json: String?): List<ArticTourCategory> {
        return safeList(json, tourCategoryListAdapter)
    }

    @TypeConverter
    fun fromDateTime(input: ZonedDateTime?): String? = input?.let {
        it.toString()
    }

    @TypeConverter
    fun toDateTime(time: String?): ZonedDateTime? = time?.let { DateTimeFormatter.ISO_DATE_TIME.parse(it, ZonedDateTime.FROM) }

    private inline fun <T> safeList(data: String?, adapterGetter: JsonAdapter<List<T>>): List<T> {
        return data?.let { adapterGetter.fromJson(data) } ?: emptyList()
    }

}