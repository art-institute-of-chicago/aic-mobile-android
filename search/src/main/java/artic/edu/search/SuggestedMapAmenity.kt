package artic.edu.search

import edu.artic.db.models.ArticMapAmenityType

/**
 * @author Sameer Dhakal (Fuzz)
 */

sealed class SuggestedMapAmenities(val type: String) {
    object Restrooms : SuggestedMapAmenities(ArticMapAmenityType.RESTROOMS)
    object GiftShop : SuggestedMapAmenities(ArticMapAmenityType.GIFT_SHOP)
    object MembersLounge : SuggestedMapAmenities(ArticMapAmenityType.MEMBERS_LOUNGE)
    object Dining : SuggestedMapAmenities(ArticMapAmenityType.DINING)
}
