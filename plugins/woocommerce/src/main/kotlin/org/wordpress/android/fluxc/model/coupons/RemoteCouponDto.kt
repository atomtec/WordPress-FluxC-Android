package org.wordpress.android.fluxc.model.coupons

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.CouponType

data class RemoteCouponDto(
    val id: Long,
    val code: String?,
    val amount: String?,
    @SerializedName("date_created")
    val dateCreated: String?,
    @SerializedName("date_created_gmt")
    val dateCreatedGmt: String?,
    @SerializedName("date_modified")
    val dateModified: String?,
    @SerializedName("date_modified_gmt")
    val dateModifiedGmt: String?,
    @SerializedName("discount_type")
    val discountType: RemoteCouponType,
    val description: String?,
    @SerializedName("date_expires")
    val dateExpires: String?,
    @SerializedName("date_expires_gmt")
    val dateExpiresGmt: String?,
    @SerializedName("usage_count")
    val usageCount: Int,
    @SerializedName("individual_use")
    val individualUse: Boolean,
    @SerializedName("product_ids")
    val productIds:  List<Long>? = null,
    @SerializedName("excluded_product_ids")
    val excludedProductIds:  List<Long>? = null,
    @SerializedName("usage_limit")
    val usageLimit: Int,
    @SerializedName("usage_limit_per_user")
    val usageLimitPerUser: Int,
    @SerializedName("usage_limit_to_x_items")
    val limitUsageToXItems: Int,
    @SerializedName("free_shipping")
    val freeShipping: Boolean,
    @SerializedName("product_categories")
    val productCategories: List<Long>? = null,
    @SerializedName("excluded_product_categories")
    val excludedProductCategories: List<Long>? = null,
    @SerializedName("excluded_sale_items")
    val excludeSaleItems: Boolean,
    @SerializedName("minimum_amount")
    val minimumAmount: String?,
    @SerializedName("maximum_amount")
    val maximumAmount: String?,
    @SerializedName("email_restrictions")
    val emailRestrictions:  List<String>? = null,
    @SerializedName("used_by")
    val usedBy:  List<Long>? = null,
    @SerializedName("meta_data")
    val metaData: List<WCMetaData>? = null
) {
    enum class RemoteCouponType {
        @SerializedName("percent")
        Percent,
        @SerializedName("fixed_cart")
        FixedCart,
        @SerializedName("fixed_product")
        FixedProduct
    }
}