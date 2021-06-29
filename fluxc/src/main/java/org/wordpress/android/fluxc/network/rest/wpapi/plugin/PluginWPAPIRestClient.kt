package org.wordpress.android.fluxc.network.rest.wpapi.plugin

import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.PluginCoroutineStore.WPApiPluginsPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PluginWPAPIRestClient @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetchPlugins(
        site: SiteModel,
        nonce: Nonce? = null,
        enableCaching: Boolean = true
    ): WPApiPluginsPayload<List<SitePluginModel>> {
        val url = buildUrl(site)
        val type = object : TypeToken<List<PluginResponseModel>>() {}.type
        val response =
                wpApiGsonRequestBuilder.syncGetRequest<List<PluginResponseModel>>(
                        this,
                        url,
                        emptyMap(),
                        emptyMap(),
                        type,
                        enableCaching,
                        nonce = nonce?.value
                )
        return when (response) {
            is Success -> {
                val plugins = response.data?.map {
                    it.toDomainModel(site.id)
                }
                WPApiPluginsPayload(site, plugins)
            }
            is Error -> {
                WPApiPluginsPayload(response.error)
            }
        }
    }

    suspend fun installPlugin(
        site: SiteModel,
        nonce: Nonce? = null,
        installedPluginSlug: String
    ): WPApiPluginsPayload<SitePluginModel> {
        val url = buildUrl(site)
        val response =
                wpApiGsonRequestBuilder.syncPostRequest(
                        this,
                        url,
                        mapOf("slug" to installedPluginSlug),
                        PluginResponseModel::class.java,
                        nonce = nonce?.value
                )
        return handleResponse(response, site)
    }

    suspend fun updatePlugin(
        site: SiteModel,
        nonce: Nonce? = null,
        updatedPlugin: String,
        active: Boolean
    ): WPApiPluginsPayload<SitePluginModel> {
        val url = buildUrl(site, updatedPlugin)
        val response =
                wpApiGsonRequestBuilder.syncPutRequest(
                        this,
                        url,
                        mapOf("status" to if (active) "active" else "inactive"),
                        PluginResponseModel::class.java,
                        nonce = nonce?.value
                )
        return handleResponse(response, site)
    }

    suspend fun deletePlugin(
        site: SiteModel,
        nonce: Nonce? = null,
        deletedPlugin: String
    ): WPApiPluginsPayload<SitePluginModel> {
        val url = buildUrl(site, deletedPlugin)
        val response =
                wpApiGsonRequestBuilder.syncDeleteRequest(
                        this,
                        url,
                        mapOf(),
                        PluginResponseModel::class.java,
                        nonce = nonce?.value
                )
        return handleResponse(response, site)
    }

    private fun handleResponse(
        response: WPAPIResponse<PluginResponseModel>,
        site: SiteModel
    ) = when (response) {
        is Success -> {
            val plugin = response.data?.toDomainModel(site.id)
            WPApiPluginsPayload(site, plugin)
        }
        is Error -> {
            WPApiPluginsPayload(response.error)
        }
    }

    private fun PluginResponseModel.toDomainModel(siteId: Int): SitePluginModel {
        val plugin = SitePluginModel()
        plugin.authorUrl = this.authorUri
        plugin.authorName = this.author
        plugin.description = this.description?.raw
        plugin.displayName = this.name
        plugin.name = this.plugin
        plugin.setIsActive(this.status == "active")
        plugin.localSiteId = siteId
        plugin.pluginUrl = this.pluginUri
        plugin.version = this.version
        plugin.slug = this.textDomain
        plugin.plugin = this.plugin
        return plugin
    }

    /**
     * POST /wp/v2/plugins { slug: "akismet" } installs the plugin with the slug aksimet from the WordPress.org plugin directory. The endpoint does not support uploading a plugin zip.
    PUT /wp/v2/plugins/akismet/akismet { status: "active" } activates the selected plugin. The status can be set to network-active to network activate the plugin on Multisite. To deactivate the plugin set the status to inactive. There is not a separate network-inactive status, inactive will perform a network deactivation if the plugin was network activated.
    DELETE /wp/v2/plugins/akismet/akismet uninstalls the selected plugin. The plugin must be inactive before deleting it.
     */
    private fun buildUrl(site: SiteModel, path: String? = null): String {
        return buildString {
            append(site.url)
            append(WP_API_URL)
            if (path != null) {
                append("/")
                append(path)
            }
        }
    }

    companion object {
        private const val WP_API_URL = "/wp-json/wp/v2/plugins"
    }
}
