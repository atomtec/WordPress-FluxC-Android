package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.FeeLine
import org.wordpress.android.fluxc.model.order.FeeLineTaxStatus
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderAddress.Billing
import org.wordpress.android.fluxc.model.order.OrderAddress.Shipping
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDtoMapper.toDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

typealias UpdateOrderFlowPredicate = suspend FlowCollector<UpdateOrderResult>.(WCOrderModel, SiteModel) -> Unit

@Singleton
class OrderUpdateStore @Inject internal constructor(
    private val coroutineEngine: CoroutineEngine,
    private val wcOrderRestClient: OrderRestClient,
    private val ordersDao: OrdersDao,
    private val siteSqlUtils: SiteSqlUtils
) {
    suspend fun updateCustomerOrderNote(
        remoteOrderId: RemoteId,
        site: SiteModel,
        newCustomerNote: String
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateCustomerOrderNote") {
            val initialOrder = ordersDao.getOrder(remoteOrderId, site.localId())

            if (initialOrder == null) {
                emitNoEntityFound("Order with id ${remoteOrderId.value} not found")
            } else {
                ordersDao.updateLocalOrder(initialOrder.remoteOrderId, initialOrder.localSiteId) {
                    copy(customerNote = newCustomerNote)
                }
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged()))

                val updateRemoteOrderPayload = wcOrderRestClient.updateCustomerOrderNote(
                        initialOrder,
                        site,
                        newCustomerNote
                )
                val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
                    ordersDao.insertOrUpdateOrder(initialOrder)
                    OnOrderChanged(orderError = updateRemoteOrderPayload.error)
                } else {
                    ordersDao.insertOrUpdateOrder(updateRemoteOrderPayload.order)
                    OnOrderChanged()
                }
                emit(RemoteUpdateResult(remoteUpdateResult))
            }
        }
    }

    suspend fun updateOrderAddress(
        remoteOrderId: RemoteId,
        localSiteId: LocalId,
        newAddress: OrderAddress
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateOrderAddress") {
            takeWhenOrderDataAcquired(remoteOrderId, localSiteId) { initialOrder, site ->
                updateLocalOrderAddress(initialOrder, newAddress)
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged()))

                when (newAddress) {
                    is Billing -> {
                        val payload = wcOrderRestClient.updateBillingAddress(initialOrder, site, newAddress.toDto())
                        emitRemoteUpdateContainingBillingAddress(payload, initialOrder, newAddress)
                    }
                    is Shipping -> {
                        val payload = wcOrderRestClient.updateShippingAddress(initialOrder, site, newAddress.toDto())
                        emitRemoteUpdateResultOrRevertOnError(payload, initialOrder)
                    }
                }
            }
        }
    }

    suspend fun updateBothOrderAddresses(
        remoteOrderId: RemoteId,
        localSiteId: LocalId,
        shippingAddress: Shipping,
        billingAddress: Billing
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateBothOrderAddresses") {
            takeWhenOrderDataAcquired(remoteOrderId, localSiteId) { initialOrder, site ->
                updateBothLocalOrderAddresses(
                        initialOrder,
                        shippingAddress,
                        billingAddress
                ).let { emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged())) }

                wcOrderRestClient.updateBothOrderAddresses(
                        initialOrder,
                        site,
                        shippingAddress.toDto(),
                        billingAddress.toDto()
                ).let { emitRemoteUpdateContainingBillingAddress(it, initialOrder, billingAddress) }
            }
        }
    }

    suspend fun updateSimplePayment(
        site: SiteModel,
        orderId: Long,
        amount: String,
        customerNote: String,
        billingEmail: String,
        isTaxable: Boolean
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateSimplePayment") {
            val initialOrder = ordersDao.getOrder(RemoteId(orderId), site.localId())
            if (initialOrder == null) {
                emitNoEntityFound("Order with id $orderId not found")
            } else {
                // simple payment is assigned a single fee list item upon creation and we must re-use the
                // existing fee id or else a new fee will be added
                val feeId = if (initialOrder.getFeeLineList().isNotEmpty()) {
                    initialOrder.getFeeLineList()[0].id
                } else {
                    null
                }

                ordersDao.updateLocalOrder(initialOrder.remoteOrderId, initialOrder.localSiteId) {
                    copy(
                        customerNote = customerNote,
                        billingEmail = billingEmail,
                        feeLines = OrderRestClient.generateSimplePaymentFeeLineJson(amount, isTaxable, feeId).toString()
                    )
                }
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged()))

                val billing = if (billingEmail.isNotEmpty()) {
                    Billing(

                            email = billingEmail,
                            firstName = "",
                            lastName = "",
                            company = "",
                            address1 = "",
                            address2 = "",
                            city = "",
                            state = "",
                            postcode = "",
                            country = "",
                            phone = ""
                    )
                } else {
                    null
                }

                val updateRequest = UpdateOrderRequest(
                    customerNote = customerNote,
                    billingAddress = billing,
                    feeLines = generateSimplePaymentFeeLineList(amount, isTaxable, feeId)
                )
                val result = updateOrder(site, orderId, updateRequest)
                val remoteUpdateResult = if (result.isError) {
                    ordersDao.insertOrUpdateOrder(initialOrder)
                    OnOrderChanged(orderError = OrderError(message = result.error.message ?: ""))
                } else {
                    OnOrderChanged()
                }
                emit(RemoteUpdateResult(remoteUpdateResult))
            }
        }
    }

    /**
     * Generates the feeLines for a simple payment order containing a single fee line item with
     * the passed information. Pass null for the feeId if this is a new fee line item, otherwise
     * pass the id of an existing fee line item to replace it.
     */
    private fun generateSimplePaymentFeeLineList(
        amount: String,
        isTaxable: Boolean,
        feeId: Long? = null
    ): List<FeeLine> {
        FeeLine().also { feeLine ->
            feeId?.let {
                feeLine.id = it
            }
            feeLine.name = OrderRestClient.SIMPLE_PAYMENT_FEELINE_NAME
            feeLine.total = amount
            feeLine.taxStatus = if (isTaxable) FeeLineTaxStatus.Taxable else FeeLineTaxStatus.None
            return listOf(feeLine)
        }
    }

    suspend fun createOrder(site: SiteModel, createOrderRequest: UpdateOrderRequest): WooResult<WCOrderModel> {
        return coroutineEngine.withDefaultContext(T.API, this, "createOrder") {
            val result = wcOrderRestClient.createOrder(site, createOrderRequest)

            return@withDefaultContext if (result.isError) {
                WooResult(result.error)
            } else {
                val model = result.result!!
                ordersDao.insertOrUpdateOrder(model)
                WooResult(model)
            }
        }
    }

    suspend fun updateOrder(
        site: SiteModel,
        orderId: Long,
        updateRequest: UpdateOrderRequest
    ): WooResult<WCOrderModel> {
        return coroutineEngine.withDefaultContext(T.API, this, "createOrder") {
            val result = wcOrderRestClient.updateOrder(site, orderId, updateRequest)

            return@withDefaultContext if (result.isError) {
                WooResult(result.error)
            } else {
                val model = result.result!!
                ordersDao.insertOrUpdateOrder(model)
                WooResult(model)
            }
        }
    }

    suspend fun deleteOrder(
        site: SiteModel,
        orderId: Long,
        trash: Boolean = true
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(T.API, this, "deleteOrder") {
            val result = wcOrderRestClient.deleteOrder(site, orderId, trash)

            return@withDefaultContext if (result.isError) {
                WooResult(result.error)
            } else {
                ordersDao.deleteOrder(site.localId(), orderId)
                WooResult(Unit)
            }
        }
    }

    private suspend fun FlowCollector<UpdateOrderResult>.takeWhenOrderDataAcquired(
        remoteOrderId: RemoteId,
        localSiteId: LocalId,
        predicate: UpdateOrderFlowPredicate
    ) {
        ordersDao.getOrder(remoteOrderId, localSiteId)?.let { initialOrder ->
            siteSqlUtils.getSiteWithLocalId(initialOrder.localSiteId)
                    ?.let { predicate(initialOrder, it) }
                    ?: emitNoEntityFound("Site with local id ${initialOrder.localSiteId} not found")
        } ?: emitNoEntityFound("Order with id ${remoteOrderId.value} not found")
    }

    private suspend fun updateLocalOrderAddress(
        initialOrder: WCOrderModel,
        newAddress: OrderAddress
    ) = ordersDao.updateLocalOrder(initialOrder.remoteOrderId, initialOrder.localSiteId) {
        when (newAddress) {
            is Billing -> updateLocalBillingAddress(newAddress)
            is Shipping -> updateLocalShippingAddress(newAddress)
        }
    }

    private suspend fun updateBothLocalOrderAddresses(
        initialOrder: WCOrderModel,
        shippingAddress: Shipping,
        billingAddress: Billing
    ) = ordersDao.updateLocalOrder(initialOrder.remoteOrderId, initialOrder.localSiteId) {
        updateLocalShippingAddress(shippingAddress)
        updateLocalBillingAddress(billingAddress)
    }

    private fun WCOrderModel.updateLocalShippingAddress(newAddress: OrderAddress): WCOrderModel {
        return copy(
                shippingFirstName = newAddress.firstName,
                shippingLastName = newAddress.lastName,
                shippingCompany = newAddress.company,
                shippingAddress1 = newAddress.address1,
                shippingAddress2 = newAddress.address2,
                shippingCity = newAddress.city,
                shippingState = newAddress.state,
                shippingPostcode = newAddress.postcode,
                shippingCountry = newAddress.country,
                shippingPhone = newAddress.phone
        )
    }

    private fun WCOrderModel.updateLocalBillingAddress(newAddress: Billing): WCOrderModel {
        return copy(
                billingFirstName = newAddress.firstName,
                billingLastName = newAddress.lastName,
                billingCompany = newAddress.company,
                billingAddress1 = newAddress.address1,
                billingAddress2 = newAddress.address2,
                billingCity = newAddress.city,
                billingState = newAddress.state,
                billingPostcode = newAddress.postcode,
                billingCountry = newAddress.country,
                billingEmail = newAddress.email,
                billingPhone = newAddress.phone
        )
    }

    private suspend fun FlowCollector<UpdateOrderResult>.emitRemoteUpdateContainingBillingAddress(
        updateRemoteOrderPayload: RemoteOrderPayload,
        initialOrder: WCOrderModel,
        billingAddress: Billing
    ) = emitRemoteUpdateResultOrRevertOnError(
            updateRemoteOrderPayload,
            initialOrder,
            mapError = { originalOrderError: OrderError? ->
                /**
                 * It's *likely* as INVALID_PARAM can be caused by probably other cases too and
                 * empty billing address email in future releases of WooCommerce will be not relevant.
                 */
                val isLikelyEmptyBillingEmailError =
                        updateRemoteOrderPayload.error.type == OrderErrorType.INVALID_PARAM &&
                                billingAddress.email.isBlank()

                if (isLikelyEmptyBillingEmailError) {
                    OrderError(
                            type = OrderErrorType.EMPTY_BILLING_EMAIL,
                            message = "Can't set empty billing email address on WooCommerce <= 5.8.1"
                    )
                } else {
                    originalOrderError
                }
            }
    )

    private suspend fun FlowCollector<UpdateOrderResult>.emitRemoteUpdateResultOrRevertOnError(
        updateRemoteOrderPayload: RemoteOrderPayload,
        initialOrder: WCOrderModel,
        mapError: (OrderError?) -> OrderError? = {
            updateRemoteOrderPayload.error
        }
    ) {
        val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
            ordersDao.insertOrUpdateOrder(initialOrder)
            OnOrderChanged(orderError = mapError(updateRemoteOrderPayload.error))
        } else {
            ordersDao.insertOrUpdateOrder(updateRemoteOrderPayload.order)
            OnOrderChanged()
        }

        emit(RemoteUpdateResult(remoteUpdateResult))
    }

    private suspend fun FlowCollector<UpdateOrderResult>.emitNoEntityFound(message: String) {
        emit(UpdateOrderResult.OptimisticUpdateResult(
                OnOrderChanged(orderError = OrderError(message = message))
        ))
    }
}
