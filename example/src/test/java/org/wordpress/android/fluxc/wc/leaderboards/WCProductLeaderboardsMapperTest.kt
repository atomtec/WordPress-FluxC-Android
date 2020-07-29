package org.wordpress.android.fluxc.wc.leaderboards

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleProductList
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleShippingLabelApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductLeaderboardsMapperTest {
    private lateinit var mapperUnderTest: WCProductLeaderboardsMapper
    private lateinit var productStore: WCProductStore

    private val expectedProducts = generateSampleProductList()
    private val productApiResponse = generateSampleShippingLabelApiResponse()
            ?.firstOrNull() { it.type == PRODUCTS }

    @Before
    fun setUp() {
        mapperUnderTest = WCProductLeaderboardsMapper()
        productStore = mock()
    }

    @Test
    fun `map should request and parse all products from id`() = test {
        configureProductStoreMock()
        val result = mapperUnderTest.map(
                productApiResponse!!,
                stubSite,
                productStore
        )
        assertThat(result).isNotNull
        assertThat(result!!.size).isEqualTo(5)
    }

    @Test
    fun `map should request and parse all products from id removing failing ones`() = test {
        configureProductStoreMock()
        configureExactFailingProductStoreMock(15)
        val result = mapperUnderTest.map(
                productApiResponse!!,
                stubSite,
                productStore
        )
        assertThat(result).isNotNull
        assertThat(result!!.size).isEqualTo(4)
    }

    @Test
    fun `map should remove any failing product response from the result`() = test {
        configureFailingProductStoreMock()
        val result = mapperUnderTest.map(
                productApiResponse!!,
                stubSite,
                productStore
        )
        assertThat(result).isNotNull
        assertThat(result).isEmpty()
    }

    private suspend fun configureProductStoreMock() {
        listOf(14, 22, 15, 20, 13)
                .forEachIndexed { index, it ->
                    whenever(productStore.fetchSingleProductSynced(stubSite, it.toLong()))
                            .thenReturn(expectedProducts[index])
                }
    }

    private suspend fun configureFailingProductStoreMock() {
        listOf(14, 22, 15, 20, 13)
                .forEachIndexed { index, it ->
                    whenever(productStore.fetchSingleProductSynced(stubSite, it.toLong()))
                            .thenReturn(null)
                }
    }

    private suspend fun configureExactFailingProductStoreMock(productId: Long) {
        whenever(productStore.fetchSingleProductSynced(stubSite, productId))
                .thenReturn(null)
    }
}
