package org.wordpress.android.fluxc.store.stats.time

import android.arch.lifecycle.LiveData
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.map
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class VisitsAndViewsStore
@Inject constructor(
    private val restClient: VisitAndViewsRestClient,
    private val sqlUtils: TimeStatsSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchVisits(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode.Top,
        date: Date,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        val payload = restClient.fetchVisits(site, date, granularity, limitMode.limit, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, granularity, date)
                val overviewResponse = timeStatsMapper.map(payload.response, limitMode)
                if (overviewResponse.period.isBlank() || overviewResponse.dates.isEmpty())
                    OnStatsFetched(StatsError(INVALID_RESPONSE, "Overview: Required data 'period' or 'dates' missing"))
                else
                    OnStatsFetched(overviewResponse)
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getVisits(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode,
        date: Date
    ): VisitsAndViewsModel? {
        return sqlUtils.selectVisitsAndViews(site, granularity, date)?.let { timeStatsMapper.map(it, limitMode) }
    }

    fun liveVisits(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode,
        date: Date
    ): LiveData<VisitsAndViewsModel> {
        return sqlUtils.liveSelectVisitsAndViews(site, granularity, date).map { timeStatsMapper.map(it, limitMode) }
    }
}
