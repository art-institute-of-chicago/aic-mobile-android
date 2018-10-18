package edu.artic.membership

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit

/**
@author Sameer Dhakal (Fuzz)
 */
class RetrofitMemberDataProvider(
        retrofit: Retrofit
) : MemberDataProvider {

    private val api: MemberInfoApi = retrofit.create(MemberInfoApi::class.java)

    override fun getMemberData(memberID: String, zipCode: String): Observable<SOAPMemberInfoResponse> {
        val data = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:xmethods-delayed-quotes">
                <soapenv:Header/>
                <soapenv:Body>
                    <urn:member_soap_retrieve>
                        <pcid>[MEMBER_ID]</pcid>
                        <phone></phone>
                        <email></email>
                        <zip>[ZIP_CODE]</zip>
                    </urn:member_soap_retrieve>
                </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()
                .replace("[MEMBER_ID]", memberID)
                .replace("[ZIP_CODE]", zipCode)

        return api.getMemberInfo(data = data)
                .observeOn(Schedulers.io())
    }

}