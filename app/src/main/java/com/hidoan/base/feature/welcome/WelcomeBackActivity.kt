package com.hidoan.base.feature.welcome

import android.app.Activity
import android.content.Intent
import android.os.CountDownTimer
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.hidoan.base.core.ads.AdPolicy
import com.hidoan.base.core.ads.Ads
import com.hidoan.base.core.ads.FullScreenGate
import com.hidoan.base.core.ads.NativeAds
import com.hidoan.base.core.ads.NativePlacement
import com.hidoan.base.core.config.AdKey
import com.hidoan.base.core.ui.base.BaseActivity
import com.hidoan.base.databinding.ActivityWelcomeBackBinding
import kotlin.math.ceil

/**
 * Màn "Welcome back" khi quay lại app nếu `on_resume` có format `welcome_back` (thay cho App Open ad).
 * Hiện 1 native ad + đếm ngược `wait_ms` của `native_welcome_back` rồi mới cho đóng. Mở qua [launch].
 */
class WelcomeBackActivity : BaseActivity<ActivityWelcomeBackBinding>() {
    private var countdown: CountDownTimer? = null
    private var gateHeld = false

    override fun inflateBinding(inflater: LayoutInflater): ActivityWelcomeBackBinding =
        ActivityWelcomeBackBinding.inflate(inflater)

    override fun initViews() {
        if (!FullScreenGate.acquire(this)) { finish(); return }
        gateHeld = true
        loadNative(binding.nativeContainer, NativePlacement.WELCOME_BACK)
        startCountdown()
    }

    override fun initListeners() {
        binding.ivClose.setOnClickListener { finish() }
    }

    private fun startCountdown() {
        val total = AdPolicy.config(AdKey.NATIVE_WELCOME_BACK).waitMs
        if (total <= 0L) {
            binding.ivClose.isVisible = true
            return
        }
        binding.tvCountdown.isVisible = true
        binding.ivClose.isVisible = false
        countdown = object : CountDownTimer(total, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountdown.text = ceil(millisUntilFinished / 1000.0).toInt().toString()
            }
            override fun onFinish() {
                binding.tvCountdown.isVisible = false
                binding.ivClose.isVisible = true
            }
        }.start()
    }

    override fun releaseResources() {
        countdown?.cancel()
        countdown = null
        if (gateHeld) {
            FullScreenGate.release(this)
            gateHeld = false
        }
        showing = false
    }

    companion object {
        @Volatile private var showing = false

        /**
         * Mở màn welcome back trên [activity], trừ khi: đang hiện 1 cái / có quảng cáo full-screen khác đang bận /
         * quảng cáo tắt / không có native nào cho [NativePlacement.WELCOME_BACK].
         */
        fun launch(activity: Activity) {
            if (showing || FullScreenGate.isBusy() || !Ads.adsEnabled) return
            if (NativeAds.isUnavailable(NativePlacement.WELCOME_BACK)) return
            showing = true
            activity.startActivity(Intent(activity, WelcomeBackActivity::class.java))
        }
    }
}
