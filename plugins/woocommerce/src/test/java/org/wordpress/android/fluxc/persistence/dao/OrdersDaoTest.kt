package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase

@RunWith(RobolectricTestRunner::class)
class OrdersDaoTest {
    private lateinit var sut: OrdersDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sut = database.ordersDao()
    }

    @Test
    fun testInsertAndUpdateOrder() {
        runBlocking {
            // when
            val initialOrder = generateSampleOrder(42).let {
                val generatedId = sut.insertOrUpdateOrder(it).toInt()
                it.copy(id = generatedId)
            }
            val site = SiteModel().apply { id = initialOrder.localSiteId.value }

            // then
            assertThat(sut.getOrdersForSite(site.localId())).containsExactly(initialOrder)

            // when
            val updatedOrder = initialOrder
                .copy(status = "processing", customerNote = "please gift wrap")
                .also { sut.insertOrUpdateOrder(it) }

            // then
            assertThat(sut.getOrdersForSite(site.localId())).containsExactly(updatedOrder)
        }
    }

    @Test
    fun testGetOrdersForSite() {
        runBlocking {
            generateSampleOrder(3, CoreOrderStatus.PROCESSING.value).let { sut.insertOrUpdateOrder(it) }
            generateSampleOrder(4, CoreOrderStatus.ON_HOLD.value).let { sut.insertOrUpdateOrder(it) }
            generateSampleOrder(5, CoreOrderStatus.CANCELLED.value).let { sut.insertOrUpdateOrder(it) }
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID }

            // Test getting orders without specifying a status
            val storedOrders = sut.getOrdersForSite(site.localId())
            assertThat(storedOrders).hasSize(3)

            // Test pulling orders with a single status specified
            val processingOrders = sut.getOrdersForSite(site.localId(), listOf(CoreOrderStatus.PROCESSING.value))
            assertThat(processingOrders).hasSize(1)

            // Test pulling orders with multiple statuses specified
            val mixStatusOrders = sut.getOrdersForSite(
                site.localId(),
                listOf(CoreOrderStatus.ON_HOLD.value, CoreOrderStatus.CANCELLED.value)
            )
            assertThat(mixStatusOrders).hasSize(2)
        }
    }

    @Test
    fun testDeleteOrdersForSite() {
        runBlocking {
            generateSampleOrder(1).let { sut.insertOrUpdateOrder(it) }
            generateSampleOrder(2).let { sut.insertOrUpdateOrder(it) }
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID }

            val storedOrders = sut.getOrdersForSite(site.localId())
            assertThat(storedOrders).hasSize(2)

            sut.deleteOrdersForSite(site.localId())

            val deletedOrders = sut.getOrdersForSite(site.localId())
            assertThat(deletedOrders).isEmpty()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private companion object {
        const val TEST_LOCAL_SITE_ID = 6

        fun generateSampleOrder(
            remoteId: Long,
            orderStatus: String = CoreOrderStatus.PROCESSING.value
        ) = WCOrderModel(
                remoteOrderId = RemoteId(remoteId),
                localSiteId = LocalId(TEST_LOCAL_SITE_ID),
                status = orderStatus
        )
    }
}
