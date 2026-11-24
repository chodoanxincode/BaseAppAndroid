package com.hidoan.base.debug

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

/**
 * [DEBUG] Điều khiển app từ adb trên các máy chặn `adb shell input` (vd. MIUI chưa bật
 * "USB debugging (Security settings)"). Thao tác chạm được dispatch ngay trong process tới các window của app,
 * nên không cần quyền INJECT_EVENTS. Toạ độ tính bằng pixel màn hình (như trong `uiautomator dump`).
 *
 * ```
 * adb shell am broadcast -a com.hidoan.base.debug.TAP --ei x 540 --ei y 1200
 * adb shell am broadcast -a com.hidoan.base.debug.SWIPE --ei x1 900 --ei y1 1000 --ei x2 100 --ei y2 1000
 * adb shell am broadcast -a com.hidoan.base.debug.TYPE --es text "3000"   (thay nội dung ô đang focus)
 * adb shell am broadcast -a com.hidoan.base.debug.BACK
 * adb shell am broadcast -a com.hidoan.base.debug.NIGHT --es mode light|dark|system   (đổi sáng/tối riêng app, không đụng cài đặt máy)
 * adb shell am broadcast -a com.hidoan.base.debug.TAPS --es seq "136,1016,0;359,1016,400"
 *   (chuỗi chạm: "x,y,mốcMs" - mốc tính từ lúc nhận broadcast, để test nhịp Lesson chính xác tới vài ms)
 * ```
 */
class AdbDriverInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        AdbDriver.install(app)
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}

private object AdbDriver {
    private const val TAG = "AdbDriver"
    private const val PREFIX = "com.hidoan.base.debug."
    private var resumed: WeakReference<Activity>? = null
    private val main = Handler(Looper.getMainLooper())

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) { resumed = WeakReference(activity) }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Giữ máy luôn sáng trong lúc app được điều khiển qua adb.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) activity.setTurnScreenOn(true)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        val filter = IntentFilter().apply { listOf("TAP", "TAPS", "SWIPE", "TYPE", "BACK", "NIGHT").forEach { addAction(PREFIX + it) } }
        ContextCompat.registerReceiver(app, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = handle(intent)
        }, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private fun handle(intent: Intent) {
        val activity = resumed?.get() ?: run { Log.w(TAG, "no resumed activity"); return }
        if (intent.action == PREFIX + "NIGHT") {
            AppCompatDelegate.setDefaultNightMode(
                when (intent.getStringExtra("mode")) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
            return
        }
        when (intent.action?.removePrefix(PREFIX)) {
            "TAP" -> {
                val x = intent.getIntExtra("x", 0).toFloat()
                val y = intent.getIntExtra("y", 0).toFloat()
                gesture(activity, listOf(x to y, x to y), stepMs = 40)
            }
            "TAPS" -> {
                val base = SystemClock.uptimeMillis() + 50
                intent.getStringExtra("seq").orEmpty().split(";").mapNotNull { part ->
                    part.split(",").mapNotNull { it.trim().toLongOrNull() }.takeIf { it.size == 3 }
                }.forEach { (x, y, at) ->
                    main.postAtTime({ resumed?.get()?.let { tapNow(it, x.toFloat(), y.toFloat()) } }, base + at)
                }
            }
            "SWIPE" -> {
                val x1 = intent.getIntExtra("x1", 0).toFloat(); val y1 = intent.getIntExtra("y1", 0).toFloat()
                val x2 = intent.getIntExtra("x2", 0).toFloat(); val y2 = intent.getIntExtra("y2", 0).toFloat()
                val steps = 12
                gesture(activity, (0..steps).map { i -> (x1 + (x2 - x1) * i / steps) to (y1 + (y2 - y1) * i / steps) }, stepMs = 16)
            }
            "TYPE" -> focusedEditText(activity)?.setText(intent.getStringExtra("text").orEmpty())
            "BACK" -> if (activity is ComponentActivity) {
                activity.onBackPressedDispatcher.onBackPressed()
            } else {
                @Suppress("DEPRECATION")
                activity.onBackPressed()
            }
        }
        Log.d(TAG, "${intent.action} on ${activity.javaClass.simpleName}")
    }

    /** Ô nhập đang focus: trong dialog đang hiện trước (ô tên bản thu…), sau đó tới window của activity. */
    private fun focusedEditText(activity: Activity): EditText? {
        val roots = mutableListOf<View>()
        if (activity is FragmentActivity) collectDialogs(activity.supportFragmentManager, roots)
        roots += activity.window.decorView
        return roots.firstNotNullOfOrNull { it.findFocus() as? EditText }
    }

    /** Window trên cùng trước: các dialog DialogFragment đang hiện, sau đó tới window của chính activity. */
    private fun targetRoot(activity: Activity, x: Float, y: Float): View? {
        val roots = mutableListOf<View>()
        if (activity is FragmentActivity) collectDialogs(activity.supportFragmentManager, roots)
        roots += activity.window.decorView
        return roots.firstOrNull { root ->
            val loc = IntArray(2).also { root.getLocationOnScreen(it) }
            x >= loc[0] && y >= loc[1] && x < loc[0] + root.width && y < loc[1] + root.height
        }
    }

    private fun collectDialogs(fm: FragmentManager, out: MutableList<View>) {
        fm.fragments.asReversed().forEach { f ->
            collectDialogs(f.childFragmentManager, out)
            val dialog = (f as? DialogFragment)?.dialog
            if (dialog?.isShowing == true) dialog.window?.decorView?.let(out::add)
        }
    }

    /** Chạm (DOWN ngay, UP sau 40 ms) tại thời điểm gọi - dùng cho chuỗi TAPS có mốc thời gian. */
    private fun tapNow(activity: Activity, x: Float, y: Float) {
        val root = targetRoot(activity, x, y) ?: return
        val loc = IntArray(2).also { root.getLocationOnScreen(it) }
        val downTime = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 40L).forEach { (action, delay) ->
            main.postDelayed({
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x - loc[0], y - loc[1], 0)
                root.dispatchTouchEvent(event)
                event.recycle()
            }, delay)
        }
    }

    /** Gửi DOWN ở điểm đầu tiên, MOVE qua các điểm ở giữa và UP ở điểm cuối. */
    private fun gesture(activity: Activity, points: List<Pair<Float, Float>>, stepMs: Long) {
        val (sx, sy) = points.first()
        val root = targetRoot(activity, sx, sy) ?: return
        val loc = IntArray(2).also { root.getLocationOnScreen(it) }
        val downTime = SystemClock.uptimeMillis()
        points.forEachIndexed { i, (px, py) ->
            val action = when (i) {
                0 -> MotionEvent.ACTION_DOWN
                points.lastIndex -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            main.postDelayed({
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, px - loc[0], py - loc[1], 0)
                root.dispatchTouchEvent(event)
                event.recycle()
            }, i * stepMs)
        }
    }
}
