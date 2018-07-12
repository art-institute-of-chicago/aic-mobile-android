package edu.artic.db.progress
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * @author Jobin Lawrance
 * @version 1.0.0
 */
class ProgressEventBus {

    val mBusSubject: PublishSubject<ProgressEvent> = PublishSubject.create()

    fun post(progressEvent: ProgressEvent) {
        mBusSubject.onNext(progressEvent)
    }

    fun observable(): Observable<ProgressEvent> = mBusSubject
}