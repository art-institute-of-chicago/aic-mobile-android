package edu.artic.search

/**
 * General-purpose exception for mis-configured gradle builds.
 *
 * You'll see this most likely if the `build.gradle` file in the `:db`
 * module is not able to assign a well-formed value to
 * [edu.artic.db.BuildConfig.BLOB_URL]. This, in turn, is typically
 * sourced from a file called `local.properties`, `debug.env`, or
 * `release.env`.
 *
 * Check the top-level project README section called `How To Build`
 * for more detailed information.
 */
object SearchUnavailableError : Error(
        "Searching for Artwork, Tours, or Events will not work with this installation." +
                if (BuildConfig.DEBUG) {
                    " Please refer to the 'How To Build' section of the project README for details."
                } else {
                    " Please let us know so we can release an update that fixes the issue."
                }
)
