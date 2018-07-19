package edu.artic.db

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import edu.artic.db.daos.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.internal.verification.Times

class AppDataManagerTest {

    lateinit var appDataProvider: AppDataServiceProvider
    lateinit var appDataManager: AppDataManager
    lateinit var database: AppDatabase
    lateinit var dashboardDao: DashboardDao
    lateinit var generalInfoDao: GeneralInfoDao
    lateinit var galleryDao: ArticGalleryDao
    lateinit var objectDao: ArticObjectDao
    lateinit var audioFileDao: ArticAudioFileDao
    lateinit var appDataPrefManager: AppDataPreferencesManager

    @Before
    fun setup() {

        dashboardDao = mock()
        generalInfoDao = mock()
        galleryDao = mock()
        objectDao = mock()
        audioFileDao = mock()
        appDataPrefManager = mock()

        database = mock()
        doReturn(dashboardDao).`when`(database).dashboardDao
        doReturn(generalInfoDao).`when`(database).generalInfoDao
        doReturn(galleryDao).`when`(database).galleryDao
        doReturn(objectDao).`when`(database).objectDao
        doReturn(audioFileDao).`when`(database).audioFileDao

        appDataProvider = mock()
        appDataManager = AppDataManager(appDataProvider, appDataPrefManager, database)
    }

    @After
    fun teardown() {
    }

    @Test
    fun testGetBlobMissingLastModifiedRequestsBlob() {

        val mockedAppData: ProgressDataState = mock()

        doReturn(Observable.just(HashMap<String, List<String>>())).`when`(appDataProvider).getBlobHeaders()
        doReturn(Observable.just(mockedAppData)).`when`(appDataProvider).getBlob()

        val testObserver = TestObserver<ProgressDataState>()

        appDataManager.getBlob().subscribe(testObserver)

        verify(appDataProvider, Times(1)).getBlobHeaders()
        verify(appDataProvider, Times(1)).getBlob()
        testObserver.assertValue(mockedAppData)


    }
}