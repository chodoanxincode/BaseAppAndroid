package com.hidoan.base.feature.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hidoan.base.core.ads.AdPolicy
import com.hidoan.base.core.ads.InterstitialPlacement
import com.hidoan.base.core.ads.NativeAds
import com.hidoan.base.core.ads.NativePlacement
import com.hidoan.base.core.config.AdKey
import com.hidoan.base.core.config.RemoteConfig
import com.hidoan.base.core.language.AppLanguage
import com.hidoan.base.core.navigation.AppRouter
import com.hidoan.base.core.navigation.StartDestination
import com.hidoan.base.core.ui.base.BaseActivity
import com.hidoan.base.databinding.ActivityLanguageBinding
import com.hidoan.base.feature.main.MainActivity
import com.hidoan.base.feature.onboarding.OnboardingActivity
import com.hidoan.base.feature.onboarding.OnboardingPageFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn chọn ngôn ngữ, dùng ở hai chỗ:
 * - lần đầu cài app (Splash → Language → Onboarding): native `NATIVE_LANGUAGE`, bấm Apply có inter `inter_language`, rồi
 *   sang Onboarding / Home;
 * - mở từ Cài đặt ([openFromSettings]): native `NATIVE_LANGUAGE_2`, bấm Apply thì lưu ngôn ngữ và quay lại Cài đặt,
 *   không có inter.
 * Cả hai dùng chung vị trí `native_language` (`enable` bật/tắt và chọn layout).
 */
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private lateinit var selectedLanguage: AppLanguage
    private val fromSettings get() = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)

    override fun inflateBinding(inflater: LayoutInflater): ActivityLanguageBinding =
        ActivityLanguageBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        selectedLanguage = AppLanguage.fromCode(prefs.selectedLanguage) ?: AppLanguage.default
        if (fromSettings) return
        OnboardingPageFragment.PLACEMENTS.filter(AdPolicy::nativeEnabled).forEach(NativeAds::preload)
        if (OnboardingActivity.fullNativePages().isNotEmpty()) NativeAds.preload(NativePlacement.ONBOARDING_AD)
    }

    override fun initViews() {
        binding.languageList.layoutManager = LinearLayoutManager(this)
        binding.languageList.adapter =
            LanguageAdapter(AppLanguage.entries, selectedLanguage) { selectedLanguage = it }
        loadNative(binding.nativeSmallContainer, NativePlacement.LANGUAGE_SMALL)
        loadNative(binding.nativeContainer, if (fromSettings) NativePlacement.LANGUAGE_SETTING else NativePlacement.LANGUAGE)

        val setup = RemoteConfig.languageSetup
        // next: 1 = icon dấu check trên thanh tiêu đề, 2 = nút chữ.
        val nextControl = if (setup.nextStyle == 1) binding.applyIcon else binding.applyButton
        lifecycleScope.launch {
            if (setup.loading) binding.applyLoading.isVisible = true
            delay(RemoteConfig.languageDelayMillis)
            binding.applyLoading.isVisible = false
            nextControl.isVisible = true
        }
    }

    override fun initListeners() {
        val apply = {
            prefs.selectedLanguage = selectedLanguage.code
            prefs.applyLocale(selectedLanguage.code)
            if (fromSettings) finish() else showInterstitial(InterstitialPlacement.LANGUAGE) { if (!isFinishing) goToNext() }
        }
        binding.applyButton.setOnClickListener { apply() }
        binding.applyIcon.setOnClickListener { apply() }
    }

    private fun goToNext() {
        val next = when (AppRouter.afterLanguage(prefs.setupCompleted)) {
            StartDestination.ONBOARDING -> OnboardingActivity::class.java
            StartDestination.HOME -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }

    override fun releaseResources() {
        binding.languageList.adapter = null
    }

    companion object {
        private const val EXTRA_FROM_SETTINGS = "from_settings"

        /** Mở màn chọn ngôn ngữ từ Cài đặt (native `NATIVE_LANGUAGE_2`, Apply xong quay lại). */
        fun openFromSettings(context: Context) =
            context.startActivity(Intent(context, LanguageActivity::class.java).putExtra(EXTRA_FROM_SETTINGS, true))
    }
}
