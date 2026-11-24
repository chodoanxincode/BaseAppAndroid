package com.hidoan.base.core.language

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Context theo ngôn ngữ đã chọn trong app. Dưới Android 13 `AppCompatDelegate.setApplicationLocales` chỉ đổi
 * Activity, còn `@ApplicationContext` vẫn theo ngôn ngữ máy - chữ lấy ngoài màn (tên file, thông báo...) phải qua hàm này.
 */
fun Context.localized(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this
    val config = Configuration(resources.configuration).apply { setLocale(locales[0]) }
    return createConfigurationContext(config)
}
