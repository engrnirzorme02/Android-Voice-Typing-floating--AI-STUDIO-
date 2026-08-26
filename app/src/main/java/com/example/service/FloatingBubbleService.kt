package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.SupportedLanguages
import com.example.data.VoiceHistoryEntity
import com.example.speech.SpeechEngine
import com.example.speech.SpeechState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingBubbleService : Service() {

    private val TAG = "FloatingBubbleService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var windowManager: WindowManager? = null
    private var floatingRootView: FrameLayout? = null
    private var bubbleContainer: FrameLayout? = null
    private var bubbleCircle: FrameLayout? = null
    private var micIcon: ImageView? = null
    private var pulseRing: View? = null
    private var infoPill: LinearLayout? = null
    private var infoPillText: TextView? = null
    private var langBadge: TextView? = null
    private var copiedBadge: LinearLayout? = null
    private var copiedBadgeText: TextView? = null
    private var quickMenuCard: LinearLayout? = null
    private var geminiStatusBadge: TextView? = null
    private var isQuickMenuOpen = false
    private var isLongPressTriggered = false
    private var longPressRunnable: Runnable? = null

    private lateinit var windowParams: WindowManager.LayoutParams
    private lateinit var speechEngine: SpeechEngine
    private lateinit var appPreferences: AppPreferences
    private lateinit var clipboardManager: ClipboardManager

    private var screenWidth = 0
    private var screenHeight = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchStartTime = 0L
    private var isDragging = false
    private var isRecording = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideCopiedBadgeRunnable: Runnable? = null
    private var inactivityRunnable: Runnable? = null
    private val INACTIVITY_TIMEOUT = 30_000L
    private var isDimmed = false

    companion object {
        const val CHANNEL_ID = "voice_bubble_service_channel"
        const val NOTIFICATION_ID = 9911
        const val ACTION_START = "com.example.voicebubble.ACTION_START"
        const val ACTION_STOP = "com.example.voicebubble.ACTION_STOP"
        const val ACTION_TOGGLE_RECORD = "com.example.voicebubble.ACTION_TOGGLE_RECORD"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingBubbleService onCreate")
        speechEngine = SpeechEngine(this)
        appPreferences = AppPreferences.getInstance(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        updateScreenDimensions()
        createNotificationChannel()

        val notification = createNotification("Voice Bubble সক্রিয় আছে (ট্যাপ করে বলুন)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true

        if (Settings.canDrawOverlays(this)) {
            initFloatingOverlay()
            observeSpeechState()
        } else {
            Log.w(TAG, "Overlay permission not granted!")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_RECORD -> {
                toggleRecording()
            }
        }
        return START_STICKY
    }

    private fun updateScreenDimensions() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Bubble Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ভয়েস বাবল ফোরগ্রাউন্ড সার্ভিস সক্রিয় রাখার জন্য নোটিফিকেশন"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val langCode = appPreferences.selectedLanguage.value
        val lang = SupportedLanguages.getLanguageByCode(langCode)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Bubble (${lang.flag} ${lang.displayName})")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingOverlay() {
        val density = resources.displayMetrics.density
        val sizeDp = appPreferences.bubbleSizeDp.value
        val sizePx = (sizeDp * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            
            val savedX = appPreferences.bubbleX.value
            val savedY = appPreferences.bubbleY.value
            
            if (savedX != -1 && savedY != -1) {
                x = savedX
                y = savedY
            } else {
                x = screenWidth - sizePx - (16 * density).toInt()
                y = (screenHeight * 0.35f).toInt()
            }
        }

        floatingRootView = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }

        pulseRing = View(this).apply {
            val ringDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4438BDF8"))
                setStroke((3 * density).toInt(), Color.parseColor("#8038BDF8"))
            }
            background = ringDrawable
            visibility = View.GONE
        }

        bubbleCircle = FrameLayout(this).apply {
            val bgDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#4F46E5"), Color.parseColor("#06B6D4"))
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke((2 * density).toInt(), Color.parseColor("#FFFFFF"))
            }
            background = bgDrawable
            elevation = 16 * density
            alpha = appPreferences.bubbleOpacity.value
        }

        micIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(Color.WHITE)
            val pad = (sizePx * 0.22f).toInt()
            setPadding(pad, pad, pad, pad)
        }

        langBadge = TextView(this).apply {
            val lang = SupportedLanguages.getLanguageByCode(appPreferences.selectedLanguage.value)
            text = lang.flag
            textSize = 11f
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E1B4B"))
                setStroke((1 * density).toInt(), Color.parseColor("#6366F1"))
            }
            background = badgeBg
        }

        val langBadgeParams = FrameLayout.LayoutParams(
            (22 * density).toInt(),
            (22 * density).toInt()
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, (2 * density).toInt(), (2 * density).toInt())
        }

        bubbleCircle?.addView(micIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        bubbleCircle?.addView(langBadge, langBadgeParams)

        bubbleContainer = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        val ringParams = FrameLayout.LayoutParams(
            (sizePx * 1.5f).toInt(),
            (sizePx * 1.5f).toInt()
        ).apply {
            gravity = Gravity.CENTER
        }
        val circleParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.CENTER
        }

        bubbleContainer?.addView(pulseRing, ringParams)
        bubbleContainer?.addView(bubbleCircle, circleParams)

        infoPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (12 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            val pillBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#E60F172A")) // semi-transparent dark slate
                setStroke((1 * density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = pillBg
            elevation = 18 * density
            visibility = View.GONE
        }

        infoPillText = TextView(this).apply {
            text = "বলুন... (Listening)"
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 3
        }
        infoPill?.addView(infoPillText)

        copiedBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (12 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#EE065F46")) // green dark
                setStroke((1 * density).toInt(), Color.parseColor("#34D399"))
            }
            background = badgeBg
            elevation = 20 * density
            visibility = View.GONE
        }

        val checkIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.checkbox_on_background)
            setColorFilter(Color.parseColor("#34D399"))
            val iconPad = (2 * density).toInt()
            setPadding(iconPad, iconPad, iconPad, iconPad)
        }
        copiedBadgeText = TextView(this).apply {
            text = "ক্লিপবোর্ডে কপি হয়েছে! ✓"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding((4 * density).toInt(), 0, 0, 0)
        }
        copiedBadge?.addView(checkIcon, LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt()))
        copiedBadge?.addView(copiedBadgeText)

        val containerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val infoPillParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, (6 * density).toInt(), 0, 0)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val copiedBadgeParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, (6 * density).toInt(), 0, 0)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Quick Menu Card (Opened on Long-Press)
        quickMenuCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * density
                setColor(Color.parseColor("#F50F172A")) // Solid dark slate
                setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = cardBg
            elevation = 24 * density
            visibility = View.GONE

            // Header Layout
            val headerLayout = LinearLayout(this@FloatingBubbleService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, (6 * density).toInt())
            }
            val headerTitle = TextView(this@FloatingBubbleService).apply {
                text = "⚡ কুইক মেনু (Quick Menu)"
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val closeBtn = TextView(this@FloatingBubbleService).apply {
                text = "✕"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                gravity = Gravity.CENTER
                val p = (4 * density).toInt()
                setPadding(p * 2, p, p * 2, p)
                setOnClickListener {
                    hideQuickMenu()
                }
            }
            headerLayout.addView(headerTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            headerLayout.addView(closeBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(headerLayout)

            // Divider
            addView(createDivider(density))

            // Option 1: Gemini AI Paraphrasing Toggle
            val geminiRow = LinearLayout(this@FloatingBubbleService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val pV = (8 * density).toInt()
                val pH = (4 * density).toInt()
                setPadding(pH, pV, pH, pV)
                isClickable = true
                isFocusable = true

                val rowTextLayout = LinearLayout(this@FloatingBubbleService).apply {
                    orientation = LinearLayout.VERTICAL
                    val textTitle = TextView(this@FloatingBubbleService).apply {
                        text = "✨ Gemini AI প্যারাফ্রেজ"
                        setTextColor(Color.parseColor("#F8FAFC"))
                        textSize = 12.5f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    val textSub = TextView(this@FloatingBubbleService).apply {
                        text = "ব্যাকরণ ও ফিলার অটো-ক্লিন"
                        setTextColor(Color.parseColor("#94A3B8"))
                        textSize = 10f
                    }
                    addView(textTitle)
                    addView(textSub)
                }

                geminiStatusBadge = TextView(this@FloatingBubbleService).apply {
                    textSize = 10.5f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    val bPadH = (8 * density).toInt()
                    val bPadV = (3 * density).toInt()
                    setPadding(bPadH, bPadV, bPadH, bPadV)
                    updateGeminiBadge(this, appPreferences.aiPolishEnabled.value, density)
                }

                addView(rowTextLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(geminiStatusBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

                setOnClickListener {
                    val newState = !appPreferences.aiPolishEnabled.value
                    appPreferences.setAiPolishEnabled(newState)
                    geminiStatusBadge?.let { updateGeminiBadge(it, newState, density) }
                    triggerHapticFeedback()
                    showCopiedBadge(if (newState) "Gemini প্যারাফ্রেজ সক্রিয় ✓" else "Gemini প্যারাফ্রেজ বন্ধ ✕")
                }
            }
            addView(geminiRow)

            // Divider
            addView(createDivider(density))

            // Option 2: History Log
            val historyRow = LinearLayout(this@FloatingBubbleService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val pV = (8 * density).toInt()
                val pH = (4 * density).toInt()
                setPadding(pH, pV, pH, pV)
                isClickable = true
                isFocusable = true

                val rowTextLayout = LinearLayout(this@FloatingBubbleService).apply {
                    orientation = LinearLayout.VERTICAL
                    val textTitle = TextView(this@FloatingBubbleService).apply {
                        text = "📜 হিস্ট্রি লগ দেখুন"
                        setTextColor(Color.parseColor("#F8FAFC"))
                        textSize = 12.5f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    val textSub = TextView(this@FloatingBubbleService).apply {
                        text = "আগের সব ট্রান্সক্রিপশন তালিকা"
                        setTextColor(Color.parseColor("#94A3B8"))
                        textSize = 10f
                    }
                    addView(textTitle)
                    addView(textSub)
                }

                val arrowIcon = TextView(this@FloatingBubbleService).apply {
                    text = "➔"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 13f
                    setPadding((6 * density).toInt(), 0, 0, 0)
                }

                addView(rowTextLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(arrowIcon, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

                setOnClickListener {
                    hideQuickMenu()
                    triggerHapticFeedback()
                    val historyIntent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.TARGET_HISTORY)
                    }
                    startActivity(historyIntent)
                }
            }
            addView(historyRow)
        }

        val quickMenuParams = LinearLayout.LayoutParams(
            (230 * density).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, (8 * density).toInt(), 0, 0)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        container.addView(bubbleContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        container.addView(quickMenuCard, quickMenuParams)
        container.addView(infoPill, infoPillParams)
        container.addView(copiedBadge, copiedBadgeParams)

        floatingRootView?.addView(container, containerParams)

        bubbleCircle?.setOnTouchListener { _, event ->
            handleTouch(event)
        }

        floatingRootView?.setOnClickListener {
            if (isQuickMenuOpen) {
                hideQuickMenu()
            }
        }

        try {
            windowManager?.addView(floatingRootView, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view to WindowManager", e)
        }
    }

    private fun resetInactivityTimer() {
        inactivityRunnable?.let { mainHandler.removeCallbacks(it) }
        
        if (isDimmed) {
            isDimmed = false
            val alphaAnim = AlphaAnimation(0.3f, appPreferences.bubbleOpacity.value).apply {
                duration = 300
                fillAfter = true
            }
            bubbleCircle?.startAnimation(alphaAnim)
            bubbleCircle?.alpha = appPreferences.bubbleOpacity.value
        }
        
        if (appPreferences.autoDim.value && !isRecording) {
            inactivityRunnable = Runnable {
                if (!isRecording) {
                    isDimmed = true
                    val alphaAnim = AlphaAnimation(appPreferences.bubbleOpacity.value, 0.3f).apply {
                        duration = 500
                        fillAfter = true
                    }
                    bubbleCircle?.startAnimation(alphaAnim)
                    bubbleCircle?.alpha = 0.3f
                }
            }
            mainHandler.postDelayed(inactivityRunnable!!, INACTIVITY_TIMEOUT)
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic trigger error", e)
        }
    }

    private fun updateGeminiBadge(badge: TextView, isEnabled: Boolean, density: Float) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10 * density
            if (isEnabled) {
                setColor(Color.parseColor("#065F46"))
                setStroke((1 * density).toInt(), Color.parseColor("#34D399"))
            } else {
                setColor(Color.parseColor("#334155"))
                setStroke((1 * density).toInt(), Color.parseColor("#64748B"))
            }
        }
        badge.background = bg
        badge.text = if (isEnabled) "চালু (ON)" else "বন্ধ (OFF)"
        badge.setTextColor(if (isEnabled) Color.parseColor("#34D399") else Color.parseColor("#94A3B8"))
    }

    private fun createDivider(density: Float): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                setMargins(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }
            setBackgroundColor(Color.parseColor("#334155"))
        }
    }

    private fun showQuickMenu() {
        if (isRecording) return
        isQuickMenuOpen = true
        val density = resources.displayMetrics.density
        geminiStatusBadge?.let {
            updateGeminiBadge(it, appPreferences.aiPolishEnabled.value, density)
        }
        
        updateScreenDimensions()
        val menuWidthPx = (240 * density).toInt()
        if (windowParams.x + menuWidthPx > screenWidth) {
            windowParams.x = maxOf((8 * density).toInt(), screenWidth - menuWidthPx - (8 * density).toInt())
            try {
                windowManager?.updateViewLayout(floatingRootView, windowParams)
            } catch (e: Exception) {
                Log.e(TAG, "Update layout on quick menu show", e)
            }
        }

        quickMenuCard?.visibility = View.VISIBLE
        val anim = AlphaAnimation(0f, 1f).apply {
            duration = 200
            fillAfter = true
        }
        quickMenuCard?.startAnimation(anim)
        infoPill?.visibility = View.GONE
        copiedBadge?.visibility = View.GONE
        resetInactivityTimer()
    }

    private fun hideQuickMenu() {
        if (!isQuickMenuOpen && quickMenuCard?.visibility != View.VISIBLE) return
        isQuickMenuOpen = false
        val anim = AlphaAnimation(1f, 0f).apply {
            duration = 150
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    quickMenuCard?.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }
        quickMenuCard?.startAnimation(anim)
        resetInactivityTimer()
    }

    private fun toggleQuickMenu() {
        if (isQuickMenuOpen) {
            hideQuickMenu()
        } else {
            showQuickMenu()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        resetInactivityTimer()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = windowParams.x
                initialY = windowParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchStartTime = System.currentTimeMillis()
                isDragging = false
                isLongPressTriggered = false

                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                longPressRunnable = Runnable {
                    if (!isDragging && !isRecording) {
                        isLongPressTriggered = true
                        triggerHapticFeedback()
                        toggleQuickMenu()
                    }
                }
                mainHandler.postDelayed(longPressRunnable!!, 450L)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    if (!isDragging) {
                        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                        isDragging = true
                        if (isQuickMenuOpen) {
                            hideQuickMenu()
                        }
                    }
                }

                if (isDragging) {
                    windowParams.x = initialX + dx
                    windowParams.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(floatingRootView, windowParams)
                    } catch (e: Exception) {
                        Log.e(TAG, "Update layout error", e)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                val clickDuration = System.currentTimeMillis() - touchStartTime

                if (isLongPressTriggered) {
                    // Handled by long press gesture
                } else if (!isDragging && clickDuration < 350) {
                    if (isQuickMenuOpen) {
                        hideQuickMenu()
                    } else {
                        toggleRecording()
                    }
                } else if (isDragging) {
                    if (appPreferences.dockToEdge.value) {
                        snapToNearestEdge()
                    }
                    appPreferences.setBubblePosition(windowParams.x, windowParams.y)
                }
                isDragging = false
                isLongPressTriggered = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                isDragging = false
                isLongPressTriggered = false
                return false
            }
        }
        return false
    }

    private fun snapToNearestEdge() {
        updateScreenDimensions()
        val density = resources.displayMetrics.density
        val sizePx = (appPreferences.bubbleSizeDp.value * density).toInt()
        val currentX = windowParams.x

        val targetX = if (currentX + sizePx / 2 < screenWidth / 2) {
            (8 * density).toInt() // snap to left
        } else {
            screenWidth - sizePx - (8 * density).toInt() // snap to right
        }

        windowParams.x = targetX
        try {
            windowManager?.updateViewLayout(floatingRootView, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error snapping to edge", e)
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecordingAndRecognize()
        } else {
            startRecording()
        }
        resetInactivityTimer()
    }

    private fun startRecording() {
        isRecording = true
        resetInactivityTimer()
        showPulseAnimation(true)
        showInfoPill("বলুন... (Listening)", true)
        updateNotification("ভয়েস শুনছি... কথা বলুন")

        val langCode = appPreferences.selectedLanguage.value
        val preferOffline = appPreferences.preferOffline.value
        val playSound = appPreferences.soundFeedback.value
        val haptic = appPreferences.hapticFeedback.value

        speechEngine.startListening(
            languageCode = langCode,
            preferOffline = preferOffline,
            playTone = playSound,
            enableHaptic = haptic,
            onPartialResult = { partial ->
                mainHandler.post {
                    showInfoPill(partial, true)
                }
            },
            onFinalResult = { resultText, lang, durationMs ->
                mainHandler.post {
                    handleTranscriptionSuccess(resultText, lang, durationMs)
                }
            },
            onErrorOccurred = { errorMsg, isNetworkIssue ->
                mainHandler.post {
                    handleTranscriptionError(errorMsg, isNetworkIssue)
                }
            }
        )
    }

    private fun stopRecordingAndRecognize() {
        isRecording = false
        resetInactivityTimer()
        showPulseAnimation(false)
        showInfoPill("প্রসেস হচ্ছে...", true)
        speechEngine.stopListening()
    }

    private fun handleTranscriptionSuccess(text: String, language: String, durationMs: Long) {
        isRecording = false
        showPulseAnimation(false)

        serviceScope.launch {
            val finalText = if (appPreferences.aiPolishEnabled.value && text.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    showInfoPill("AI সাজাচ্ছে...", true)
                }
                val polished = GeminiApiClient.polishText(text).trim()
                if (polished.isNotBlank()) polished else text
            } else {
                text
            }

            withContext(Dispatchers.Main) {
                showInfoPill("", false)
                if (appPreferences.autoCopy.value && finalText.isNotBlank()) {
                    val clip = ClipData.newPlainText("Voice Transcription", finalText)
                    clipboardManager.setPrimaryClip(clip)
                    showCopiedBadge("কপি হয়েছে: \"${finalText.take(30)}${if (finalText.length > 30) "..." else ""}\"")
                }
                updateNotification("ভয়েস টাইপিং সম্পন্ন: \"${finalText.take(20)}\"")
            }

            try {
                val entity = VoiceHistoryEntity(
                    text = finalText,
                    language = language,
                    durationMs = durationMs
                )
                AppDatabase.getDatabase(this@FloatingBubbleService).voiceHistoryDao().insert(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving history to Room DB", e)
            }
        }
    }

    private fun handleTranscriptionError(errorMsg: String, isNetworkIssue: Boolean) {
        isRecording = false
        showPulseAnimation(false)
        if (isNetworkIssue) {
            showInfoPill("ওয়াইফাই সমস্যা! অফলাইন মোড ট্রাই করুন।", true)
        } else {
            showInfoPill(errorMsg, true)
        }

        mainHandler.postDelayed({
            showInfoPill("", false)
        }, 3000)

        updateNotification("Voice Bubble সক্রিয় আছে (ট্যাপ করে বলুন)")
    }

    private fun showPulseAnimation(show: Boolean) {
        if (show) {
            pulseRing?.visibility = View.VISIBLE
            val scaleAnim = ScaleAnimation(
                1f, 1.4f, 1f, 1.4f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 800
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
            val alphaAnim = AlphaAnimation(0.8f, 0.2f).apply {
                duration = 800
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
            val animSet = AnimationSet(true).apply {
                addAnimation(scaleAnim)
                addAnimation(alphaAnim)
            }
            pulseRing?.startAnimation(animSet)
        } else {
            pulseRing?.clearAnimation()
            pulseRing?.visibility = View.GONE
        }
    }

    private fun showInfoPill(text: String, show: Boolean) {
        if (show) {
            infoPillText?.text = text
            infoPill?.visibility = View.VISIBLE
        } else {
            infoPill?.visibility = View.GONE
        }
    }

    private fun showCopiedBadge(message: String) {
        hideCopiedBadgeRunnable?.let { mainHandler.removeCallbacks(it) }
        copiedBadgeText?.text = message
        copiedBadge?.visibility = View.VISIBLE

        val anim = AlphaAnimation(0f, 1f).apply { duration = 200 }
        copiedBadge?.startAnimation(anim)

        hideCopiedBadgeRunnable = Runnable {
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 300
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        copiedBadge?.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
            copiedBadge?.startAnimation(fadeOut)
        }
        mainHandler.postDelayed(hideCopiedBadgeRunnable!!, 2500)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun observeSpeechState() {
        serviceScope.launch {
            appPreferences.selectedLanguage.collect { code ->
                val lang = SupportedLanguages.getLanguageByCode(code)
                langBadge?.text = lang.flag
                updateNotification("ভাষা: ${lang.displayName}")
            }
        }
        serviceScope.launch {
            appPreferences.bubbleOpacity.collect { opacity ->
                if (!isDimmed) {
                    bubbleCircle?.alpha = opacity
                }
            }
        }
        serviceScope.launch {
            appPreferences.autoDim.collect {
                resetInactivityTimer()
            }
        }
        serviceScope.launch {
            appPreferences.aiPolishEnabled.collect { isEnabled ->
                geminiStatusBadge?.let {
                    updateGeminiBadge(it, isEnabled, resources.displayMetrics.density)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingBubbleService onDestroy")
        _isServiceRunning.value = false
        speechEngine.cancelListening()
        serviceScope.cancel()

        hideCopiedBadgeRunnable?.let { mainHandler.removeCallbacks(it) }
        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
        inactivityRunnable?.let { mainHandler.removeCallbacks(it) }

        if (floatingRootView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingRootView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view", e)
            }
        }
    }
}
