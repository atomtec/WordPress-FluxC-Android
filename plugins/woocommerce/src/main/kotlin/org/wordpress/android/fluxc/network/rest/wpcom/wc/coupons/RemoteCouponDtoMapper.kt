package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import org.wordpress.android.fluxc.model.coupons.RemoteCouponDto
import org.wordpress.android.fluxc.model.coupons.RemoteCouponDto.RemoteCouponType
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.CouponType
import java.math.BigDecimal

object RemoteCouponDtoMapper {
    fun toEntityModel(dto: RemoteCouponDto, remoteSiteId: Long): CouponEntity {
        return CouponEntity(
                remoteCouponId = dto.id,
                siteRemoteId = remoteSiteId,
                code = dto.code ?: "",
                amount = dto.amount?.toDouble() ?: BigDecimal.ZERO.toDouble(),
                dateCreated = dto.dateCreated,
                dateCreatedGmt = dto.dateCreatedGmt,
                dateModified = dto.dateModified,
                dateModifiedGmt = dto.dateModifiedGmt,
                discountType = mapToLocalCouponType(dto.discountType),
                description = dto.description,
                dateExpires = dto.dateExpires,
                dateExpiresGmt = dto.dateExpiresGmt,
                usageCount = dto.usageCount,
                individualUse = dto.individualUse,
                productIds = dto.productIds,
                excludedProductIds = dto.excludedProductIds,
                usageLimit = dto.usageLimit,
                usageLimitPerUser = dto.usageLimitPerUser,
                limitUsageToXItems = dto.limitUsageToXItems,
                freeShipping = dto.freeShipping,
                productCategories = dto.productCategories,
                excludedProductCategories = dto.excludedProductCategories,
                excludeSaleItems = dto.excludeSaleItems,
                minimumAmount = dto.minimumAmount?.toDouble() ?: 0.00,
                maximumAmount = dto.maximumAmount?.toDouble() ?: 0.00,
                emailRestrictions = dto.emailRestrictions,
                usedBy = dto.usedBy,
                metaData = dto.metaData
        )
    }

    fun mapToLocalCouponType(remoteCouponType: RemoteCouponType): CouponType {
        return when(remoteCouponType) {
            RemoteCouponType.Percent -> CouponType.Percent
            RemoteCouponType.FixedCart -> CouponType.FixedCart
            RemoteCouponType.FixedProduct -> CouponType.FixedProduct
        }
    }
}