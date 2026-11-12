package com.hidoan.base.feature.main

import android.content.Intent
import com.hidoan.base.core.billing.Premium
import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.hidoan.base.core.ads.AdPolicy
import com.hidoan.base.core.ads.AdUnit
import com.hidoan.base.core.ads.AppOpenAds
import com.hidoan.base.core.ads.BannerPlacement
import com.hidoan.base.core.ads.InterstitialAds
import com.hidoan.base.core.ads.InterstitialPlacement
import com.hidoan.base.core.ads.NativeAds
import com.hidoan.base.core.ads.NativePlacement
import com.hidoan.base.core.config.AdKey
import com.hidoan.base.core.config.PlacementFormat
import com.hidoan.base.core.ui.base.BaseActivity
import com.hidoan.base.databinding.ActivityMainBinding
import com.hidoan.base.feature.settings.SettingsActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    /** Lần onResume đầu tiên là lúc mở màn hình, khi banner đã đang load - chỉ các lần sau mới reload. */
    private var hasResumedOnce = false

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun initConfig(savedInstanceState: Bundle?) {
        if (AdPolicy.nativeEnabled(NativePlacement.HOME_FEED)) NativeAds.preload(NativePlacement.HOME_FEED)
    }

    override fun initViews() {
        // Chỉ hâm nóng App Open ad khi việc quay lại app có thể hiện nó (on_resume format aoa).
        if (AdPolicy.format(AdKey.ON_RESUME) == PlacementFormat.AOA) AppOpenAds.load(AdUnit.ON_RESUME)
        InterstitialAds.preload(InterstitialPlacement.HOME)
        // Main tự reload slot banner - khi resume và khi đổi tab - thay vì theo bộ hẹn giờ.
        loadBanner(binding.bannerContainer, BannerPlacement.HOME, autoReload = false)
        binding.mainFeedPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = TAB_COUNT
            override fun createFragment(position: Int) = MainFeedPageFragment.newInstance(position)
        }
        TabLayoutMediator(binding.mainFeedTabs, binding.mainFeedPager) { tab, position ->
            tab.text = "Tab ${position + 1}"
        }.attach()
    }

    override fun initListeners() {
        binding.mainFeedPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var lastPosition = binding.mainFeedPager.currentItem

            override fun onPageSelected(position: Int) {
                // ViewPager2 cũng gọi hàm này cho trang ban đầu (và khi restore) - chỉ khi đổi tab thật mới reload.
                if (position == lastPosition) return
                lastPosition = position
                reloadBanner()
            }
        })
        binding.ivSetting.setOnClickListener {
            showInterstitial(InterstitialPlacement.HOME) {
                if (!isFinishing) startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasResumedOnce) reloadBanner() else hasResumedOnce = true
        if (!paymentIssueChecked) {
            // Gia hạn bị từ chối: để Google Play nhắc user sửa thanh toán, mỗi phiên một lần.
            paymentIssueChecked = true
            Premium.showPaymentIssueMessage(this)
        }
    }

    override fun releaseResources() {
        binding.mainFeedPager.adapter = null
    }

    private companion object {
        const val TAB_COUNT = 3

        /** Đã cho Google Play kiểm tra lỗi thanh toán trong phiên này chưa (sống theo process). */
        var paymentIssueChecked = false
    }
}
