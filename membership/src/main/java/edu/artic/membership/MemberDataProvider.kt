package edu.artic.membership

import io.reactivex.Observable

/**
 * @author Sameer Dhakal (Fuzz)
 */

/**
 * Contract between
 * * a background service that calls an API and
 * * the ViewModel layer ([InformationViewModel], perhaps)
 *
 * Implementors are encouraged to delegate their work to an instance
 * of [MemberInfoApi].
 */
interface MemberDataProvider {
    /**
     * Retrieve information about the given member's membership. Both parameters
     * should be non-empty.
     *
     * Note that this assumes that the [API][MemberInfoApi] communicates solely
     * via `XML`; more formally, that it follows well-defined `SOAP` (Simple
     * Object Access Protocol).
     */
    fun getMemberData(memberID: String, zipCode: String): Observable<SOAPMemberInfoResponse>
}

/**
 * One of the two bundled implementations of [MemberDataProvider]. The other is
 * [RetrofitMemberDataProvider].
 *
 * Returns [MemberDataForbiddenException] to everything that tries to subscribe
 * to [getMemberData].
 */
object NoContentMemberDataProvider: MemberDataProvider {
    override fun getMemberData(memberID: String, zipCode: String): Observable<SOAPMemberInfoResponse> {
        return Observable.error {
            MemberDataForbiddenException
        }
    }
}