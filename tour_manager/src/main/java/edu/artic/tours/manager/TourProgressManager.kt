package edu.artic.tours.manager

import android.annotation.SuppressLint
import com.fuzz.rx.Optional
import com.fuzz.rx.filterValue
import edu.artic.db.INTRO_TOUR_STOP_OBJECT_ID
import edu.artic.db.daos.ArticAudioFileDao
import edu.artic.db.models.ArticTour
import edu.artic.db.models.AudioFileModel
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * Responsible for managing the communication between tour carousel and the map.
 * @author Sameer Dhakal (Fuzz)
 */
class TourProgressManager(val audioFileDao: ArticAudioFileDao) {
    /**
     * Selected ArticObject
     */
    val selectedStop: Subject<String> = BehaviorSubject.create()
    /**
     * Save the last selected tour.
     */
    val selectedTour: Subject<Optional<ArticTour>> = BehaviorSubject.createDefault(Optional(null))
    val proposedTour: Subject<Optional<Pair<ArticTour, ArticTour.TourStop>>> = BehaviorSubject.createDefault(Optional(null))
    val leaveTourRequest: Subject<Boolean> = PublishSubject.create()

    /**
     * If [audioFileModel] belongs to currently selected tour i.e. [selectedTour],
     * this method advances [selectedStop] to next tour stop.
     */
    @SuppressLint("CheckResult")
    fun playBackEnded(audioFileModel: AudioFileModel) {
        Observables.combineLatest(selectedTour.filterValue(), selectedStop)
                .take(1)
                .subscribe { (tour, currentStopID) ->

                    /**
                     * Get audio file id
                     */
                    val audioFileID: String? = if (currentStopID == INTRO_TOUR_STOP_OBJECT_ID) {
                        tour.tourAudio
                    } else {
                        tour.tourStops.find { it.objectId == currentStopID }?.audioId
                    }

                    audioFileID?.let { audioID ->
                        audioFileDao
                                .getAudioByIdAsync(audioID)
                                .toObservable()
                                .subscribe { audioFile ->

                                    /**
                                     * Check if currently ended audio playback session belongs to
                                     * current tour stop.
                                     */
                                    val isCurrentStopsAudioTranslation = audioFile
                                            ?.allTranslations()
                                            ?.contains(audioFileModel) == true

                                    /**
                                     * Advance [selectedStop] if current audio translation playback
                                     * completed.
                                     */
                                    if (isCurrentStopsAudioTranslation) {
                                        val indexOfCurrentTourStop = tour.tourStops.indexOfFirst { it.objectId == currentStopID }
                                        val nextStopIndex = Math.min(indexOfCurrentTourStop + 1, tour.tourStops.size)
                                        tour.tourStops[nextStopIndex].objectId?.let {
                                            selectedStop.onNext(it)
                                        }
                                    }
                                }

                    }
                }

    }


}