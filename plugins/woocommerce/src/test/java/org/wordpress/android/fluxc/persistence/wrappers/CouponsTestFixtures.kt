package org.wordpress.android.fluxc.persistence.wrappers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupons.RemoteCouponDto
import org.wordpress.android.fluxc.model.coupons.RemoteCouponDto.RemoteCouponType
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.CouponType.FixedCart

object CouponsTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

    val sampleEntity = CouponEntity(
            siteRemoteId = stubSite.siteId,
            remoteCouponId = 1L,
            code = "test",
            amount = 0.00,
            dateCreated = "",
            dateCreatedGmt = "",
            dateModified = "",
            dateExpiresGmt = "",
            discountType = FixedCart,
            description = "",
            dateExpires = null,
            dateModifiedGmt = null,
            usageCount = 2,
            individualUse = false,
            productIds = arrayListOf(17),
            excludedProductIds = emptyList(),
            usageLimit = 3,
            usageLimitPerUser = 2,
            limitUsageToXItems = 0,
            freeShipping = false,
            productCategories = emptyList(),
            excludedProductCategories = emptyList(),
            excludeSaleItems = false,
            minimumAmount = 0.00,
            maximumAmount = 0.00,
            usedBy = arrayListOf(1, 1),
            metaData = null,
            emailRestrictions = emptyList()
    )
    val sampleModel = RemoteCouponDto(
            id = 1L,
            code = "test",
            amount = "0.00",
            dateCreated = "",
            dateCreatedGmt = "",
            dateModified = "",
            dateModifiedGmt = "",
            discountType = RemoteCouponType.FixedCart,
            description = "",
            dateExpires = null,
            dateExpiresGmt = null,
            usageCount = 2,
            individualUse = false,
            productIds = arrayListOf(17),
            excludedProductIds = emptyList(),
            usageLimit = 3,
            usageLimitPerUser = 2,
            limitUsageToXItems = 0,
            freeShipping = false,
            productCategories = emptyList(),
            excludedProductCategories = emptyList(),
            excludeSaleItems = false,
            minimumAmount = "0.00",
            maximumAmount = "0.00",
            usedBy = arrayListOf(1, 1),
            metaData = null,
            emailRestrictions = emptyList()
    )
}