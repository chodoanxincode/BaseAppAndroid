package com.hidoan.base.core.ads

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.hidoan.base.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lifecycle consent/init và config dùng chung cho mọi định dạng ad. Logic load/show riêng của từng
 * định dạng nằm trong file riêng (BannerAds, NativeAds, InterstitialAds, RewardAds,
 * AppOpenAds) - object này chỉ điều phối khởi động/dọn dẹp.
 */
object Ads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val readyCallbacks = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private var initializationStarted = false
    private var sdkInitializationScheduled = false
    private var ready = false
    private var consentInformation: ConsentInformation? = null

    @Volatile
    var adsEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) clearAll()
        }

    val isReady: Boolean get() = ready
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation?.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * @param onConsentResolved chạy một lần khi bước consent (UMP) xong - hỏi xong form hoặc không cần hỏi - trước khi
     * SDK khởi tạo xong. Màn Splash chỉ cần chờ tới đây (như P19); [onReady] có thể chậm vài giây trên máy yếu.
     */
    fun initialize(
        activity: Activity,
        onConsentResolved: () -> Unit = {},
        onReady: (Boolean) -> Unit = {},
    ) {
        if (!adsEnabled || ready || initializationStarted && consentResolved) onConsentResolved()
        else consentCallbacks += onConsentResolved
        if (!adsEnabled) {
            onReady(false); return
        }
        if (ready) {
            onReady(true); return
        }
        readyCallbacks += onReady
        if (initializationStarted) return
        initializationStarted = true
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = consent
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                initializeIfAllowed(activity, consent)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    resolveConsent()
                    initializeIfAllowed(activity, consent)
                    if (!consent.canRequestAds()) finishInitialization(false)
                }
            },
            {
                resolveConsent()
                if (consent.canRequestAds()) initializeSdk(activity) else finishInitialization(false)
            },
        )
    }

    @Volatile private var consentResolved = false
    private val consentCallbacks = mutableListOf<() -> Unit>()

    @Synchronized
    private fun resolveConsent() {
        consentResolved = true
        val callbacks = consentCallbacks.toList()
        consentCallbacks.clear()
        callbacks.forEach { it() }
    }

    private fun initializeIfAllowed(activity: Activity, consent: ConsentInformation) {
        if (consent.canRequestAds()) initializeSdk(activity)
    }

    @Synchronized
    private fun initializeSdk(activity: Activity) {
        if (ready || MobileAds.isInitialized) {
            finishInitialization(true)
            return
        }
        if (sdkInitializationScheduled) return
        sdkInitializationScheduled = true
        scope.launch {
            MobileAds.initialize(
                activity.applicationContext,
                InitializationConfig.Builder(activity.getString(R.string.admob_app_id)).build(),
            ) { activity.runOnUiThread { finishInitialization(true) } }
        }
    }

    @Synchronized
    private fun finishInitialization(success: Boolean) {
        if (success && !ready) {
            ready = true
        }
        val callbacks = readyCallbacks.toList()
        readyCallbacks.clear()
        callbacks.forEach { it(success) }
    }

    fun showPrivacyOptions(
        activity: Activity,
        onReady: (Boolean) -> Unit = {},
        onDismissed: (String?) -> Unit = {},
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (consentInformation?.canRequestAds() == true) {
                readyCallbacks += onReady
                initializeSdk(activity)
            } else {
                onReady(false)
            }
            onDismissed(error?.message)
        }
    }

    fun onReturnToForeground(activity: Activity) {
        if (!adsEnabled) return
        AppOpenAds.onReturnToForeground(activity)
    }

    private fun clearAll() {
        InterstitialAds.clear()
        RewardAds.clear()
        AppOpenAds.clear()
        NativeAds.clear()
    }
}
