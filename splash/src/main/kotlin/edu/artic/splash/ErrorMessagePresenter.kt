package edu.artic.splash

import edu.artic.base.NetworkException
import edu.artic.db.AppDataPreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */
class ErrorMessagePresenter(val throwable: Throwable,
                            val genericErrorMessage: String,
                            val appDataPreferencesManager: AppDataPreferencesManager) {

    /**
     * If the build is release, mask the real exception message by genericErrorMessage.
     */
    fun getErrorMessage(): String {
        return when (throwable) {
            is NetworkException ->
                throwable.localizedMessage
            else ->
                if (isDebugBuild()) {
                    throwable.localizedMessage
                } else {
                    genericErrorMessage
                }
        }
    }

    /**
     * Display all errors in debug build.
     * We only display [NetworkException] in release build.
     */
    fun shouldDisplayErrorDialog(): Boolean {
        val releaseBuildErrorPolicy = !hasDownloadedBasicData() || throwable is NetworkException
        val debugBuildErrorPolicy = isDebugBuild()

        return releaseBuildErrorPolicy || debugBuildErrorPolicy
    }

    private fun hasDownloadedBasicData(): Boolean {
        return appDataPreferencesManager.downloadedNecessaryData
    }

    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }
}