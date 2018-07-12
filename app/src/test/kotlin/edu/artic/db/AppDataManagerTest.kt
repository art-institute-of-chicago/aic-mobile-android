package edu.artic.db

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.internal.verification.Times

class AppDataManagerTest {

    lateinit var appDataProvider: AppDataServiceProvider
    lateinit var appDataManager: AppDataManager


    @Before
    fun setup() {
        appDataProvider = mock()
        appDataManager = AppDataManager(appDataProvider)
    }

    @After
    fun teardown() {
    }

    @Test
    fun testGetBlobMissingLastModifiedRequestsBlob() {

        val mockedAppData: AppDataState = mock()

        doReturn(Observable.just(HashMap<String, List<String>>())).`when`(appDataProvider).getBlobHeaders()
        doReturn(Observable.just(mockedAppData)).`when`(appDataProvider).getBlob()

        val testObserver = TestObserver<AppDataState>()

        appDataManager.getBlob().subscribe(testObserver)

        verify(appDataProvider, Times(1)).getBlobHeaders()
        verify(appDataProvider, Times(1)).getBlob()
        testObserver.assertValue(mockedAppData)


    }
}