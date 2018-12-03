package edu.artic.membership

import io.reactivex.Observable
import retrofit2.http.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
// TODO: Briefly describe API capabilities, explain why we're using `xmethods-delayed-quotes`
interface MemberInfoApi {

    @Headers("Content-Type: text/xml",
            "Accept: text/xml",
            "Accept-Charset: utf-8",
            "cache-control: no-cache",
            "SOAPAction: urn:xmethods-delayed-quotes#member_soap_retrieve"
    )

    @POST()
    fun getMemberInfo(@Url url: String = "/api/1",
                      @Body data: String,
                      @Query("token") token: String = BuildConfig.MEMBER_INFO_API_TOKEN
    ): Observable<SOAPMemberInfoResponse>

}
