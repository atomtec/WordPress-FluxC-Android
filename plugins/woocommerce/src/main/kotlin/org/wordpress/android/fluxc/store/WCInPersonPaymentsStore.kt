package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentResponsePayload
import org.wordpress.android.fluxc.model.payments.inperson.WCConnectionTokenResult
import org.wordpress.android.fluxc.model.payments.inperson.WCCreateCustomerByOrderIdResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentChargeApiResult
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson.InPersonPaymentsRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCInPersonPaymentsStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: InPersonPaymentsRestClient
) {
    suspend fun fetchConnectionToken(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel
    ): WooResult<WCConnectionTokenResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchConnectionToken") {
            val response = restClient.fetchConnectionToken(activePlugin, site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    WooResult(WCConnectionTokenResult(response.result.token, response.result.isTestMode))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun capturePayment(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ): WCCapturePaymentResponsePayload {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "capturePayment") {
            restClient.capturePayment(activePlugin, site, paymentId, orderId)
        }
    }

    suspend fun loadAccount(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel
    ): WooResult<WCPaymentAccountResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "loadAccount") {
            restClient.loadAccount(activePlugin, site).asWooResult()
        }
    }

    suspend fun createCustomerByOrderId(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel,
        orderId: Long
    ): WooResult<WCCreateCustomerByOrderIdResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createCustomerByOrderId") {
            restClient.createCustomerByOrderId(activePlugin, site, orderId).asWooResult()
        }
    }

    suspend fun getStoreLocationForSite(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel
    ): WCTerminalStoreLocationResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "getStoreLocationForSite") {
            restClient.getStoreLocationForSite(activePlugin, site)
        }
    }

    suspend fun fetchPaymentCharge(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel,
        chargeId: String
    ): WooPayload<WCPaymentChargeApiResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchPaymentCharge") {
            restClient.fetchPaymentCharge(activePlugin, chargeId, site)
        }
    }

    enum class InPersonPaymentsPluginType {
        WOOCOMMERCE_PAYMENTS,
        STRIPE
    }
}
