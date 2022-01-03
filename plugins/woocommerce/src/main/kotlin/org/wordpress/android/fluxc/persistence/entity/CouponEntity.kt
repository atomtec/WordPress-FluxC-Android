package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.WCMetaData

@Entity
data class CouponEntity(
    @PrimaryKey(autoGenerate = true) val couponLocalId: Long = 0,
    val remoteCouponId: Long,
    val siteRemoteId: Long,
    val code: String,
    val amount: Double,
    val dateCreated: String?,
    val dateCreatedGmt: String?,
    val dateModified: String?,
    val dateModifiedGmt: String?,
    val discountType: CouponType,
    val description: String?,
    val dateExpires: String?,
    val dateExpiresGmt: String?,
    val usageCount: Int,
    val individualUse: Boolean,
    val productIds: List<Long>?,
    val excludedProductIds: List<Long>?,
    val usageLimit: Int,
    val usageLimitPerUser: Int,
    val limitUsageToXItems: Int,
    val freeShipping: Boolean,
    val productCategories: List<Long>?,
    val excludedProductCategories: List<Long>?,
    val excludeSaleItems: Boolean,
    val minimumAmount: Double,
    val maximumAmount: Double,
    val emailRestrictions:  List<String>?,
    val usedBy: List<Long>?,
    val metaData: List<WCMetaData>?
) {
    enum class CouponType {
        Percent,
        FixedCart,
        FixedProduct
    }
}
