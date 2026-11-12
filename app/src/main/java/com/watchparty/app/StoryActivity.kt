package com.watchparty.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.socket.client.Socket
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject

class DustView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = Random(5)
    private val n = 45
    private val x = FloatArray(n) { rnd.nextFloat() }
    private val y0 = FloatArray(n) { rnd.nextFloat() }
    private val sp = FloatArray(n) { 0.015f + rnd.nextFloat() * 0.03f }
    private val sz = FloatArray(n) { 1.5f + rnd.nextFloat() * 3f }
    private val ph = FloatArray(n) { rnd.nextFloat() * 6.28f }
    private val t0 = System.nanoTime()

    override fun onDraw(canvas: Canvas) {
        val t = (System.nanoTime() - t0) / 1e9f
        val w = width.toFloat()
        val h = height.toFloat()
        for (i in 0 until n) {
            val yy = ((y0[i] - t * sp[i]) % 1f + 1f) % 1f
            val xx = x[i] + 0.02f * sin(t * 0.8f + ph[i])
            val a = (60 + 140 * (0.5f + 0.5f * sin(t * 2f + ph[i]))).toInt()
            p.color = (a shl 24) or 0xFFD27A
            canvas.drawCircle(xx * w, yy * h, sz[i], p)
        }
        postInvalidateOnAnimation()
    }
}

class StoryActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
    private lateinit var imgA: ImageView
    private lateinit var imgB: ImageView
    private lateinit var ui: FrameLayout
    private lateinit var status: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var imm: InputMethodManager
    private val h = Handler(Looper.getMainLooper())
    private val bmps = arrayOfNulls<Bitmap>(5)
    private var front: ImageView? = null
    private var kb: ValueAnimator? = null
    private val pulses = ArrayList<ObjectAnimator>()
    private var step = -1
    private var role = "host"
    private var nameV = ""
    private var roomV = ""
    private var maxV = 5
    private var joining = false
    private var busy = false
    private val gold = 0xFFFFD27A.toInt()

    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    private fun bmp(i: Int): Bitmap? {
        bmps[i]?.let { return it }
        val id = resources.getIdentifier("s" + (i + 1), "drawable", packageName)
        if (id == 0) return null
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, id, o)
        var s = 1
        while (o.outWidth / (s * 2) >= 1100) s *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = s
        val b = BitmapFactory.decodeResource(resources, id, o2)
        bmps[i] = b
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goFullscreen()
        prefs = getSharedPreferences("fp", MODE_PRIVATE)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val sh = resources.displayMetrics.heightPixels

        root = FrameLayout(this)
        root.setBackgroundColor(0xFF000000.toInt())
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        imgA = ImageView(this)
        imgA.scaleType = ImageView.ScaleType.CENTER_CROP
        imgA.alpha = 0f
        imgB = ImageView(this)
        imgB.scaleType = ImageView.ScaleType.CENTER_CROP
        imgB.alpha = 0f
        root.addView(imgA, FrameLayout.LayoutParams(mp, mp))
        root.addView(imgB, FrameLayout.LayoutParams(mp, mp))
        root.addView(DustView(this), FrameLayout.LayoutParams(mp, mp))
        ui = FrameLayout(this)
        root.addView(ui, FrameLayout.LayoutParams(mp, mp))

        status = TextView(this)
        status.setTextColor(gold)
        status.textSize = 17f
        status.gravity = Gravity.CENTER
        status.setShadowLayer(12f, 0f, 0f, 0xFF000000.toInt())
        val slp = FrameLayout.LayoutParams(mp, ViewGroup.LayoutParams.WRAP_CONTENT)
        slp.topMargin = (sh * 0.06f).toInt()
        root.addView(status, slp)
        setContentView(root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (busy) return
                    if (joining) {
                        joining = false
                        Net.socket?.disconnect()
                        show(3)
                    } else if (step <= 0) {
                        finish()
                    } else {
                        show(step - 1)
                    }
                }
            }
        )

        if (intent.getBooleanExtra("skip", false)) {
            role = prefs.getString("role", "host") ?: "host"
            nameV = prefs.getString("name", "") ?: ""
            if (nameV.isEmpty()) nameV = "ناشناس"
            show(3)
        } else {
            show(0)
        }
    }

    private fun show(n: Int) {
        step = n
        status.text = ""
        imm.hideSoftInputFromWindow(root.windowToken, 0)
        for (a in pulses) a.cancel()
        pulses.clear()
        kb?.cancel()
        val b = bmp(n)
        val next = if (front === imgA) imgB else imgA
        val prev = front
        next.setImageBitmap(b)
        next.scaleX = 1f
        next.scaleY = 1f
        next.alpha = 0f
        next.animate().alpha(1f).setDuration(600).start()
        prev?.animate()?.alpha(0f)?.setDuration(600)?.start()
        front = next
        ui.removeAllViews()
        ui.animate().cancel()
        ui.alpha = 0f
        ui.scaleX = 1f
        ui.scaleY = 1f
        kb = ValueAnimator.ofFloat(1f, 1.06f).apply {
            duration = 12000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val s = it.animatedValue as Float
                next.scaleX = s
                next.scaleY = s
                ui.scaleX = s
                ui.scaleY = s
            }
            start()
        }
        h.postDelayed({
            if (step == n) {
                build(n, b)
                ui.animate().alpha(1f).setDuration(400).start()
            }
        }, 450)
    }

    private fun region(
        b: Bitmap?, x0: Float, y0: Float, x1: Float, y1: Float
    ): FloatArray {
        val sw = root.width.toFloat()
        val sh = root.height.toFloat()
        val bw = b?.width?.toFloat() ?: sw
        val bh = b?.height?.toFloat() ?: sh
        val sc = max(sw / bw, sh / bh)
        val dx = (sw - bw * sc) / 2f
        val dy = (sh - bh * sc) / 2f
        return floatArrayOf(
            dx + x0 * bw * sc, dy + y0 * bh * sc,
            (x1 - x0) * bw * sc, (y1 - y0) * bh * sc
        )
    }

    private fun put(v: View, r: FloatArray) {
        val lp = FrameLayout.LayoutParams(r[2].toInt(), r[3].toInt())
        lp.leftMargin = r[0].toInt()
        lp.topMargin = r[1].toInt()
        ui.addView(v, lp)
    }

    private fun hot(
        b: Bitmap?, x0: Float, y0: Float, x1: Float, y1: Float,
        rf: Float, onClick: () -> Unit
    ) {
        val r = region(b, x0, y0, x1, y1)
        val v = View(this)
        val g = GradientDrawable()
        g.setStroke(4, gold)
        g.setColor(0x11FFD27A)
        g.cornerRadius = min(r[2], r[3]) * rf
        v.background = g
        v.setOnClickListener { onClick() }
        put(v, r)
        val a = ObjectAnimator.ofFloat(v, "alpha", 0.1f, 0.85f)
        a.duration = 1100
        a.repeatMode = ValueAnimator.REVERSE
        a.repeatCount = ValueAnimator.INFINITE
        a.start()
        pulses.add(a)
    }

    private fun field(
        b: Bitmap?, x0: Float, y0: Float, x1: Float, y1: Float,
        hint: String, preset: String, numeric: Boolean,
        done: (String) -> Unit
    ): EditText {
        val r = region(b, x0, y0, x1, y1)
        val e = EditText(this)
        e.inputType =
            if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        e.hint = hint
        e.setText(preset)
        e.setTextColor(0xFFFFE9B8.toInt())
        e.setHintTextColor(0xFF8C7A5A.toInt())
        e.textSize = 18f
        e.gravity = Gravity.CENTER
        e.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        e.imeOptions = EditorInfo.IME_ACTION_DONE
        val g = GradientDrawable()
        g.setColor(0xFF15110E.toInt())
        g.setStroke(2, 0x55FFD27A)
        g.cornerRadius = r[3] * 0.3f
        e.background = g
        e.setOnEditorActionListener { _, _, _ ->
            done(e.text.toString().trim())
            true
        }
        put(e, r)
        return e
    }

    private fun build(n: Int, b: Bitmap?) {
        if (b == null) status.text = "عکس s" + (n + 1) + " پیدا نشد"
        when (n) {
            0 -> hot(b, 0.28f, 0.765f, 0.79f, 0.84f, 0.5f) { show(1) }
            1 -> {
                hot(b, 0.06f, 0.40f, 0.48f, 0.78f, 0.12f) { pick("viewer") }
                hot(b, 0.52f, 0.40f, 0.94f, 0.78f, 0.12f) { pick("host") }
                hot(b, 0.07f, 0.88f, 0.19f, 0.945f, 0.5f) { show(0) }
            }
            2 -> {
                val ok = { v: String ->
                    nameV = if (v.isEmpty()) "ناشناس" else v
                    show(3)
                }
                val f = field(
                    b, 0.11f, 0.44f, 0.88f, 0.53f, "نام شما",
                    if (nameV.isNotEmpty()) nameV else (prefs.getString("name", "") ?: ""),
                    false, ok
                )
                hot(b, 0.11f, 0.556f, 0.85f, 0.64f, 0.5f) { ok(f.text.toString().trim()) }
            }
            3 -> {
                val ok = { v: String ->
                    if (v.isEmpty()) {
                        status.text = "رمز اتاق را بنویس"
                    } else {
                        roomV = v
                        if (role == "host") show(4) else doJoin()
                    }
                }
                val f = field(
                    b, 0.40f, 0.35f, 0.84f, 0.46f, "رمز اتاق",
                    if (roomV.isNotEmpty()) roomV else (prefs.getString("room", "") ?: ""),
                    false, ok
                )
                hot(b, 0.41f, 0.50f, 0.80f, 0.62f, 0.5f) { ok(f.text.toString().trim()) }
            }
            4 -> {
                val ok = { v: String ->
                    maxV = (v.toIntOrNull() ?: 5).coerceIn(2, 5)
                    doJoin()
                }
                val f = field(
                    b, 0.38f, 0.25f, 0.71f, 0.355f, "۲ تا ۵",
                    prefs.getString("max", "5") ?: "5", true, ok
                )
                hot(b, 0.39f, 0.64f, 0.74f, 0.77f, 0.5f) { ok(f.text.toString().trim()) }
            }
        }
    }

    private fun pick(r: String) {
        role = r
        show(2)
    }

    private fun fail(msg: String) {
        runOnUiThread {
            if (!joining) return@runOnUiThread
            joining = false
            Net.socket?.disconnect()
            show(3)
            status.text = msg
        }
    }

    private fun doJoin() {
        if (joining) return
        imm.hideSoftInputFromWindow(root.windowToken, 0)
        prefs.edit()
            .putString("name", nameV)
            .putString("room", roomV)
            .putString("max", maxV.toString())
            .putString("role", role)
            .apply()
        status.text = "در حال باز کردن در..."
        joining = true
        val s = Net.create()
        s.on(Socket.EVENT_CONNECT) {
            val o = JSONObject()
            o.put("name", nameV)
            o.put("room", roomV)
            o.put("role", role)
            if (role == "host") o.put("max", maxV)
            s.emit("join", o)
        }
        s.on(Socket.EVENT_CONNECT_ERROR) { fail("خطای اتصال به سرور") }
        s.on("full") { fail("اتاق پر است") }
        s.on("host-exists") {
            fail("این اتاق سازنده دارد؛ چند ثانیه صبر کن یا بیننده شو")
        }
        s.on("count") {
            val d = it.firstOrNull() as? JSONObject
            Net.lastCount = d?.optInt("n", 1) ?: 1
        }
        s.on("history") {
            Net.history = it.firstOrNull() as? JSONArray
        }
        s.on("joined") {
            val d = it.firstOrNull() as? JSONObject
            Net.joinedInfo = d
            Net.isHost = d?.optBoolean("host", false) ?: false
            if (!Net.inRoom) {
                Net.inRoom = true
                joining = false
                busy = true
                runOnUiThread {
                    status.text = ""
                    val f = View(this)
                    f.setBackgroundColor(0xFFFFE9B8.toInt())
                    f.alpha = 0f
                    root.addView(
                        f,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    f.animate().alpha(1f).setDuration(700).withEndAction {
                        startActivity(Intent(this, PlayerActivity::class.java))
                        finish()
                    }.start()
                }
            }
        }
        s.connect()
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        for (a in pulses) a.cancel()
        kb?.cancel()
        super.onDestroy()
    }
}
