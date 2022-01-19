package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase

@RunWith(RobolectricTestRunner::class)
internal class CouponDaoTest {
    private lateinit var database: WCAndroidDatabase
    private lateinit var sut: CouponDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sut = database.couponDao()
    }

    @Test
    fun `save and retrieve coupons from a store`(): Unit = runBlocking {

    }

    @Test
    fun `delete coupons for a store`(): Unit = runBlocking {

    }

    @Test
    fun `update coupons for multiple stores`(): Unit = runBlocking {

    }

    @After
    fun tearDown() {
        database.close()
    }
}