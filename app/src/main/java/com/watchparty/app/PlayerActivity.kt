package com.watchparty.app

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class IconBtn(c: Context, private val kind: Int) : View(c) {
    var pauseIcon = false
        set(v) {
            field = v
            invalidate()
        }
    var active = false
        set(v) {
            field = v
            invalidate()
        }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ic = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        fill.color = 0xCC0B0B0F.toInt()
        ring.color = 0xFFD90429.toInt()
        ring.style = Paint.Style.STROKE
        ring.strokeWidth = 4f
        ic.color = 0xFFFFFFFF.toInt()
        ic.textAlign = Paint.Align.CENTER
        ic.isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - 4f
        fill.color = if (active) 0xFFD90429.toInt() else 0xCC0B0B0F.toInt()
        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, ring)
        if (kind == 0 || kind == 2) {
            ic.textSize = r * 0.65f
            val label = if (kind == 0) "-10" else "+10"
            canvas.drawText(label, cx, cy + r * 0.22f, ic)
        } else if (kind == 1) {
            if (pauseIcon) {
                val w = r * 0.18f
                val hh = r * 0.5f
                canvas.drawRect(cx - w * 2f, cy - hh, cx - w * 0.5f, cy + hh, ic)
                canvas.drawRect(cx + w * 0.5f, cy - hh, cx + w * 2f, cy + hh, ic)
            } else {
                val p = Path()
                p.moveTo(cx - r * 0.25f, cy - r * 0.5f)
                p.lineTo(cx - r * 0.25f, cy + r * 0.5f)
                p.lineTo(cx + r * 0.55f, cy)
                p.close()
                canvas.drawPath(p, ic)
            }
        } else if (kind in 5..8) {
            ic.textSize = if (kind == 6) r * 0.42f else r * 0.55f
            val label = when (kind) {
                5 -> if (active) "SW" else "HW"
                7 -> if (active) "SF" else "TX"
                8 -> "FN"
                else -> "SUB"
            }
            canvas.drawText(label, cx, cy + r * 0.2f, ic)
        } else if (kind == 4) {
            ic.style = Paint.Style.STROKE
            ic.strokeWidth = r * 0.16f
            ic.strokeCap = Paint.Cap.ROUND
            val p = Path()
            p.moveTo(cx + r * 0.1f, cy - r * 0.42f)
            p.lineTo(cx - r * 0.3f, cy)
            p.lineTo(cx + r * 0.1f, cy + r * 0.42f)
            canvas.drawPath(p, ic)
            canvas.drawLine(cx - r * 0.3f, cy, cx + r * 0.45f, cy, ic)
            ic.style = Paint.Style.FILL
        } else {
            canvas.drawRoundRect(
                RectF(cx - r * 0.5f, cy - r * 0.4f, cx + r * 0.5f, cy + r * 0.2f),
                r * 0.15f, r * 0.15f, ic
            )
            val p = Path()
            p.moveTo(cx - r * 0.3f, cy + r * 0.15f)
            p.lineTo(cx - r * 0.3f, cy + r * 0.5f)
            p.lineTo(cx + r * 0.05f, cy + r * 0.15f)
            p.close()
            canvas.drawPath(p, ic)
        }
    }
}

class PlayerActivity : AppCompatActivity() {
    private var libVLC: LibVLC? = null
    private var player: MediaPlayer? = null
    private val h = Handler(Looper.getMainLooper())
    private lateinit var controls: View
    private lateinit var infoBox: View
    private lateinit var topBox: View
    private lateinit var chatList: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var countView: TextView
    private lateinit var waitView: TextView
    private var started = false
    private var lastBack = 0L

    private val hide = Runnable {
        controls.animate().cancel()
        controls.animate().alpha(0f).setDuration(400).withEndAction {
            controls.visibility = View.GONE
        }.start()
        infoBox.animate().cancel()
        infoBox.animate().alpha(0f).setDuration(400).withEndAction {
            infoBox.visibility = View.GONE
        }.start()
    }

    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    private fun showControls() {
        controls.animate().cancel()
        controls.alpha = 1f
        controls.visibility = View.VISIBLE
        infoBox.animate().cancel()
        infoBox.alpha = 1f
        infoBox.visibility = View.VISIBLE
        topBox.visibility = if (started) View.GONE else View.VISIBLE
        h.removeCallbacks(hide)
        h.postDelayed(hide, 3000)
    }

    private fun leave() {
        h.removeCallbacksAndMessages(null)
        Net.socket?.disconnect()
        Net.inRoom = false
        val i = Intent(this, StoryActivity::class.java)
        i.putExtra("skip", true)
        startActivity(i)
        finish()
    }

    private fun fmt(ms: Long): String {
        val s = maxOf(ms, 0L) / 1000
        val hr = s / 3600
        val mi = (s % 3600) / 60
        val se = s % 60
        return if (hr > 0) String.format("%d:%02d:%02d", hr, mi, se)
        else String.format("%02d:%02d", mi, se)
    }

    private fun addChat(who: String, text: String) {
        runOnUiThread {
            val tv = TextView(this)
            tv.setTextColor(0xFFFFFFFF.toInt())
            val bg = GradientDrawable()
            bg.setColor(0x99000000.toInt())
            bg.cornerRadius = 28f
            tv.background = bg
            tv.setPadding(24, 10, 24, 10)
            tv.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            tv.text = if (who.isEmpty()) text else who + ": " + text
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 8
            chatList.addView(tv, lp)
            if (chatList.childCount > 40) chatList.removeViewAt(0)
            chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
            h.postDelayed({ chatList.removeView(tv) }, 8000)
        }
    }

    private fun parseChat(o: Any?) {
        if (o is JSONObject) {
            val who = o.optString("name", "")
            val t = o.optString("text", "")
            if (t.isNotEmpty()) addChat(who, t)
        }
    }

    private fun setCount(o: Any?) {
        if (o is JSONObject) {
            val n = o.optInt("n", -1)
            if (n >= 0) {
                runOnUiThread { countView.text = "Viewers: " + n }
            }
        }
    }

    private fun findArabicFont(): String? {
        val dirs = listOf(
            "/system/fonts", "/product/fonts",
            "/system_ext/fonts", "/system/product/fonts"
        )
        var any: String? = null
        for (d in dirs) {
            val fs = java.io.File(d).listFiles() ?: continue
            for (f in fs) {
                val n = f.name.lowercase()
                if (!n.endsWith(".ttf") && !n.endsWith(".otf")) continue
                if (!n.contains("arabic")) continue
                if (n.contains("regular")) return f.absolutePath
                if (any == null) any = f.absolutePath
            }
        }
        return any
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
        goFullscreen()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val now = System.currentTimeMillis()
                    if (now - lastBack < 2000) {
                        leave()
                    } else {
                        lastBack = now
                        Toast.makeText(
                            this@PlayerActivity,
                            "دوباره بزن تا از اتاق خارج شی",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        val info = Net.joinedInfo
        val firstUrl = info?.optString("url", "") ?: ""
        val firstPlaying = info?.optBoolean("playing", false) ?: false
        val firstTime = info?.optDouble("time", 0.0) ?: 0.0
        val host = Net.isHost
        val red = 0xFFD90429.toInt()

        val root = FrameLayout(this)
        root.setBackgroundColor(0xFF000000.toInt())
        val video = VLCVideoLayout(this)
        root.addView(
            video,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val ctl = FrameLayout(this)
        controls = ctl

        val top = LinearLayout(this)
        topBox = top
        top.orientation = LinearLayout.VERTICAL
        top.setBackgroundColor(0xAA000000.toInt())
        top.setPadding(24, 24, 24, 24)
        val link = EditText(this)
        link.hint = "Movie link"
        link.setTextColor(0xFFFFFFFF.toInt())
        link.setHintTextColor(0xFFCCCCCC.toInt())
        val go = Button(this)
        go.text = "Play"
        go.setTextColor(0xFFFFFFFF.toInt())
        val gbg = GradientDrawable()
        gbg.setColor(red)
        gbg.cornerRadius = 24f
        go.background = gbg
        if (host) {
            top.addView(link)
            top.addView(go)
        }
        val topLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        topLp.topMargin = 130
        ctl.addView(top, topLp)

        val backBtn = IconBtn(this, 4)
        val backLp = FrameLayout.LayoutParams(80, 80)
        backLp.gravity = Gravity.TOP or Gravity.START
        backLp.topMargin = 24
        backLp.leftMargin = 48
        ctl.addView(backBtn, backLp)

        val compatBtn = IconBtn(this, 5)
        val compatLp = FrameLayout.LayoutParams(80, 80)
        compatLp.gravity = Gravity.TOP or Gravity.START
        compatLp.topMargin = 24
        compatLp.leftMargin = 152
        ctl.addView(compatBtn, compatLp)

        val subBtn = IconBtn(this, 6)
        val subLp = FrameLayout.LayoutParams(80, 80)
        subLp.gravity = Gravity.TOP or Gravity.START
        subLp.topMargin = 24
        subLp.leftMargin = 256
        ctl.addView(subBtn, subLp)

        val viewBtn = IconBtn(this, 7)
        val viewLp = FrameLayout.LayoutParams(80, 80)
        viewLp.gravity = Gravity.TOP or Gravity.START
        viewLp.topMargin = 24
        viewLp.leftMargin = 360
        ctl.addView(viewBtn, viewLp)

        val fontBtn = IconBtn(this, 8)
        val fontLp = FrameLayout.LayoutParams(80, 80)
        fontLp.gravity = Gravity.TOP or Gravity.START
        fontLp.topMargin = 24
        fontLp.leftMargin = 464
        ctl.addView(fontBtn, fontLp)

        val mid = LinearLayout(this)
        mid.orientation = LinearLayout.HORIZONTAL
        val back = IconBtn(this, 0)
        val pp = IconBtn(this, 1)
        val fwd = IconBtn(this, 2)
        for (b in listOf(back, pp, fwd)) {
            val blp = LinearLayout.LayoutParams(96, 96)
            blp.leftMargin = 16
            blp.rightMargin = 16
            mid.addView(b, blp)
        }
        val mlp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mlp.gravity = Gravity.CENTER
        if (host) ctl.addView(mid, mlp)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        val timeText = TextView(this)
        timeText.setTextColor(0xFFFFFFFF.toInt())
        timeText.text = "00:00 / 00:00"
        val seek = SeekBar(this)
        seek.max = 1000
        seek.progressTintList = ColorStateList.valueOf(red)
        seek.thumbTintList = ColorStateList.valueOf(red)
        seek.isEnabled = host
        bar.addView(timeText)
        bar.addView(
            seek,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val barLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        barLp.gravity = Gravity.BOTTOM
        barLp.leftMargin = 48
        barLp.rightMargin = 190
        barLp.bottomMargin = 50
        ctl.addView(bar, barLp)

        val chatBtn = IconBtn(this, 3)
        val chatLp = FrameLayout.LayoutParams(96, 96)
        chatLp.gravity = Gravity.BOTTOM or Gravity.END
        chatLp.bottomMargin = 40
        chatLp.rightMargin = 48
        ctl.addView(chatBtn, chatLp)

        root.addView(
            ctl,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val flash = IconBtn(this, 1)
        flash.alpha = 0f
        val flp = FrameLayout.LayoutParams(200, 200)
        flp.gravity = Gravity.CENTER
        root.addView(flash, flp)

        waitView = TextView(this)
        waitView.setTextColor(0xFFFFFFFF.toInt())
        waitView.textSize = 18f
        val wbg = GradientDrawable()
        wbg.setColor(0xAA000000.toInt())
        wbg.cornerRadius = 28f
        waitView.background = wbg
        waitView.setPadding(40, 20, 40, 20)
        waitView.text = "منتظر سازنده اتاق..."
        waitView.visibility =
            if (!host && firstUrl.isEmpty()) View.VISIBLE else View.GONE
        val wlp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        wlp.gravity = Gravity.CENTER
        root.addView(waitView, wlp)

        val info2 = LinearLayout(this)
        infoBox = info2
        info2.orientation = LinearLayout.VERTICAL
        info2.setPadding(24, 12, 24, 12)
        val role = TextView(this)
        role.setTextColor(0xFFFFFFFF.toInt())
        val rbg = GradientDrawable()
        rbg.setColor(0x99000000.toInt())
        rbg.cornerRadius = 20f
        role.background = rbg
        role.setPadding(20, 6, 20, 6)
        role.text = if (host) "Host" else "Viewer"
        countView = TextView(this)
        countView.setTextColor(0xFFFFFFFF.toInt())
        val cbg = GradientDrawable()
        cbg.setColor(0x99000000.toInt())
        cbg.cornerRadius = 20f
        countView.background = cbg
        countView.setPadding(20, 6, 20, 6)
        countView.text = "Viewers: " + Net.lastCount
        info2.addView(role)
        info2.addView(countView)
        val ilp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        ilp.gravity = Gravity.TOP or Gravity.END
        ilp.topMargin = 12
        ilp.rightMargin = 24
        root.addView(info2, ilp)

        val chatBox = LinearLayout(this)
        chatBox.orientation = LinearLayout.VERTICAL
        val input = EditText(this)
        input.hint = "Comment..."
        input.setTextColor(0xFFFFFFFF.toInt())
        input.setHintTextColor(0xFFCCCCCC.toInt())
        val ibg = GradientDrawable()
        ibg.setColor(0xAA000000.toInt())
        ibg.cornerRadius = 28f
        input.background = ibg
        input.setPadding(28, 16, 28, 16)
        input.setSingleLine(true)
        input.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        input.imeOptions =
            EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        input.visibility = View.GONE
        chatBox.addView(input)
        chatScroll = ScrollView(this)
        chatList = LinearLayout(this)
        chatList.orientation = LinearLayout.VERTICAL
        chatScroll.addView(chatList)
        chatBox.addView(
            chatScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                320
            )
        )
        val clp = FrameLayout.LayoutParams(
            600,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        clp.gravity = Gravity.TOP or Gravity.START
        clp.topMargin = 130
        clp.leftMargin = 48
        root.addView(chatBox, clp)
        setContentView(root)

        val hist = Net.history
        if (hist != null) {
            val from = maxOf(0, hist.length() - 5)
            for (i in from until hist.length()) parseChat(hist.opt(i))
        }

        val imm =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        val prefs = getSharedPreferences("player", MODE_PRIVATE)
        var compat = prefs.getBoolean("compat", false)
        var useTex = prefs.getBoolean("tex", true)
        var useFont = prefs.getBoolean("font", false)
        var curUrl = ""

        fun makeVlc(): LibVLC {
            val o = arrayListOf(
                "--network-caching=3000",
                "--freetype-rel-fontsize=16",
                "--subsdec-encoding=UTF-8"
            )
            if (compat) o.add("--avcodec-hw=none")
            if (useFont) {
                val f = findArabicFont()
                if (f != null) o.add("--freetype-font=" + f)
            }
            return LibVLC(this, o)
        }

        var vlc = makeVlc()
        libVLC = vlc
        var mp = MediaPlayer(vlc)
        player = mp
        mp.attachViews(video, null, true, useTex)

        var dragging = false
        var reattached = false
        var allowBeat = true

        fun flashIcon(playing: Boolean) {
            flash.pauseIcon = !playing
            flash.animate().cancel()
            flash.alpha = 1f
            flash.animate().alpha(0f).setStartDelay(400).setDuration(500).start()
        }

        fun pickSub() {
            try {
                val tracks = mp.spuTracks ?: return
                if (tracks.isEmpty()) return
                if (mp.spuTrack != -1) return
                var best = -1
                for (t in tracks) {
                    if (t.id == -1) continue
                    val n = (t.name ?: "").lowercase()
                    if (best == -1) best = t.id
                    if (n.contains("fa") || n.contains("per") ||
                        n.contains("فارسی")
                    ) {
                        best = t.id
                        break
                    }
                }
                if (best != -1) mp.spuTrack = best
            } catch (e: Exception) {
            }
        }

        fun start(u: String) {
            if (u.isEmpty()) return
            curUrl = u
            val m = Media(vlc, Uri.parse(u))
            if (compat) m.setHWDecoderEnabled(false, false)
            mp.media = m
            m.release()
            mp.play()
            started = true
            waitView.visibility = View.GONE
            topBox.visibility = View.GONE
            for (d in longArrayOf(2500, 5000, 9000, 15000, 25000)) {
                h.postDelayed({ pickSub() }, d)
            }
            if (host && !reattached) {
                reattached = true
                h.postDelayed({
                    val t = mp.time
                    mp.detachViews()
                    mp.attachViews(video, null, true, useTex)
                    if (t > 0) mp.time = t
                }, 2500)
            }
        }

        fun rebuildPlayer() {
            val wasPlaying = mp.isPlaying
            val t = mp.time
            try {
                mp.stop()
                mp.detachViews()
                mp.release()
                vlc.release()
            } catch (e: Exception) {
            }
            vlc = makeVlc()
            libVLC = vlc
            mp = MediaPlayer(vlc)
            player = mp
            mp.attachViews(video, null, true, useTex)
            if (curUrl.isNotEmpty()) {
                val m = Media(vlc, Uri.parse(curUrl))
                if (compat) m.setHWDecoderEnabled(false, false)
                mp.media = m
                m.release()
                mp.play()
                h.postDelayed({
                    if (t > 0) mp.time = t
                    if (!wasPlaying) mp.pause()
                }, 2500)
                for (d in longArrayOf(4000, 7000, 12000)) {
                    h.postDelayed({ pickSub() }, d)
                }
            }
        }

        fun sendAction(playing: Boolean, ms: Long) {
            val o = JSONObject()
            o.put("playing", playing)
            o.put("time", maxOf(ms, 0L) / 1000.0)
            Net.socket?.emit("action", o)
        }

        fun sendTwice(playing: Boolean, ms: Long) {
            sendAction(playing, ms)
            h.postDelayed({ sendAction(mp.isPlaying, mp.time) }, 1000)
        }

        fun applyState(playing: Boolean, sec: Double) {
            val ms = (sec * 1000).toLong()
            val cur = mp.time
            val was = mp.isPlaying
            if (!playing && mp.isPlaying) mp.pause()
            if (cur < 0 || abs(cur - ms) > 1000) mp.time = ms
            if (playing && !mp.isPlaying) mp.play()
            if (was != playing) flashIcon(playing)
        }

        val s = Net.socket
        s?.on("chat") { a -> parseChat(a.firstOrNull()) }
        s?.on("count") { a -> setCount(a.firstOrNull()) }
        s?.on("history") { a ->
            val x = a.firstOrNull()
            if (x is JSONArray) {
                val from = maxOf(0, x.length() - 5)
                for (i in from until x.length()) parseChat(x.opt(i))
            }
        }
        if (!host) {
            s?.on("source") { a ->
                val d = a.firstOrNull() as? JSONObject
                val u = d?.optString("url", "") ?: ""
                runOnUiThread { start(u) }
            }
            s?.on("action") { a ->
                val d = a.firstOrNull() as? JSONObject
                if (d != null) {
                    val pl = d.optBoolean("playing", false)
                    val t = d.optDouble("time", 0.0)
                    runOnUiThread { applyState(pl, t) }
                }
            }
            s?.on("host-left") {
                runOnUiThread {
                    waitView.text = "سازنده خارج شد؛ منتظر بازگشت..."
                    waitView.visibility = View.VISIBLE
                }
            }
            s?.on("host-joined") {
                runOnUiThread {
                    if (started) {
                        waitView.visibility = View.GONE
                    } else {
                        waitView.text = "منتظر انتخاب فیلم..."
                    }
                }
            }
        }

        fun closeInput() {
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
            input.visibility = View.GONE
        }

        backBtn.setOnClickListener { leave() }

        compatBtn.active = compat
        compatBtn.setOnClickListener {
            compat = !compat
            prefs.edit().putBoolean("compat", compat).apply()
            compatBtn.active = compat
            Toast.makeText(
                this,
                if (compat) "دیکودر: نرم‌افزاری (SW)" else "دیکودر: سخت‌افزاری (HW)",
                Toast.LENGTH_SHORT
            ).show()
            allowBeat = false
            rebuildPlayer()
            h.postDelayed({ allowBeat = true }, 3500)
            showControls()
        }

        viewBtn.active = !useTex
        viewBtn.setOnClickListener {
            useTex = !useTex
            prefs.edit().putBoolean("tex", useTex).apply()
            viewBtn.active = !useTex
            Toast.makeText(
                this,
                if (useTex) "نمایش: TextureView (TX)" else "نمایش: SurfaceView (SF)",
                Toast.LENGTH_SHORT
            ).show()
            allowBeat = false
            rebuildPlayer()
            h.postDelayed({ allowBeat = true }, 3500)
            showControls()
        }

        fontBtn.active = useFont
        fontBtn.setOnClickListener {
            useFont = !useFont
            prefs.edit().putBoolean("font", useFont).apply()
            fontBtn.active = useFont
            val f = if (useFont) findArabicFont() else null
            Toast.makeText(
                this,
                if (!useFont) "فونت: پیش‌فرض"
                else if (f == null) "فونت عربی توی گوشی پیدا نشد"
                else "فونت: " + f.substringAfterLast('/'),
                Toast.LENGTH_LONG
            ).show()
            allowBeat = false
            rebuildPlayer()
            h.postDelayed({ allowBeat = true }, 3500)
            showControls()
        }

        subBtn.setOnClickListener {
            try {
                val all = mp.spuTracks
                val list = all?.filter { it.id != -1 } ?: emptyList()
                val mode = (if (compat) "SW" else "HW") +
                    " / " + (if (useTex) "TX" else "SF")
                if (list.isEmpty()) {
                    Toast.makeText(
                        this,
                        "ترک زیرنویس: 0 ($mode)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val cur = mp.spuTrack
                    val idx = list.indexOfFirst { it.id == cur }
                    val next = if (idx + 1 < list.size) list[idx + 1] else null
                    mp.spuTrack = next?.id ?: -1
                    Toast.makeText(
                        this,
                        "ترک‌ها: " + list.size + " | قبل از لمس: " +
                            (if (cur == -1) "خاموش" else "روشن") + " | حالا: " +
                            (if (next == null) "خاموش شد" else "روشن شد") +
                            " | " + mode,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "خطا: " + e.message, Toast.LENGTH_LONG).show()
            }
            showControls()
        }

        chatBtn.setOnClickListener {
            input.visibility = View.VISIBLE
            input.requestFocus()
            imm.showSoftInput(input, 0)
            showControls()
        }

        input.setOnEditorActionListener { _, _, _ ->
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                s?.emit("chat", t)
                input.setText("")
            }
            closeInput()
            true
        }

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                sb: SeekBar?, p: Int, fromUser: Boolean
            ) {
                if (fromUser) {
                    val len = mp.length
                    timeText.text =
                        fmt(p.toLong() * len / 1000) + " / " + fmt(len)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                dragging = true
                h.removeCallbacks(hide)
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                dragging = false
                val len = mp.length
                val p = sb?.progress ?: 0
                if (len > 0) {
                    val t = p.toLong() * len / 1000
                    mp.time = t
                    sendTwice(mp.isPlaying, t)
                }
                showControls()
            }
        })

        h.postDelayed({ start(firstUrl) }, 600)
        if (firstUrl.isNotEmpty()) {
            if (host) {
                if (firstTime > 1.0) {
                    allowBeat = false
                    h.postDelayed({
                        mp.time = (firstTime * 1000).toLong()
                        allowBeat = true
                    }, 4500)
                }
            } else {
                h.postDelayed({ applyState(firstPlaying, firstTime) }, 3000)
            }
        }

        go.setOnClickListener {
            val u = link.text.toString().trim()
            if (u.isNotEmpty()) {
                imm.hideSoftInputFromWindow(link.windowToken, 0)
                link.clearFocus()
                h.postDelayed({
                    start(u)
                    val o = JSONObject()
                    o.put("mode", "link")
                    o.put("url", u)
                    Net.socket?.emit("set-source", o)
                    h.postDelayed({ sendAction(true, 0L) }, 800)
                }, 400)
            }
            showControls()
        }
        pp.setOnClickListener {
            val will = !mp.isPlaying
            if (will) mp.play() else mp.pause()
            flashIcon(will)
            pp.pauseIcon = will
            sendTwice(will, mp.time)
            showControls()
        }
        back.setOnClickListener {
            val t = maxOf(mp.time - 10000, 0L)
            mp.time = t
            sendTwice(mp.isPlaying, t)
            showControls()
        }
        fwd.setOnClickListener {
            val t = mp.time + 10000
            mp.time = t
            sendTwice(mp.isPlaying, t)
            showControls()
        }
        video.setOnClickListener {
            closeInput()
            showControls()
        }

        val ticker = object : Runnable {
            override fun run() {
                if (started) {
                    val len = mp.length
                    val t = mp.time
                    if (!dragging && len > 0) {
                        seek.progress = (t * 1000 / len).toInt()
                        timeText.text = fmt(t) + " / " + fmt(len)
                    }
                    pp.pauseIcon = mp.isPlaying
                }
                h.postDelayed(this, 500)
            }
        }
        h.postDelayed(ticker, 500)

        if (host) {
            val beat = object : Runnable {
                override fun run() {
                    if (started && allowBeat) sendAction(mp.isPlaying, mp.time)
                    h.postDelayed(this, 2000)
                }
            }
            h.postDelayed(beat, 2000)
        }
        showControls()
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        player?.stop()
        player?.detachViews()
        player?.release()
        libVLC?.release()
        Net.socket?.disconnect()
        super.onDestroy()
    }
}
