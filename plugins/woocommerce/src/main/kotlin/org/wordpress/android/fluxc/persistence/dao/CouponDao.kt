package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.CouponEntity

@Dao
abstract class CouponDao {
    @Insert
    abstract suspend fun insertCoupon(couponEntity: CouponEntity): Long

    @Transaction
    @Query("SELECT * FROM CouponEntity WHERE couponLocalId = :localId")
    abstract fun observeCouponsByLocalId(localId: Long): Flow<List<CouponEntity>>

    @Transaction
    @Query("SELECT * FROM CouponEntity WHERE siteRemoteId = :siteRemoteId")
    abstract fun observeCouponsForSite(siteRemoteId: Long): Flow<List<CouponEntity>>

    @Transaction
    @Query("SELECT * FROM CouponEntity WHERE siteRemoteId = :siteRemoteId AND remoteCouponId = :remoteCouponId")
    abstract fun observeCouponByRemoteCouponIdForSite(siteRemoteId: Long, remoteCouponId: Long): Flow<List<CouponEntity>>

    @Query("DELETE FROM CouponEntity WHERE couponLocalId = :localId")
    abstract suspend fun deleteCouponByLocalId(localId: Long)

    @Query("DELETE FROM CouponEntity WHERE siteRemoteId = :siteRemoteId")
    abstract suspend fun deleteCouponsForSite(siteRemoteId: Long)

    @Query("DELETE FROM CouponEntity WHERE remoteCouponId = :remoteCouponId AND siteRemoteId = :siteRemoteId")
    abstract suspend fun deleteCouponByRemoteCouponIdForSite(remoteCouponId: Long, siteRemoteId: Long)

    @Transaction
    open suspend fun cacheCoupons(
        remoteCouponId: Long,
        siteRemoteId: Long,
        couponEntities: List<CouponEntity>
    ) {
        deleteCouponByRemoteCouponIdForSite(remoteCouponId = remoteCouponId, siteRemoteId = siteRemoteId)

        couponEntities.forEach { couponEntity ->
            insertCoupon(couponEntity)
        }
    }
}