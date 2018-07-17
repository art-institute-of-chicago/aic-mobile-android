package edu.artic.db

import io.reactivex.Observable

interface AppDataServiceProvider {

    fun getBlobHeaders() : Observable<Map<String, List<String>>>
    fun getBlob(): Observable<ProgressDataState>
    fun getExhibitions() : Observable<ProgressDataState>
}