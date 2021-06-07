package org.wordpress.android.fluxc.store

import android.text.TextUtils
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.WellSql
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_AUTOMATED_TRANSFER_ELIGIBILITY
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_AUTOMATED_TRANSFER_STATUS
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_DOMAIN_AVAILABILITY
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_IS_WPCOM_URL
import org.wordpress.android.fluxc.action.SiteAction.CHECK_AUTOMATED_TRANSFER_ELIGIBILITY
import org.wordpress.android.fluxc.action.SiteAction.CHECK_AUTOMATED_TRANSFER_STATUS
import org.wordpress.android.fluxc.action.SiteAction.CHECK_DOMAIN_AVAILABILITY
import org.wordpress.android.fluxc.action.SiteAction.COMPLETED_QUICK_START
import org.wordpress.android.fluxc.action.SiteAction.COMPLETE_QUICK_START
import org.wordpress.android.fluxc.action.SiteAction.CREATED_NEW_SITE
import org.wordpress.android.fluxc.action.SiteAction.CREATE_NEW_SITE
import org.wordpress.android.fluxc.action.SiteAction.DELETED_SITE
import org.wordpress.android.fluxc.action.SiteAction.DELETE_SITE
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATED_MOBILE_EDITOR_FOR_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATED_PRIMARY_DOMAIN
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_MOBILE_EDITOR
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_MOBILE_EDITOR_FOR_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_PRIMARY_DOMAIN
import org.wordpress.android.fluxc.action.SiteAction.EXPORTED_SITE
import org.wordpress.android.fluxc.action.SiteAction.EXPORT_SITE
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_BLOCK_LAYOUTS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_CONNECT_SITE_INFO
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_DOMAIN_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_DOMAIN_SUPPORTED_STATES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_JETPACK_CAPABILITIES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_PLANS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_POST_FORMATS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_PRIVATE_ATOMIC_COOKIE
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_PROFILE_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_SITES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_SITES_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_SITE_EDITORS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_USER_ROLES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_WPCOM_SITE_BY_URL
import org.wordpress.android.fluxc.action.SiteAction.FETCH_BLOCK_LAYOUTS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_CONNECT_SITE_INFO
import org.wordpress.android.fluxc.action.SiteAction.FETCH_DOMAIN_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_DOMAIN_SUPPORTED_STATES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_JETPACK_CAPABILITIES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_PLANS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_POST_FORMATS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_PRIVATE_ATOMIC_COOKIE
import org.wordpress.android.fluxc.action.SiteAction.FETCH_PROFILE_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITE
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITES_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITE_EDITORS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_USER_ROLES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_WPCOM_SITE_BY_URL
import org.wordpress.android.fluxc.action.SiteAction.HIDE_SITES
import org.wordpress.android.fluxc.action.SiteAction.INITIATED_AUTOMATED_TRANSFER
import org.wordpress.android.fluxc.action.SiteAction.INITIATE_AUTOMATED_TRANSFER
import org.wordpress.android.fluxc.action.SiteAction.IS_WPCOM_URL
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_SITE
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_WPCOM_AND_JETPACK_SITES
import org.wordpress.android.fluxc.action.SiteAction.SHOW_SITES
import org.wordpress.android.fluxc.action.SiteAction.SUGGESTED_DOMAINS
import org.wordpress.android.fluxc.action.SiteAction.SUGGEST_DOMAINS
import org.wordpress.android.fluxc.action.SiteAction.UPDATE_SITE
import org.wordpress.android.fluxc.action.SiteAction.UPDATE_SITES
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.PostFormatModel
import org.wordpress.android.fluxc.model.RoleModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookieResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedCountryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType.NON_PRIVATE_AT_SITE
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType.SITE_MISSING_FROM_STORE
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteErrorType.INVALID_SITE
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityErrorType.INVALID_DOMAIN_NAME
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType.INVALID_COUNTRY_CODE
import org.wordpress.android.fluxc.store.SiteStore.ExportSiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType.NOT_AVAILABLE
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.DUPLICATE_SITE
import org.wordpress.android.fluxc.utils.SiteErrorUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 */
@Singleton
class SiteStore
@Inject constructor(
    dispatcher: Dispatcher?,
    private val mPostSqlUtils: PostSqlUtils,
    private val mSiteRestClient: SiteRestClient,
    private val mSiteXMLRPCClient: SiteXMLRPCClient,
    private val mPrivateAtomicCookie: PrivateAtomicCookie
) : Store(dispatcher) {
    // Payloads
    data class CompleteQuickStartPayload(val site: SiteModel, val variant: String) : Payload<BaseNetworkError?>()
    data class RefreshSitesXMLRPCPayload(
        @JvmField val username: String = "",
        @JvmField val password: String = "",
        @JvmField val url: String = ""
    ) : Payload<BaseNetworkError?>()

    data class FetchSitesPayload(val filters: List<SiteFilter> = ArrayList()) : Payload<BaseNetworkError?>()

    data class NewSitePayload(
        val siteName: String,
        val language: String,
        val visibility: SiteVisibility,
        val segmentId: Long? = null,
        val siteDesign: String? = null,
        val dryRun: Boolean
    ) : Payload<BaseNetworkError?>() {
        constructor(
            siteName: String, language: String,
            visibility: SiteVisibility, dryRun: Boolean
        ) : this(siteName, language, visibility, null, null, dryRun)

        constructor(
            siteName: String, language: String,
            visibility: SiteVisibility, segmentId: Long?, dryRun: Boolean
        ) : this(siteName, language, visibility, segmentId, null, dryRun)
    }

    data class FetchedPostFormatsPayload(
        val site: SiteModel,
        val postFormats: List<PostFormatModel>
    ) : Payload<PostFormatsError?>()

    data class DesignateMobileEditorForAllSitesPayload(
        val editor: String,
        val setOnlyIfEmpty: Boolean = true
    ) : Payload<SiteEditorsError?>()

    data class DesignateMobileEditorPayload(val site: SiteModel, val editor: String) : Payload<SiteEditorsError?>()
    data class FetchedEditorsPayload(
        val site: SiteModel,
        val webEditor: String,
        val mobileEditor: String
    ) : Payload<SiteEditorsError?>()

    data class FetchBlockLayoutsPayload(
        val site: SiteModel,
        val supportedBlocks: List<String>?,
        val previewWidth: Float?,
        val previewHeight: Float?,
        val scale: Float?,
        val isBeta: Boolean?,
        val preferCache: Boolean?
    ) : Payload<BaseNetworkError?>()

    data class FetchedBlockLayoutsResponsePayload(
        val site: SiteModel,
        val layouts: List<GutenbergLayout>? = null,
        val categories: List<GutenbergLayoutCategory>? = null
    ) : Payload<SiteError?>() {
        constructor(site: SiteModel, error: SiteError?) : this(site) {
            this.error = error
        }
    }

    data class DesignateMobileEditorForAllSitesResponsePayload(val editors: Map<String, String>? = null) : Payload<SiteEditorsError>()
    data class FetchedUserRolesPayload(val site: SiteModel, val roles: List<RoleModel>) : Payload<UserRolesError?>()
    data class FetchedPlansPayload(
        val site: SiteModel,
        val plans: List<PlanModel>? = null
    ) : Payload<PlansError?>() {
        constructor(site: SiteModel, error: PlansError) : this(site) {
            this.error = error
        }
    }

    data class FetchedPrivateAtomicCookiePayload(
        val site: SiteModel,
        val cookie: PrivateAtomicCookieResponse?
    ) : Payload<PrivateAtomicCookieError?>()

    data class FetchPrivateAtomicCookiePayload(val siteId: Long)
    data class FetchJetpackCapabilitiesPayload(val remoteSiteId: Long)
    data class FetchedJetpackCapabilitiesPayload(
        val remoteSiteId: Long,
        val capabilities: List<JetpackCapability> = listOf()
    ) : Payload<JetpackCapabilitiesError?>() {
        constructor(remoteSiteId: Long, error: JetpackCapabilitiesError) : this(remoteSiteId) {
            this.error = error
        }
    }

    data class OnJetpackCapabilitiesFetched(
        val remoteSiteId: Long,
        val capabilities: List<JetpackCapability> = listOf(),
        val error: JetpackCapabilitiesError? = null
    ) : OnChanged<JetpackCapabilitiesError?>()

    data class SuggestDomainsPayload(
        val query: String,
        val onlyWordpressCom: Boolean? = null,
        val includeWordpressCom: Boolean? = null,
        val includeDotBlogSubdomain: Boolean? = null,
        val tlds: String? = null,
        val segmentId: Long? = null,
        val quantity: Int,
        val includeVendorDot: Boolean = false
    ) : Payload<BaseNetworkError?>() {
        constructor(
            query: String, onlyWordpressCom: Boolean, includeWordpressCom: Boolean,
            includeDotBlogSubdomain: Boolean, quantity: Int, includeVendorDot: Boolean
        ) : this(
                query = query,
                onlyWordpressCom = onlyWordpressCom,
                includeWordpressCom = includeWordpressCom,
                includeDotBlogSubdomain = includeDotBlogSubdomain,
                quantity = quantity,
                includeVendorDot = includeVendorDot,
                segmentId = null,
                tlds = null
        )

        constructor(
            query: String, segmentId: Long?,
            quantity: Int, includeVendorDot: Boolean
        ) : this(
                query = query,
                segmentId = segmentId,
                quantity = quantity,
                includeVendorDot = includeVendorDot,
                tlds = null
        )

        constructor(query: String, quantity: Int, tlds: String?) : this(
                query = query,
                quantity = quantity,
                tlds = tlds,
                segmentId = null
        )
    }

    data class SuggestDomainsResponsePayload(
        val query: String,
        val suggestions: List<DomainSuggestionResponse> = listOf()
    ) : Payload<SuggestDomainError?>() {
        constructor(query: String, error: SuggestDomainError?) : this(query) {
            this.error = error
        }
    }

    data class ConnectSiteInfoPayload
    @JvmOverloads constructor(
        val url: String,
        @JvmField val exists: Boolean = false,
        @JvmField val isWordPress: Boolean = false,
        @JvmField val hasJetpack: Boolean = false,
        @JvmField val isJetpackActive: Boolean = false,
        @JvmField val isJetpackConnected: Boolean = false,
        @JvmField val isWPCom: Boolean = false,
        @JvmField val urlAfterRedirects: String? = null
    ) : Payload<SiteError?>() {
        constructor(url: String, error: SiteError?) : this(url) {
            this.error = error
        }

        fun description(): String {
            return String.format(
                    "url: %s, e: %b, wp: %b, jp: %b, wpcom: %b, urlAfterRedirects: %s",
                    url, exists, isWordPress, hasJetpack, isWPCom, urlAfterRedirects
            )
        }
    }

    data class DesignatePrimaryDomainPayload(
        val site: SiteModel,
        val domain: String
    ) : Payload<DesignatePrimaryDomainError?>()

    data class InitiateAutomatedTransferPayload(
        val site: SiteModel,
        val pluginSlugToInstall: String
    ) : Payload<AutomatedTransferError?>()

    data class AutomatedTransferEligibilityResponsePayload
    @JvmOverloads constructor(
        val site: SiteModel,
        val isEligible: Boolean = false,
        val errorCodes: List<String> = listOf()
    ) : Payload<AutomatedTransferError?>() {
        constructor(site: SiteModel, error: AutomatedTransferError) : this(site) {
            this.error = error
        }
    }

    data class InitiateAutomatedTransferResponsePayload
    @JvmOverloads constructor(
        val site: SiteModel,
        val pluginSlugToInstall: String,
        val success: Boolean = false
    ) : Payload<AutomatedTransferError?>()

    data class AutomatedTransferStatusResponsePayload(
        val site: SiteModel,
        val status: String? = null,
        val currentStep: Int = 0,
        val totalSteps: Int = 0
    ) : Payload<AutomatedTransferError?>() {
        constructor(site: SiteModel, error: AutomatedTransferError?) : this(site) {
            this.error = error
        }
    }

    data class DomainAvailabilityResponsePayload(
        val status: DomainAvailabilityStatus? = null,
        val mappable: DomainMappabilityStatus? = null,
        val supportsPrivacy: Boolean = false
    ) : Payload<DomainAvailabilityError?>() {
        constructor(error: DomainAvailabilityError) : this() {
            this.error = error
        }
    }

    data class DomainSupportedStatesResponsePayload(
        val supportedStates: List<SupportedStateResponse>? = null
    ) : Payload<DomainSupportedStatesError?>() {
        constructor(error: DomainSupportedStatesError) : this() {
            this.error = error
        }
    }

    data class DomainSupportedCountriesResponsePayload(
        val supportedCountries: List<SupportedCountryResponse>? = null
    ) : Payload<DomainSupportedCountriesError?>() {
        constructor(error: DomainSupportedCountriesError) : this() {
            this.error = error
        }
    }

    data class SiteError @JvmOverloads constructor(
        @JvmField val type: SiteErrorType,
        @JvmField val message: String = ""
    ) : OnChangedError

    data class SiteEditorsError internal constructor(val type: SiteEditorsErrorType?, val message: String) :
            OnChangedError {
        constructor(type: SiteEditorsErrorType?) : this(type, "") {}
    }

    data class PostFormatsError @JvmOverloads constructor(val type: PostFormatsErrorType, val message: String = "") :
            OnChangedError

    data class UserRolesError internal constructor(
        val type: UserRolesErrorType?,
        val message: String
    ) : OnChangedError {
        constructor(type: UserRolesErrorType?) : this(type, "") {}
    }

    data class NewSiteError(@JvmField val type: NewSiteErrorType, @JvmField val message: String) : OnChangedError
    data class DeleteSiteError(
        @JvmField val type: DeleteSiteErrorType,
        @JvmField val message: String = ""
    ) : OnChangedError {
        constructor(errorType: String, message: String) : this(DeleteSiteErrorType.fromString(errorType), message)
    }

    data class ExportSiteError(@JvmField val type: ExportSiteErrorType) : OnChangedError
    data class AutomatedTransferError(val type: AutomatedTransferErrorType?, val message: String?) : OnChangedError {
        constructor(type: String, message: String) : this(AutomatedTransferErrorType.fromString(type), message)
    }

    data class DomainAvailabilityError
    @JvmOverloads
    constructor(
        @JvmField val type: DomainAvailabilityErrorType,
        val message: String? = null
    ) : OnChangedError

    data class DomainSupportedStatesError
    @JvmOverloads
    constructor(
        @JvmField val type: DomainSupportedStatesErrorType,
        val message: String? = null
    ) : OnChangedError

    data class DomainSupportedCountriesError(
        val type: DomainSupportedCountriesErrorType,
        val message: String?
    ) : OnChangedError

    data class QuickStartError(val type: QuickStartErrorType, val message: String?) : OnChangedError
    data class DesignatePrimaryDomainError(val type: DesignatePrimaryDomainErrorType, val message: String?) :
            OnChangedError

    // OnChanged Events
    data class OnProfileFetched(@JvmField val site: SiteModel) : OnChanged<SiteError?>()
    data class OnSiteChanged(@JvmField val rowsAffected: Int = 0) : OnChanged<SiteError?>() {
        constructor(rowsAffected: Int = 0, siteError: SiteError) : this(rowsAffected) {
            this.error = siteError
        }

        constructor(siteError: SiteError) : this()
    }

    data class OnSiteRemoved(val mRowsAffected: Int) : OnChanged<SiteError?>()
    data class OnAllSitesRemoved(val mRowsAffected: Int) : OnChanged<SiteError?>()
    data class OnBlockLayoutsFetched(
        val layouts: List<GutenbergLayout>?,
        val categories: List<GutenbergLayoutCategory>?
    ) : OnChanged<SiteError?>() {
        constructor(
            layouts: List<GutenbergLayout>?,
            categories: List<GutenbergLayoutCategory>?,
            error: SiteError?
        ) : this(layouts, categories) {
            this.error = error
        }
    }

    data class OnNewSiteCreated(
        @JvmField val dryRun: Boolean = false,
        @JvmField val newSiteRemoteId: Long = 0
    ) : OnChanged<NewSiteError?>() {
        constructor(dryRun: Boolean, newSiteRemoteId: Long, error: NewSiteError) : this(dryRun, newSiteRemoteId) {
            this.error = error
        }
    }

    data class OnSiteDeleted(@JvmField val error: DeleteSiteError?) : OnChanged<DeleteSiteError?>() {
        init {
            this.error = error
        }
    }

    class OnSiteExported() : OnChanged<ExportSiteError?>() {
        constructor(error: ExportSiteError) : this() {
            this.error = error
        }
    }

    data class OnPostFormatsChanged(val site: SiteModel) : OnChanged<PostFormatsError?>()
    data class OnSiteEditorsChanged(val site: SiteModel, val rowsAffected: Int = 0) : OnChanged<SiteEditorsError?>() {
        constructor(site: SiteModel, error: SiteEditorsError) : this(site) {
            this.error = error
        }
    }

    data class OnAllSitesMobileEditorChanged(
        val rowsAffected: Int = 0,
            // True when all sites are self-hosted or wpcom backend response
        val isNetworkResponse: Boolean = false,
        val siteEditorsError: SiteEditorsError? = null
    ) : OnChanged<SiteEditorsError?>() {
        init {
            this.error = siteEditorsError
        }
    }

    data class OnUserRolesChanged(val site: SiteModel) : OnChanged<UserRolesError?>()
    data class OnPlansFetched(
        @JvmField val site: SiteModel,
        @JvmField val plans: List<PlanModel>?
    ) : OnChanged<PlansError?>() {
        constructor(
            site: SiteModel,
            plans: List<PlanModel>?,
            error: PlansError?
        ) : this(site, plans) {
            this.error = error
        }
    }

    data class OnPrivateAtomicCookieFetched(
        val site: SiteModel?,
        val success: Boolean,
        val privateAtomicCookieError: PrivateAtomicCookieError? = null
    ) : OnChanged<PrivateAtomicCookieError?>() {
        init {
            this.error = privateAtomicCookieError
        }
    }

    data class OnURLChecked(
        @JvmField val url: String,
        @JvmField val isWPCom: Boolean = false,
        var siteError: SiteError? = null
    ) :
            OnChanged<SiteError?>() {
        init {
            this.error = siteError
        }
    }

    data class OnConnectSiteInfoChecked(@JvmField val info: ConnectSiteInfoPayload) : OnChanged<SiteError?>()
    data class OnWPComSiteFetched(
        @JvmField val checkedUrl: String,
        @JvmField val site: SiteModel?
    ) : OnChanged<SiteError?>()

    data class SuggestDomainError(@JvmField val type: SuggestDomainErrorType, @JvmField val message: String) :
            OnChangedError {
        constructor(apiErrorType: String, message: String) : this(
                SuggestDomainErrorType.fromString(apiErrorType),
                message
        )
    }

    data class OnSuggestedDomains(
        val query: String,
        @JvmField val suggestions: List<DomainSuggestionResponse>
    ) : OnChanged<SuggestDomainError?>()

    data class OnDomainAvailabilityChecked(
        val status: DomainAvailabilityStatus?,
        val mappable: DomainMappabilityStatus?,
        val supportsPrivacy: Boolean
    ) : OnChanged<DomainAvailabilityError?>() {
        constructor(
            status: DomainAvailabilityStatus?,
            mappable: DomainMappabilityStatus?,
            supportsPrivacy: Boolean,
            error: DomainAvailabilityError?
        ) : this(status, mappable, supportsPrivacy) {
            this.error = error
        }
    }

    enum class DomainAvailabilityStatus {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        TLD_NOT_SUPPORTED,
        TRANSFERRABLE_DOMAIN,
        AVAILABLE,
        UNKNOWN_STATUS;

        companion object {
            @JvmStatic fun fromString(string: String): DomainAvailabilityStatus {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return UNKNOWN_STATUS
            }
        }
    }

    enum class DomainMappabilityStatus {
        BLACKLISTED_DOMAIN, INVALID_TLD, INVALID_DOMAIN, MAPPABLE_DOMAIN, UNKNOWN_STATUS;

        companion object {
            @JvmStatic fun fromString(string: String): DomainMappabilityStatus {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return UNKNOWN_STATUS
            }
        }
    }

    data class OnDomainSupportedStatesFetched(
        val supportedStates: List<SupportedStateResponse>?
    ) : OnChanged<DomainSupportedStatesError?>() {
        constructor(
            supportedStates: List<SupportedStateResponse>?,
            error: DomainSupportedStatesError?
        ) : this(supportedStates) {
            this.error = error
        }
    }

    class OnDomainSupportedCountriesFetched(
        val supportedCountries: List<SupportedCountryResponse>?,
        error: DomainSupportedCountriesError?
    ) : OnChanged<DomainSupportedCountriesError?>() {
        init {
            this.error = error
        }
    }

    class PlansError
    @JvmOverloads constructor(
        @JvmField val type: PlansErrorType,
        @JvmField val message: String? = null
    ) : OnChangedError {
        constructor(type: String?, message: String?) : this(PlansErrorType.fromString(type), message)
    }

    class PrivateAtomicCookieError(val type: AccessCookieErrorType, val message: String) : OnChangedError

    class JetpackCapabilitiesError(val type: JetpackCapabilitiesErrorType, val message: String?) : OnChangedError
    class OnAutomatedTransferEligibilityChecked(
        val site: SiteModel,
        val isEligible: Boolean,
        val eligibilityErrorCodes: List<String>
    ) : OnChanged<AutomatedTransferError?>() {
        constructor(
            site: SiteModel,
            isEligible: Boolean,
            eligibilityErrorCodes: List<String>,
            error: AutomatedTransferError?
        ) : this(site, isEligible, eligibilityErrorCodes) {
            this.error = error
        }
    }

    class OnAutomatedTransferInitiated(
        val site: SiteModel,
        val pluginSlugToInstall: String
    ) : OnChanged<AutomatedTransferError?>() {
        constructor(
            site: SiteModel,
            pluginSlugToInstall: String,
            error: AutomatedTransferError?
        ) : this(site, pluginSlugToInstall) {
            this.error = error
        }
    }

    class OnAutomatedTransferStatusChecked(
        @JvmField val site: SiteModel,
        @JvmField val isCompleted: Boolean = false,
        @JvmField val currentStep: Int = 0,
        @JvmField val totalSteps: Int = 0
    ) : OnChanged<AutomatedTransferError?>() {
        constructor(site: SiteModel, error: AutomatedTransferError?) : this(site) {
            this.error = error
        }
    }

    class QuickStartCompletedResponsePayload(val site: SiteModel, val success: Boolean) : OnChanged<QuickStartError?>()
    class OnQuickStartCompleted internal constructor(
        val site: SiteModel,
        val success: Boolean
    ) : OnChanged<QuickStartError?>()

    class DesignatedPrimaryDomainPayload(
        val site: SiteModel,
        val success: Boolean
    ) : OnChanged<DesignatePrimaryDomainError?>()

    class OnPrimaryDomainDesignated(
        val site: SiteModel,
        val success: Boolean
    ) : OnChanged<DesignatePrimaryDomainError?>()

    data class UpdateSitesResult(
        @JvmField val rowsAffected: Int = 0,
        @JvmField val duplicateSiteFound: Boolean = false
    )

    enum class SiteErrorType {
        INVALID_SITE, UNKNOWN_SITE, DUPLICATE_SITE, INVALID_RESPONSE, UNAUTHORIZED, GENERIC_ERROR
    }

    enum class SuggestDomainErrorType {
        EMPTY_RESULTS, EMPTY_QUERY, INVALID_MINIMUM_QUANTITY, INVALID_MAXIMUM_QUANTITY, INVALID_QUERY, GENERIC_ERROR;

        companion object {
            fun fromString(string: String): SuggestDomainErrorType {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class PostFormatsErrorType {
        INVALID_SITE, INVALID_RESPONSE, GENERIC_ERROR
    }

    enum class PlansErrorType {
        NOT_AVAILABLE, AUTHORIZATION_REQUIRED, UNAUTHORIZED, UNKNOWN_BLOG, GENERIC_ERROR;

        companion object {
            fun fromString(type: String?): PlansErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class AccessCookieErrorType {
        GENERIC_ERROR, INVALID_RESPONSE, SITE_MISSING_FROM_STORE, NON_PRIVATE_AT_SITE
    }

    enum class UserRolesErrorType {
        GENERIC_ERROR
    }

    enum class SiteEditorsErrorType {
        GENERIC_ERROR
    }

    enum class JetpackCapabilitiesErrorType {
        GENERIC_ERROR
    }

    enum class DeleteSiteErrorType {
        INVALID_SITE, UNAUTHORIZED,  // user don't have permission to delete
        AUTHORIZATION_REQUIRED,  // missing access token
        GENERIC_ERROR;

        companion object {
            fun fromString(string: String): DeleteSiteErrorType {
                if (!TextUtils.isEmpty(string)) {
                    if (string == "unauthorized") {
                        return UNAUTHORIZED
                    } else if (string == "authorization_required") {
                        return AUTHORIZATION_REQUIRED
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class ExportSiteErrorType {
        INVALID_SITE, GENERIC_ERROR
    }

    // Enums
    enum class NewSiteErrorType {
        SITE_NAME_REQUIRED,
        SITE_NAME_NOT_ALLOWED,
        SITE_NAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS,
        SITE_NAME_MUST_BE_LESS_THAN_SIXTY_FOUR_CHARACTERS,
        SITE_NAME_CONTAINS_INVALID_CHARACTERS,
        SITE_NAME_CANT_BE_USED,
        SITE_NAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS,
        SITE_NAME_MUST_INCLUDE_LETTERS,
        SITE_NAME_EXISTS,
        SITE_NAME_RESERVED,
        SITE_NAME_RESERVED_BUT_MAY_BE_AVAILABLE,
        SITE_NAME_INVALID,
        SITE_TITLE_INVALID,
        GENERIC_ERROR;

        companion object {
            // SiteStore semantics prefers SITE over BLOG but errors reported from the API use BLOG
            // these are used to convert API errors to the appropriate enum value in fromString
            private const val BLOG = "BLOG"
            private const val SITE = "SITE"
            @JvmStatic fun fromString(string: String): NewSiteErrorType {
                if (!TextUtils.isEmpty(string)) {
                    val siteString = string.toUpperCase(Locale.US).replace(BLOG, SITE)
                    for (v in values()) {
                        if (siteString.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class AutomatedTransferErrorType {
        AT_NOT_ELIGIBLE,  // occurs if AT is initiated when the site is not eligible
        NOT_FOUND,  // occurs if transfer status of a site with no active transfer is checked
        GENERIC_ERROR;

        companion object {
            fun fromString(type: String?): AutomatedTransferErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class DomainAvailabilityErrorType {
        INVALID_DOMAIN_NAME, GENERIC_ERROR
    }

    enum class DomainSupportedStatesErrorType {
        INVALID_COUNTRY_CODE, INVALID_QUERY, GENERIC_ERROR;

        companion object {
            @JvmStatic fun fromString(type: String): DomainSupportedStatesErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class DomainSupportedCountriesErrorType {
        GENERIC_ERROR
    }

    enum class QuickStartErrorType {
        GENERIC_ERROR
    }

    enum class DesignatePrimaryDomainErrorType {
        GENERIC_ERROR
    }

    enum class SiteVisibility(private val mValue: Int) {
        PRIVATE(-1), BLOCK_SEARCH_ENGINE(0), PUBLIC(1);

        fun value(): Int {
            return mValue
        }
    }

    enum class CompleteQuickStartVariant(private val mString: String) {
        NEXT_STEPS("next-steps");

        override fun toString(): String {
            return mString
        }
    }

    enum class SiteFilter(private val mString: String) {
        ATOMIC("atomic"), JETPACK("jetpack"), WPCOM("wpcom");

        override fun toString(): String {
            return mString
        }
    }

    override fun onRegister() {
        AppLog.d(API, "SiteStore onRegister")
    }

    /**
     * Returns all sites in the store as a [SiteModel] list.
     */
    val sites: List<SiteModel>
        get() = WellSql.select(SiteModel::class.java).asModel

    /**
     * Returns the number of sites of any kind in the store.
     */
    val sitesCount: Int
        get() = WellSql.select(SiteModel::class.java).count().toInt()

    /**
     * Checks whether the store contains any sites of any kind.
     */
    fun hasSite(): Boolean {
        return sitesCount != 0
    }

    /**
     * Obtains the site with the given (local) id and returns it as a [SiteModel].
     */
    fun getSiteByLocalId(id: Int): SiteModel? {
        val result = SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).asModel
        return if (result.size > 0) {
            result[0]
        } else null
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    fun hasSiteWithLocalId(id: Int): Boolean {
        return SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).exists()
    }

    /**
     * Returns all .COM sites in the store.
     */
    val wPComSites: List<SiteModel>
        get() = SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).asModel

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API).
     */
    val sitesAccessedViaWPComRest: List<SiteModel>
        get() = SiteSqlUtils.getSitesAccessedViaWPComRest().asModel

    /**
     * Returns the number of sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected
     * via WPCom REST API).
     */
    val sitesAccessedViaWPComRestCount: Int
        get() = SiteSqlUtils.getSitesAccessedViaWPComRest().count().toInt()

    /**
     * Checks whether the store contains at least one site accessed via WPCom REST API (WPCom sites or Jetpack
     * sites connected via WPCom REST API).
     */
    fun hasSitesAccessedViaWPComRest(): Boolean {
        return sitesAccessedViaWPComRestCount != 0
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    val wPComSitesCount: Int
        get() = SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).count().toInt()

    /**
     * Returns the number of .COM Atomic sites in the store.
     */
    val wPComAtomicSitesCount: Int
        get() = SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM_ATOMIC, true).count().toInt()

    /**
     * Returns sites with a name or url matching the search string.
     */
    fun getSitesByNameOrUrlMatching(searchString: String): List<SiteModel> {
        return SiteSqlUtils.getSitesByNameOrUrlMatching(searchString)
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API) with a
     * name or url matching the search string.
     */
    fun getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString: String): List<SiteModel> {
        return SiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString)
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    fun hasWPComSite(): Boolean {
        return wPComSitesCount != 0
    }

    /**
     * Checks whether the store contains at least one .COM Atomic site.
     */
    fun hasWPComAtomicSite(): Boolean {
        return wPComAtomicSitesCount != 0
    }

    /**
     * Returns sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    val sitesAccessedViaXMLRPC: List<SiteModel>
        get() = SiteSqlUtils.getSitesAccessedViaXMLRPC().asModel

    /**
     * Returns the number of sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    val sitesAccessedViaXMLRPCCount: Int
        get() = SiteSqlUtils.getSitesAccessedViaXMLRPC().count().toInt()

    /**
     * Checks whether the store contains at least one site accessed via XMLRPC (self-hosted sites or
     * Jetpack sites accessed via XMLRPC).
     */
    fun hasSiteAccessedViaXMLRPC(): Boolean {
        return sitesAccessedViaXMLRPCCount != 0
    }

    /**
     * Returns all visible sites as [SiteModel]s. All self-hosted sites over XML-RPC are visible by default.
     */
    val visibleSites: List<SiteModel>
        get() = SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).asModel

    /**
     * Returns the number of visible sites. All self-hosted sites over XML-RPC are visible by default.
     */
    val visibleSitesCount: Int
        get() = SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).count().toInt()

    /**
     * Returns all visible .COM sites as [SiteModel]s.
     */
    val visibleSitesAccessedViaWPCom: List<SiteModel>
        get() = SiteSqlUtils.getVisibleSitesAccessedViaWPCom().asModel

    /**
     * Returns the number of visible .COM sites.
     */
    val visibleSitesAccessedViaWPComCount: Int
        get() = SiteSqlUtils.getVisibleSitesAccessedViaWPCom().count().toInt()

    /**
     * Checks whether the .COM site with the given (local) id is visible.
     */
    fun isWPComSiteVisibleByLocalId(id: Int): Boolean {
        return WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .equals(SiteModelTable.IS_WPCOM, true)
                .equals(SiteModelTable.IS_VISIBLE, true)
                .endGroup().endWhere()
                .exists()
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    fun getLocalIdForRemoteSiteId(siteId: Long): Int {
        val sites = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, siteId)
                .endGroup().endWhere()
                .getAsModel { cursor ->
                    val siteModel = SiteModel()
                    siteModel.id = cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID))
                    siteModel
                }
        return if (sites.size > 0) {
            sites[0].id
        } else 0
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    fun getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(selfHostedSiteId: Long, xmlRpcUrl: String?): Int {
        val sites = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, selfHostedSiteId)
                .equals(SiteModelTable.XMLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel { cursor ->
                    val siteModel = SiteModel()
                    siteModel.id = cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID))
                    siteModel
                }
        return if (sites.size > 0) {
            sites[0].id
        } else 0
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    fun getSiteIdForLocalId(id: Int): Long {
        val result = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel { cursor ->
                    val siteModel = SiteModel()
                    siteModel.siteId = cursor.getInt(cursor.getColumnIndex(SiteModelTable.SITE_ID)).toLong()
                    siteModel.selfHostedSiteId = cursor.getLong(
                            cursor.getColumnIndex(SiteModelTable.SELF_HOSTED_SITE_ID)
                    )
                    siteModel
                }
        if (result.isEmpty()) {
            return 0
        }
        return if (result[0].siteId > 0) {
            result[0].siteId
        } else {
            result[0].selfHostedSiteId
        }
    }

    /**
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * [SiteModel].
     */
    fun getSiteBySiteId(siteId: Long): SiteModel? {
        if (siteId == 0L) {
            return null
        }
        val sites = SiteSqlUtils.getSitesWith(SiteModelTable.SITE_ID, siteId).asModel
        return if (sites.isEmpty()) {
            null
        } else {
            sites[0]
        }
    }

    /**
     * Gets the cached content of a page layout
     *
     * @param site the current site
     * @param slug the slug of the layout
     * @return the content or null if the content is not cached
     */
    fun getBlockLayoutContent(site: SiteModel, slug: String): String? {
        return SiteSqlUtils.getBlockLayoutContent(site, slug)
    }

    /**
     * Gets the cached page layout
     *
     * @param site the current site
     * @param slug the slug of the layout
     * @return the layout or null if the layout is not cached
     */
    fun getBlockLayout(site: SiteModel, slug: String): GutenbergLayout? {
        return SiteSqlUtils.getBlockLayout(site, slug)
    }

    fun getPostFormats(site: SiteModel?): List<PostFormatModel> {
        return SiteSqlUtils.getPostFormats(site!!)
    }

    fun getUserRoles(site: SiteModel?): List<RoleModel> {
        return SiteSqlUtils.getUserRoles(site!!)
    }

    @Subscribe(threadMode = ASYNC) override fun onAction(action: Action<*>) {
        val actionType = action.type as? SiteAction ?: return
        when (actionType) {
            FETCH_PROFILE_XML_RPC -> fetchProfileXmlRpc(action.payload as SiteModel)
            FETCHED_PROFILE_XML_RPC -> updateSiteProfile(action.payload as SiteModel)
            FETCH_SITE -> fetchSite(action.payload as SiteModel)
            FETCH_SITES -> fetchSites(action.payload as FetchSitesPayload)
            FETCHED_SITES -> handleFetchedSitesWPComRest(action.payload as SitesModel)
            FETCH_SITES_XML_RPC -> fetchSitesXmlRpc(action.payload as RefreshSitesXMLRPCPayload)
            FETCHED_SITES_XML_RPC -> updateSites(action.payload as SitesModel)
            UPDATE_SITE -> updateSite(action.payload as SiteModel)
            UPDATE_SITES -> updateSites(action.payload as SitesModel)
            DELETE_SITE -> deleteSite(action.payload as SiteModel)
            DELETED_SITE -> handleDeletedSite(action.payload as DeleteSiteResponsePayload)
            EXPORT_SITE -> exportSite(action.payload as SiteModel)
            EXPORTED_SITE -> handleExportedSite(action.payload as ExportSiteResponsePayload)
            REMOVE_SITE -> removeSite(action.payload as SiteModel)
            REMOVE_ALL_SITES -> removeAllSites()
            REMOVE_WPCOM_AND_JETPACK_SITES -> removeWPComAndJetpackSites()
            SHOW_SITES -> toggleSitesVisibility(action.payload as SitesModel, true)
            HIDE_SITES -> toggleSitesVisibility(action.payload as SitesModel, false)
            CREATE_NEW_SITE -> createNewSite(action.payload as NewSitePayload)
            CREATED_NEW_SITE -> handleCreateNewSiteCompleted(action.payload as NewSiteResponsePayload)
            FETCH_POST_FORMATS -> fetchPostFormats(action.payload as SiteModel)
            FETCHED_POST_FORMATS -> updatePostFormats(action.payload as FetchedPostFormatsPayload)
            FETCH_SITE_EDITORS -> fetchSiteEditors(action.payload as SiteModel)
            FETCH_BLOCK_LAYOUTS -> fetchBlockLayouts(action.payload as FetchBlockLayoutsPayload)
            FETCHED_BLOCK_LAYOUTS -> handleFetchedBlockLayouts(action.payload as FetchedBlockLayoutsResponsePayload)
            DESIGNATE_MOBILE_EDITOR -> designateMobileEditor(action.payload as DesignateMobileEditorPayload)
            DESIGNATE_MOBILE_EDITOR_FOR_ALL_SITES -> designateMobileEditorForAllSites(action.payload as DesignateMobileEditorForAllSitesPayload)
            FETCHED_SITE_EDITORS -> updateSiteEditors(action.payload as FetchedEditorsPayload)
            DESIGNATED_MOBILE_EDITOR_FOR_ALL_SITES -> handleDesignatedMobileEditorForAllSites(
                    action.payload as DesignateMobileEditorForAllSitesResponsePayload
            )
            FETCH_USER_ROLES -> fetchUserRoles(action.payload as SiteModel)
            FETCHED_USER_ROLES -> updateUserRoles(action.payload as FetchedUserRolesPayload)
            FETCH_CONNECT_SITE_INFO -> fetchConnectSiteInfo(action.payload as String)
            FETCHED_CONNECT_SITE_INFO -> handleFetchedConnectSiteInfo(action.payload as ConnectSiteInfoPayload)
            FETCH_WPCOM_SITE_BY_URL -> fetchWPComSiteByUrl(action.payload as String)
            FETCHED_WPCOM_SITE_BY_URL -> handleFetchedWPComSiteByUrl(action.payload as FetchWPComSiteResponsePayload)
            IS_WPCOM_URL -> checkUrlIsWPCom(action.payload as String)
            CHECKED_IS_WPCOM_URL -> handleCheckedIsWPComUrl(action.payload as IsWPComResponsePayload)
            SUGGEST_DOMAINS -> suggestDomains(action.payload as SuggestDomainsPayload)
            SUGGESTED_DOMAINS -> handleSuggestedDomains(action.payload as SuggestDomainsResponsePayload)
            FETCH_PLANS -> fetchPlans(action.payload as SiteModel)
            FETCHED_PLANS -> handleFetchedPlans(action.payload as FetchedPlansPayload)
            CHECK_DOMAIN_AVAILABILITY -> checkDomainAvailability(action.payload as String)
            CHECKED_DOMAIN_AVAILABILITY -> handleCheckedDomainAvailability(action.payload as DomainAvailabilityResponsePayload)
            FETCH_DOMAIN_SUPPORTED_STATES -> fetchSupportedStates(action.payload as String)
            FETCHED_DOMAIN_SUPPORTED_STATES -> handleFetchedSupportedStates(action.payload as DomainSupportedStatesResponsePayload)
            FETCH_DOMAIN_SUPPORTED_COUNTRIES -> mSiteRestClient.fetchSupportedCountries()
            FETCHED_DOMAIN_SUPPORTED_COUNTRIES -> handleFetchedSupportedCountries(action.payload as DomainSupportedCountriesResponsePayload)
            CHECK_AUTOMATED_TRANSFER_ELIGIBILITY -> checkAutomatedTransferEligibility(action.payload as SiteModel)
            INITIATE_AUTOMATED_TRANSFER -> initiateAutomatedTransfer(action.payload as InitiateAutomatedTransferPayload)
            CHECK_AUTOMATED_TRANSFER_STATUS -> checkAutomatedTransferStatus(action.payload as SiteModel)
            CHECKED_AUTOMATED_TRANSFER_ELIGIBILITY -> handleCheckedAutomatedTransferEligibility(action.payload as AutomatedTransferEligibilityResponsePayload)
            INITIATED_AUTOMATED_TRANSFER -> handleInitiatedAutomatedTransfer(action.payload as InitiateAutomatedTransferResponsePayload)
            CHECKED_AUTOMATED_TRANSFER_STATUS -> handleCheckedAutomatedTransferStatus(action.payload as AutomatedTransferStatusResponsePayload)
            COMPLETE_QUICK_START -> completeQuickStart(action.payload as CompleteQuickStartPayload)
            COMPLETED_QUICK_START -> handleQuickStartCompleted(action.payload as QuickStartCompletedResponsePayload)
            DESIGNATE_PRIMARY_DOMAIN -> designatePrimaryDomain(action.payload as DesignatePrimaryDomainPayload)
            DESIGNATED_PRIMARY_DOMAIN -> handleDesignatedPrimaryDomain(action.payload as DesignatedPrimaryDomainPayload)
            FETCH_PRIVATE_ATOMIC_COOKIE -> fetchPrivateAtomicCookie(action.payload as FetchPrivateAtomicCookiePayload)
            FETCHED_PRIVATE_ATOMIC_COOKIE -> handleFetchedPrivateAtomicCookie(action.payload as FetchedPrivateAtomicCookiePayload)
            FETCH_JETPACK_CAPABILITIES -> fetchJetpackCapabilities(action.payload as FetchJetpackCapabilitiesPayload)
            FETCHED_JETPACK_CAPABILITIES -> handleFetchedJetpackCapabilities(action.payload as FetchedJetpackCapabilitiesPayload)
        }
    }

    private fun fetchProfileXmlRpc(site: SiteModel) {
        mSiteXMLRPCClient.fetchProfile(site)
    }

    private fun fetchSite(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            mSiteRestClient.fetchSite(site)
        } else {
            mSiteXMLRPCClient.fetchSite(site)
        }
    }

    private fun fetchSites(payload: FetchSitesPayload) {
        mSiteRestClient.fetchSites(payload.filters)
    }

    private fun fetchSitesXmlRpc(payload: RefreshSitesXMLRPCPayload) {
        mSiteXMLRPCClient.fetchSites(payload.url, payload.username, payload.password)
    }

    private fun updateSiteProfile(siteModel: SiteModel) {
        val event = OnProfileFetched(siteModel)
        if (siteModel.isError) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(siteModel.error)
        } else {
            try {
                SiteSqlUtils.insertOrUpdateSite(siteModel)
            } catch (e: DuplicateSiteException) {
                event.error = SiteError(DUPLICATE_SITE)
            }
        }
        emitChange(event)
    }

    private fun updateSite(siteModel: SiteModel) {
        val event = if (siteModel.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(siteModel.error))
        } else {
            try {
                // The REST API doesn't return info about the editor(s). Make sure to copy current values
                // available on the DB. Otherwise the apps will receive an update site without editor prefs set.
                // The apps will dispatch the action to update editor(s) when necessary.
                val freshSiteFromDB = getSiteByLocalId(siteModel.id)
                if (freshSiteFromDB != null) {
                    siteModel.mobileEditor = freshSiteFromDB.mobileEditor
                    siteModel.webEditor = freshSiteFromDB.webEditor
                }
                OnSiteChanged(SiteSqlUtils.insertOrUpdateSite(siteModel))
            } catch (e: DuplicateSiteException) {
                OnSiteChanged(SiteError(DUPLICATE_SITE))
            }
        }
        emitChange(event)
    }

    private fun updateSites(sitesModel: SitesModel) {
        val event = if (sitesModel.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(sitesModel.error))
        } else {
            val res = createOrUpdateSites(sitesModel)
            if (res.duplicateSiteFound) {
                OnSiteChanged(res.rowsAffected, SiteError(DUPLICATE_SITE))
            } else {
                OnSiteChanged(res.rowsAffected)
            }
        }
        emitChange(event)
    }

    private fun handleFetchedSitesWPComRest(fetchedSites: SitesModel) {
        val event = if (fetchedSites.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(fetchedSites.error))
        } else {
            val res = createOrUpdateSites(fetchedSites)
            val result = if (res.duplicateSiteFound) {
                OnSiteChanged(res.rowsAffected, SiteError(DUPLICATE_SITE))
            } else {
                OnSiteChanged(res.rowsAffected)
            }
            SiteSqlUtils.removeWPComRestSitesAbsentFromList(mPostSqlUtils, fetchedSites.sites)
            result
        }
        emitChange(event)
    }

    private fun createOrUpdateSites(sites: SitesModel): UpdateSitesResult {
        var rowsAffected = 0
        var duplicateSiteFound = false
        for (site in sites.sites) {
            try {
                // The REST API doesn't return info about the editor(s). Make sure to copy current values
                // available on the DB. Otherwise the apps will receive an update site without editor prefs set.
                // The apps will dispatch the action to update editor(s) when necessary.
                val siteFromDB = getSiteBySiteId(site.siteId)
                if (siteFromDB != null) {
                    site.mobileEditor = siteFromDB.mobileEditor
                    site.webEditor = siteFromDB.webEditor
                }
                rowsAffected += SiteSqlUtils.insertOrUpdateSite(site)
            } catch (caughtException: DuplicateSiteException) {
                duplicateSiteFound = true
            }
        }
        return UpdateSitesResult(rowsAffected, duplicateSiteFound)
    }

    private fun deleteSite(site: SiteModel) {
        // Not available for Jetpack sites
        if (!site.isWPCom) {
            val event = OnSiteDeleted(DeleteSiteError(INVALID_SITE))
            emitChange(event)
            return
        }
        mSiteRestClient.deleteSite(site)
    }

    private fun handleDeletedSite(payload: DeleteSiteResponsePayload) {
        val event = OnSiteDeleted(payload.error)
        if (!payload.isError) {
            SiteSqlUtils.deleteSite(payload.site)
        }
        emitChange(event)
    }

    private fun exportSite(site: SiteModel) {
        // Not available for Jetpack sites
        if (!site.isWPCom) {
            emitChange(OnSiteExported(ExportSiteError(ExportSiteErrorType.INVALID_SITE)))
            return
        }
        mSiteRestClient.exportSite(site)
    }

    private fun handleExportedSite(payload: ExportSiteResponsePayload) {
        val event = if (payload.isError) {
            // TODO: what kind of error could we get here?
            OnSiteExported(ExportSiteError(GENERIC_ERROR))
        } else {
            OnSiteExported()
        }
        emitChange(event)
    }

    private fun removeSite(site: SiteModel) {
        val rowsAffected = SiteSqlUtils.deleteSite(site)
        emitChange(OnSiteRemoved(rowsAffected))
    }

    private fun removeAllSites() {
        val rowsAffected = SiteSqlUtils.deleteAllSites()
        val event = OnAllSitesRemoved(rowsAffected)
        emitChange(event)
    }

    private fun removeWPComAndJetpackSites() {
        // Logging out of WP.com. Drop all WP.com sites, and all Jetpack sites that were fetched over the WP.com
        // REST API only (they don't have a .org site id)
        val wpcomAndJetpackSites = SiteSqlUtils.getSitesAccessedViaWPComRest().asModel
        val rowsAffected = removeSites(wpcomAndJetpackSites)
        emitChange(OnSiteRemoved(rowsAffected))
    }

    private fun toggleSitesVisibility(sites: SitesModel, visible: Boolean): Int {
        var rowsAffected = 0
        for (site in sites.sites) {
            rowsAffected += SiteSqlUtils.setSiteVisibility(site, visible)
        }
        return rowsAffected
    }

    private fun createNewSite(payload: NewSitePayload) {
        mSiteRestClient.newSite(
                payload.siteName, payload.language, payload.visibility,
                payload.segmentId, payload.siteDesign, payload.dryRun
        )
    }

    private fun handleCreateNewSiteCompleted(payload: NewSiteResponsePayload) {
        emitChange(OnNewSiteCreated(payload.dryRun, payload.newSiteRemoteId, payload.error))
    }

    private fun fetchPostFormats(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            mSiteRestClient.fetchPostFormats(site)
        } else {
            mSiteXMLRPCClient.fetchPostFormats(site)
        }
    }

    private fun updatePostFormats(payload: FetchedPostFormatsPayload) {
        val event = OnPostFormatsChanged(payload.site)
        if (payload.isError) {
            event.error = payload.error
        } else {
            SiteSqlUtils.insertOrReplacePostFormats(payload.site, payload.postFormats)
        }
        emitChange(event)
    }

    private fun fetchSiteEditors(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            mSiteRestClient.fetchSiteEditors(site)
        }
    }

    private fun fetchBlockLayouts(payload: FetchBlockLayoutsPayload) {
        if (payload.preferCache == true && cachedLayoutsRetrieved(payload.site)) return
        if (payload.site.isUsingWpComRestApi) {
            mSiteRestClient
                    .fetchWpComBlockLayouts(
                            payload.site, payload.supportedBlocks,
                            payload.previewWidth, payload.previewHeight, payload.scale, payload.isBeta
                    )
        } else {
            mSiteRestClient.fetchSelfHostedBlockLayouts(
                    payload.site, payload.supportedBlocks,
                    payload.previewWidth, payload.previewHeight, payload.scale, payload.isBeta
            )
        }
    }

    private fun designateMobileEditor(payload: DesignateMobileEditorPayload) {
        // wpcom sites sync the new value with the backend
        if (payload.site.isUsingWpComRestApi) {
            mSiteRestClient.designateMobileEditor(payload.site, payload.editor)
        }

        // Update the editor pref on the DB, and emit the change immediately
        val site = payload.site
        site.mobileEditor = payload.editor
        val event = try {
            OnSiteEditorsChanged(site, SiteSqlUtils.insertOrUpdateSite(site))
        } catch (e: Exception) {
            OnSiteEditorsChanged(site, SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR))
        }
        emitChange(event)
    }

    private fun designateMobileEditorForAllSites(payload: DesignateMobileEditorForAllSitesPayload) {
        var rowsAffected = 0
        var wpcomPostRequestRequired = false
        var error: SiteEditorsError? = null
        for (site in sites) {
            site.mobileEditor = payload.editor
            if (!wpcomPostRequestRequired && site.isUsingWpComRestApi) {
                wpcomPostRequestRequired = true
            }
            try {
                rowsAffected += SiteSqlUtils.insertOrUpdateSite(site)
            } catch (e: Exception) {
                error = SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR)
            }
        }
        val isNetworkResponse = if (wpcomPostRequestRequired) {
            mSiteRestClient.designateMobileEditorForAllSites(payload.editor, payload.setOnlyIfEmpty)
            false
        } else {
            true
        }

        emitChange(OnAllSitesMobileEditorChanged(rowsAffected, isNetworkResponse, error))
    }

    private fun updateSiteEditors(payload: FetchedEditorsPayload) {
        val site = payload.site
        val event = if (payload.isError) {
            OnSiteEditorsChanged(site, payload.error ?: SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR))
        } else {
            site.mobileEditor = payload.mobileEditor
            site.webEditor = payload.webEditor
            try {
                OnSiteEditorsChanged(site, SiteSqlUtils.insertOrUpdateSite(site))
            } catch (e: Exception) {
                OnSiteEditorsChanged(site, SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR))
            }
        }
        emitChange(event)
    }

    private fun handleDesignatedMobileEditorForAllSites(payload: DesignateMobileEditorForAllSitesResponsePayload) {
        val event = if (payload.isError) {
            OnAllSitesMobileEditorChanged(siteEditorsError = payload.error)
        } else {
            var rowsAffected = 0
            var error: SiteEditorsError? = null
            // Loop over the returned sites and make sure we've the fresh values for editor prop stored locally
            for ((key, value) in payload.editors ?: mapOf()) {
                val currentModel = getSiteBySiteId(key.toLong())
                if (currentModel == null) {
                    // this could happen when a site was added to the current account with another app, or on the web
                    AppLog.e(
                            API, "handleDesignatedMobileEditorForAllSites - The backend returned info for "
                            + "the following siteID " + key + " but there is no site with that "
                            + "remote ID in SiteStore."
                    )
                    continue
                }
                if (currentModel.mobileEditor == null
                        || currentModel.mobileEditor != value) {
                    // the current editor is either null or != from the value on the server. Update it
                    currentModel.mobileEditor = value
                    try {
                        rowsAffected += SiteSqlUtils.insertOrUpdateSite(currentModel)
                    } catch (e: Exception) {
                        error = SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR)
                    }
                }
            }
            OnAllSitesMobileEditorChanged(rowsAffected, true, error)
        }
        emitChange(event)
    }

    private fun fetchUserRoles(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            mSiteRestClient.fetchUserRoles(site)
        }
    }

    private fun updateUserRoles(payload: FetchedUserRolesPayload) {
        val event = OnUserRolesChanged(payload.site)
        if (payload.isError) {
            event.error = payload.error
        } else {
            SiteSqlUtils.insertOrReplaceUserRoles(payload.site, payload.roles)
        }
        emitChange(event)
    }

    private fun removeSites(sites: List<SiteModel>): Int {
        var rowsAffected = 0
        for (site in sites) {
            rowsAffected += SiteSqlUtils.deleteSite(site)
        }
        return rowsAffected
    }

    private fun fetchConnectSiteInfo(payload: String) {
        mSiteRestClient.fetchConnectSiteInfo(payload)
    }

    private fun handleFetchedConnectSiteInfo(payload: ConnectSiteInfoPayload) {
        val event = OnConnectSiteInfoChecked(payload)
        event.error = payload.error
        emitChange(event)
    }

    private fun fetchWPComSiteByUrl(payload: String) {
        mSiteRestClient.fetchWPComSiteByUrl(payload)
    }

    private fun handleFetchedWPComSiteByUrl(payload: FetchWPComSiteResponsePayload) {
        val event = OnWPComSiteFetched(payload.checkedUrl, payload.site)
        event.error = payload.error
        emitChange(event)
    }

    private fun checkUrlIsWPCom(payload: String) {
        mSiteRestClient.checkUrlIsWPCom(payload)
    }

    private fun handleCheckedIsWPComUrl(payload: IsWPComResponsePayload) {
        val error = if (payload.isError) {
            // Return invalid site for all errors (this endpoint seems a bit drunk).
            // Client likely needs to know if there was an error or not.
            SiteError(SiteErrorType.INVALID_SITE)
        } else {
            null
        }
        emitChange(OnURLChecked(payload.url, payload.isWPCom, error))
    }

    private fun suggestDomains(payload: SuggestDomainsPayload) {
        mSiteRestClient.suggestDomains(
                payload.query, payload.onlyWordpressCom, payload.includeWordpressCom,
                payload.includeDotBlogSubdomain, payload.segmentId, payload.quantity, payload.includeVendorDot,
                payload.tlds
        )
    }

    private fun handleSuggestedDomains(payload: SuggestDomainsResponsePayload) {
        val event = OnSuggestedDomains(payload.query, payload.suggestions)
        if (payload.isError) {
            event.error = payload.error
        }
        emitChange(event)
    }

    private fun fetchPrivateAtomicCookie(payload: FetchPrivateAtomicCookiePayload) {
        val site = getSiteBySiteId(payload.siteId)
        if (site == null) {
            val cookieError = PrivateAtomicCookieError(
                    SITE_MISSING_FROM_STORE,
                    "Requested site is missing from the store."
            )
            emitChange(OnPrivateAtomicCookieFetched(null, false, cookieError))
            return
        }
        if (!site.isPrivateWPComAtomic) {
            val cookieError = PrivateAtomicCookieError(
                    NON_PRIVATE_AT_SITE,
                    "Cookie can only be requested for private atomic site."
            )
            emitChange(OnPrivateAtomicCookieFetched(site, false, cookieError))
            return
        }
        mSiteRestClient.fetchAccessCookie(site)
    }

    private fun handleFetchedPrivateAtomicCookie(payload: FetchedPrivateAtomicCookiePayload) {
        if (payload.cookie == null || payload.cookie.cookies.isEmpty()) {
            emitChange(
                    OnPrivateAtomicCookieFetched(
                            payload.site, false,
                            PrivateAtomicCookieError(
                                    INVALID_RESPONSE,
                                    "Cookie is missing from response."
                            )
                    )
            )
            mPrivateAtomicCookie.set(null)
            return
        }
        mPrivateAtomicCookie.set(payload.cookie.cookies[0])
        emitChange(OnPrivateAtomicCookieFetched(payload.site, true, payload.error))
    }

    private fun fetchJetpackCapabilities(payload: FetchJetpackCapabilitiesPayload) {
        mSiteRestClient.fetchJetpackCapabilities(payload.remoteSiteId)
    }

    private fun handleFetchedJetpackCapabilities(payload: FetchedJetpackCapabilitiesPayload) {
        emitChange(OnJetpackCapabilitiesFetched(payload.remoteSiteId, payload.capabilities, payload.error))
    }

    private fun fetchPlans(siteModel: SiteModel) {
        if (siteModel.isUsingWpComRestApi) {
            mSiteRestClient.fetchPlans(siteModel)
        } else {
            val plansError = PlansError(NOT_AVAILABLE)
            handleFetchedPlans(FetchedPlansPayload(siteModel, plansError))
        }
    }

    private fun handleFetchedPlans(payload: FetchedPlansPayload) {
        emitChange(OnPlansFetched(payload.site, payload.plans, payload.error))
    }

    private fun checkDomainAvailability(domainName: String) {
        if (TextUtils.isEmpty(domainName)) {
            val error = DomainAvailabilityError(INVALID_DOMAIN_NAME)
            handleCheckedDomainAvailability(DomainAvailabilityResponsePayload(error))
        } else {
            mSiteRestClient.checkDomainAvailability(domainName)
        }
    }

    private fun handleCheckedDomainAvailability(payload: DomainAvailabilityResponsePayload) {
        emitChange(
                OnDomainAvailabilityChecked(
                        payload.status,
                        payload.mappable,
                        payload.supportsPrivacy,
                        payload.error
                )
        )
    }

    private fun fetchSupportedStates(countryCode: String) {
        if (TextUtils.isEmpty(countryCode)) {
            val error = DomainSupportedStatesError(INVALID_COUNTRY_CODE)
            handleFetchedSupportedStates(DomainSupportedStatesResponsePayload(error))
        } else {
            mSiteRestClient.fetchSupportedStates(countryCode)
        }
    }

    private fun handleFetchedSupportedStates(payload: DomainSupportedStatesResponsePayload) {
        emitChange(OnDomainSupportedStatesFetched(payload.supportedStates, payload.error))
    }

    private fun handleFetchedSupportedCountries(payload: DomainSupportedCountriesResponsePayload) {
        emitChange(OnDomainSupportedCountriesFetched(payload.supportedCountries, payload.error))
    }

    private fun handleFetchedBlockLayouts(payload: FetchedBlockLayoutsResponsePayload) {
        if (payload.isError) {
            // Return cached layouts on error
            if (!cachedLayoutsRetrieved(payload.site)) {
                emitChange(OnBlockLayoutsFetched(payload.layouts, payload.categories, payload.error))
            }
        } else {
            SiteSqlUtils.insertOrReplaceBlockLayouts(payload.site, payload.categories!!, payload.layouts!!)
            emitChange(OnBlockLayoutsFetched(payload.layouts, payload.categories, payload.error))
        }
    }

    /**
     * Emits a new [OnBlockLayoutsFetched] event with cached layouts for a given site
     *
     * @param site the site for which the cached layouts should be retrieved
     * @return true if cached layouts were retrieved successfully
     */
    private fun cachedLayoutsRetrieved(site: SiteModel): Boolean {
        val layouts = SiteSqlUtils.getBlockLayouts(site)
        val categories = SiteSqlUtils.getBlockLayoutCategories(site)
        if (!layouts.isEmpty() && !categories.isEmpty()) {
            emitChange(OnBlockLayoutsFetched(layouts, categories, null))
            return true
        }
        return false
    }

    // Automated Transfers
    private fun checkAutomatedTransferEligibility(site: SiteModel) {
        mSiteRestClient.checkAutomatedTransferEligibility(site)
    }

    private fun handleCheckedAutomatedTransferEligibility(payload: AutomatedTransferEligibilityResponsePayload) {
        emitChange(
                OnAutomatedTransferEligibilityChecked(
                        payload.site, payload.isEligible, payload.errorCodes,
                        payload.error
                )
        )
    }

    private fun initiateAutomatedTransfer(payload: InitiateAutomatedTransferPayload) {
        mSiteRestClient.initiateAutomatedTransfer(payload.site, payload.pluginSlugToInstall)
    }

    private fun handleInitiatedAutomatedTransfer(payload: InitiateAutomatedTransferResponsePayload) {
        emitChange(OnAutomatedTransferInitiated(payload.site, payload.pluginSlugToInstall, payload.error))
    }

    private fun checkAutomatedTransferStatus(site: SiteModel) {
        mSiteRestClient.checkAutomatedTransferStatus(site)
    }

    private fun handleCheckedAutomatedTransferStatus(payload: AutomatedTransferStatusResponsePayload) {
        val event: OnAutomatedTransferStatusChecked
        event = if (!payload.isError) {
            // We can't rely on the currentStep and totalSteps as it may not be equal when the transfer is complete
            val isTransferCompleted = payload.status.equals("complete", ignoreCase = true)
            OnAutomatedTransferStatusChecked(
                    payload.site, isTransferCompleted, payload.currentStep,
                    payload.totalSteps
            )
        } else {
            OnAutomatedTransferStatusChecked(payload.site, payload.error)
        }
        emitChange(event)
    }

    private fun completeQuickStart(payload: CompleteQuickStartPayload) {
        mSiteRestClient.completeQuickStart(payload.site, payload.variant)
    }

    private fun handleQuickStartCompleted(payload: QuickStartCompletedResponsePayload) {
        val event = OnQuickStartCompleted(payload.site, payload.success)
        event.error = payload.error
        emitChange(event)
    }

    private fun designatePrimaryDomain(payload: DesignatePrimaryDomainPayload) {
        mSiteRestClient.designatePrimaryDomain(payload.site, payload.domain)
    }

    private fun handleDesignatedPrimaryDomain(payload: DesignatedPrimaryDomainPayload) {
        val event = OnPrimaryDomainDesignated(payload.site, payload.success)
        event.error = payload.error
        emitChange(event)
    }
}