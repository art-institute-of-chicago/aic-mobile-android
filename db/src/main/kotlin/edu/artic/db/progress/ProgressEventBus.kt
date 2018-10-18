package edu.artic.db.progress
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * @author Jobin Lawrance
 * @version 1.0.0
 */
class ProgressEventBus {

    private val busSubject: PublishSubject<ProgressEvent> = PublishSubject.create()

    fun post(progressEvent: ProgressEvent) {
        busSubject.onNext(progressEvent)
    }

    fun observable(): Observable<ProgressEvent> = busSubject
}