package com.hidoan.base.feature.settings

import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import com.hidoan.base.BuildConfig
import com.hidoan.base.R
import com.hidoan.base.core.ads.Ads
import com.hidoan.base.core.ads.BannerPlacement
import com.hidoan.base.core.language.AppLanguage
import com.hidoan.base.core.ui.base.BaseActivity
import com.hidoan.base.databinding.ActivitySettingsBinding
import com.hidoan.base.feature.debug.DebugConfigActivity
import com.hidoan.base.feature.language.LanguageActivity
import com.hidoan.base.feature.premium.PremiumActivity

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivitySettingsBinding =
        ActivitySettingsBinding.inflate(inflater)

    override fun initViews() {
        binding.privacyButton.isVisible = Ads.isPrivacyOptionsRequired
        binding.debugConfigButton.isVisible = BuildConfig.DEBUG
        loadBanner(binding.bannerContainer, BannerPlacement.SETTING)
    }

    override fun onResume() {
        super.onResume()
        binding.languageButton.text = (AppLanguage.fromCode(prefs.selectedLanguage) ?: AppLanguage.default).displayName
    }

    override fun initListeners() {
        binding.languageButton.setOnClickListener { LanguageActivity.openFromSettings(this) }
        binding.privacyButton.setOnClickListener { Ads.showPrivacyOptions(this) }
        binding.resetOnboardingButton.setOnClickListener {
            prefs.resetOnboarding()
            Toast.makeText(this, R.string.onboarding_reset, Toast.LENGTH_SHORT).show()
        }
        binding.premiumButton.setOnClickListener { startActivity(Intent(this, PremiumActivity::class.java)) }
        binding.debugConfigButton.setOnClickListener { startActivity(Intent(this, DebugConfigActivity::class.java)) }
    }
}
