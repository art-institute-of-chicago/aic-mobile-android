package edu.artic.splash

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import edu.artic.base.NetworkException
import edu.artic.db.AppDataPreferencesManager
import org.junit.Assert
import org.junit.Test

/**
 * @author Sameer Dhakal (Fuzz)
 */
class ErrorMessagePresenterTest {

    lateinit var appDataPrefManager: AppDataPreferencesManager

    @Test
    fun `app should display network error message for all builds`() {
        appDataPrefManager = mock()

        val errorMessage = "no internet connection"
        val throwable = NetworkException(errorMessage,
                IllegalStateException("blah"))

        val errorMessagePresenter = spy(ErrorMessagePresenter(throwable,
                "loading error",
                appDataPrefManager))

        /**
         * First check for debug build.
         */
        doReturn(true).whenever(errorMessagePresenter).isDebugBuild()
        Assert.assertEquals(errorMessagePresenter.shouldDisplayErrorDialog(), true)
        Assert.assertEquals(errorMessagePresenter.getErrorMessage(), errorMessage)

        /**
         * Testing release build.
         */
        doReturn(false).whenever(errorMessagePresenter).isDebugBuild()
        Assert.assertEquals(errorMessagePresenter.shouldDisplayErrorDialog(), true)
        Assert.assertEquals(errorMessagePresenter.getErrorMessage(), errorMessage)
    }

    @Test
    fun `app should display generic error message for release build when no cache available`() {
        appDataPrefManager = mock()
        val throwable = IllegalStateException("blah")
        val genericErrorMessage = "loading error"
        val errorMessagePresenter = spy(ErrorMessagePresenter(throwable,
                genericErrorMessage,
                appDataPrefManager))

        doReturn(false).`when`(appDataPrefManager).downloadedNecessaryData
        doReturn(false).whenever(errorMessagePresenter).isDebugBuild()

        Assert.assertEquals(errorMessagePresenter.shouldDisplayErrorDialog(), true)
        Assert.assertEquals(errorMessagePresenter.getErrorMessage(), genericErrorMessage)

    }

    @Test
    fun `app should not display error if cache is available for release build`() {
        appDataPrefManager = mock()

        val throwable = IllegalStateException("blah")
        val genericErrorMessage = "loading error"
        val errorMessagePresenter = spy(ErrorMessagePresenter(throwable,
                genericErrorMessage,
                appDataPrefManager))

        doReturn(true).`when`(appDataPrefManager).downloadedNecessaryData
        doReturn(false).whenever(errorMessagePresenter).isDebugBuild()

        Assert.assertEquals(errorMessagePresenter.shouldDisplayErrorDialog(), false)
    }
}
