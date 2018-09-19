package edu.artic.info

import io.reactivex.Observable

/**
 * @author Sameer Dhakal (Fuzz)
 */

interface MemberDataProvider {
    fun getMemberData(memberID: String, zipCode: String): Observable<SOAPMemberInfoResponse>
}