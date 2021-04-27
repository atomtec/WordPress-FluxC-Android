package org.wordpress.android.fluxc.ditests

import dagger.Component
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            ReleaseOkHttpClientModule::class,
            ReleaseNetworkModule::class
        ]
)
interface OkHttpReleaseTestComponent {
    fun inject(testRelease: OkHttpReleaseInjectionTest)
}