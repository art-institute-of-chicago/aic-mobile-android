package edu.artic.membership

import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

@Root(strict = false)
class MemberInfo {
    @field:Element(name = "MemberLevel")
    var memberLevel: String? = null

    @field:Element(name = "Expiration")
    var expiration: String? = null

    @field:Element(name = "CardHolder")
    var cardHolder: String? = null

    @field:Element(name = "PrimaryConstituentID")
    var primaryConstituentID: String? = null
}

/**
 * Represents SOAP Envelope.
 */
@Root(name = "Envelope", strict = false)
@Namespace(prefix = "SOAP-ENV")
class SOAPMemberInfoResponse {
    @field:Element(name = "Body")
    var responseBody: SOAPBody? = null
}

/**
 * SOAP response body.
 */
@Root(name = "Body", strict = false)
class SOAPBody {
    @field:Element(name = "member_soap_retrieveResponse", required = false)
    var soapResponse: MemberSoapResponse? = null

    @field:Element(name = "Fault", required = false)
    var fault: Fault? = null
}

/**
 * Represents response_object.
 */
@Root(name = "member_soap_retrieveResponse", strict = false)
@Namespace(prefix = "ns1")
class MemberSoapResponse {
    @field:Element(name = "response_object")
    var memberResponseObject: MemberResponseObject? = null
}

@Root(name = "response_object", strict = false)
class MemberResponseObject {

    @field:Element(name = "ResultCode")
    var resultCode: Int = 0

    @field:Element(name = "Memberships")
    var members: Members? = null

}

/**
 * Single account can have multiple cardHolders.
 * Handles only two members now.(matches iOS implementation)
 */
@Root(name = "Memberships", strict = false)
class Members {
    @field:Element(name = "Member-1", required = false)
    var member1: MemberInfo? = null
    @field:Element(name = "Member-2", required = false)
    var member2: MemberInfo? = null
}

/**
 * Error message model.
 * Server returns Fault object on error.
 */
@Root(name = "Fault", strict = false)
class Fault {

    @field:Element(name = "faultcode", required = false)
    var faultCode: String? = null

    @field:Element(name = "faultstring", required = false)
    var faultString: String? = null
}