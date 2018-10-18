package edu.artic.membership

/**
 * Specialized [RuntimeException] for when [BuildConfig.MEMBER_INFO_API] does not conform
 * to the `https://` uri scheme.
 *
 * Note that [BuildConfig.MEMBER_INFO_API] defaults to `"null"` (that is, a String with
 * those four characters in that order).
 *
 * If this is thrown, in all likelihood the issue is a simple omission: the command-line
 * used to build the `:info` module did not include the necessary properties. Please
 * refer to the README for more details.
 */
object MemberDataForbiddenException: RuntimeException(
        "Unfortunately, this build of the app wasn't really set up to work with membership info. Maybe in a future version?"
)
