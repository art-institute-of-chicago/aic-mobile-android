package edu.artic.db

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import edu.artic.db.daos.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.internal.verification.Times
import java.util.concurrent.LinkedBlockingQueue

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
        appDataProvider = mock()
        appDataManager = AppDataManager(serviceProvider = appDataProvider,
                appDataPreferencesManager = appDataPrefManager,
                dashboardDao = dashboardDao,
                generalInfoDao = generalInfoDao,
                galleryDao = galleryDao,
                objectDao = objectDao,
                audioFileDao = audioFileDao,
                appDatabase = database,
                dataObjectDao = mock(),
                eventDao = mock(),
                exhibitionCMSDao = mock(),
                exhibitionDao = mock(),
                mapAnnotationDao = mock(),
                tourDao = mock(),
                searchSuggestionDao = mock(),
                articMapFloorDao = mock()
        )
    }

    @After
    fun teardown() {
    }

    @Test
    fun testGetBlobMissingLastModifiedRequestsBlob() {

        val mockedAppData: ProgressDataState = mock()

        doReturn(Observable.just(HashMap<String, List<String>>())).`when`(appDataProvider).getBlobHeaders()
        doReturn(Observable.just(mockedAppData)).`when`(appDataProvider).getBlob()

        val subscriptionInterceptor = LinkedBlockingQueue<Runnable>()
        val testObserver = TestObserver<ProgressDataState>()

        appDataManager.getBlob()
                .observeOn(Schedulers.from { r -> subscriptionInterceptor.add(r) })
                .subscribe(testObserver)

        subscriptionInterceptor
                // This blocks until we observe the first emission from the ::getBlob call...
                .take()
                // ..at which point we can synchronously execute ::subscribeActual
                .run()

        verify(appDataProvider, Times(1)).getBlobHeaders()
        verify(appDataProvider, Times(1)).getBlob()
        testObserver.assertValue(mockedAppData)


    }
}