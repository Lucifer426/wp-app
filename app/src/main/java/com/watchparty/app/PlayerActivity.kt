package com.watchparty.app

import android.net.Uri
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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class PlayerActivity : AppCompatActivity() {
    private var libVLC: LibVLC? = null
    private var player: MediaPlayer? = null
    private val h = Handler(Looper.getMainLooper())
    private lateinit var controls: View
    private lateinit var topBox: View
    private lateinit var chatList: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var countView: TextView
    private var started = false
    private val hide = Runnable { controls.visibility = View.GONE }

    private fun showControls() {
        controls.visibility = View.VISIBLE
        topBox.visibility = if (started) View.GONE else View.VISIBLE
        h.removeCallbacks(hide)
        h.postDelayed(hide, 3000)
    }

    private fun addChat(who: String, text: String) {
        runOnUiThread {
            val tv = TextView(this)
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.setBackgroundColor(0x66000000)
            tv.setPadding(16, 6, 16, 6)
            tv.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            tv.text = if (who.isEmpty()) text else who + ": " + text
            chatList.addView(tv)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )

        val info = Net.joinedInfo
        val firstUrl = info?.optString("url", "") ?: ""
        val firstPlaying = info?.optBoolean("playing", false) ?: false
        val firstTime = info?.optDouble("time", 0.0) ?: 0.0
        val host = Net.isHost

        val root = FrameLayout(this)
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
        top.setBackgroundColor(0x88000000.toInt())
        top.setPadding(24, 24, 24, 24)
        val link = EditText(this)
        link.hint = "Movie link"
        link.setTextColor(0xFFFFFFFF.toInt())
        link.setHintTextColor(0xFFCCCCCC.toInt())
        val go = Button(this)
        go.text = "Play"
        if (host) {
            top.addView(link)
            top.addView(go)
        }
        ctl.addView(
            top,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val mid = LinearLayout(this)
        mid.orientation = LinearLayout.HORIZONTAL
        val back = Button(this)
        back.text = "-10"
        val pp = Button(this)
        pp.text = "II / >"
        val fwd = Button(this)
        fwd.text = "+10"
        mid.addView(back)
        mid.addView(pp)
        mid.addView(fwd)
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        if (host) ctl.addView(mid, lp)

        val chatBtn = Button(this)
        chatBtn.text = "Chat"
        val blp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        blp.gravity = Gravity.BOTTOM or Gravity.START
        blp.bottomMargin = 24
        blp.leftMargin = 24
        ctl.addView(chatBtn, blp)

        root.addView(
            ctl,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val info2 = LinearLayout(this)
        info2.orientation = LinearLayout.VERTICAL
        info2.setPadding(24, 12, 24, 12)
        val role = TextView(this)
        role.setTextColor(0xFFFFFFFF.toInt())
        role.setBackgroundColor(0x66000000)
        role.setPadding(12, 4, 12, 4)
        role.text = if (host) "Host" else "Viewer"
        countView = TextView(this)
        countView.setTextColor(0xFFFFFFFF.toInt())
        countView.setBackgroundColor(0x66000000)
        countView.setPadding(12, 4, 12, 4)
        countView.text = "Viewers: 1"
        info2.addView(role)
        info2.addView(countView)
        val ilp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        ilp.gravity = Gravity.TOP or Gravity.END
        root.addView(info2, ilp)

        val chatBox = LinearLayout(this)
        chatBox.orientation = LinearLayout.VERTICAL
        val input = EditText(this)
        input.hint = "Comment..."
        input.setTextColor(0xFFFFFFFF.toInt())
        input.setHintTextColor(0xFFCCCCCC.toInt())
        input.setBackgroundColor(0x88000000.toInt())
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
        clp.topMargin = 100
        clp.leftMargin = 24
        root.addView(chatBox, clp)
        setContentView(root)

        val imm =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        val vlc = LibVLC(this, arrayListOf("--network-caching=3000"))
        libVLC = vlc
        val mp = MediaPlayer(vlc)
        player = mp
        mp.attachViews(video, null, false, false)

        fun start(u: String) {
            if (u.isEmpty()) return
            val m = Media(vlc, Uri.parse(u))
            mp.media = m
            m.release()
            mp.play()
            started = true
            topBox.visibility = View.GONE
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
            if (!playing && mp.isPlaying) mp.pause()
            if (cur < 0 || abs(cur - ms) > 1000) mp.time = ms
            if (playing && !mp.isPlaying) mp.play()
        }

        val s = Net.socket
        s?.on("chat") { a -> parseChat(a.firstOrNull()) }
        s?.on("count") { a -> setCount(a.firstOrNull()) }
        s?.on("history") { a ->
            val x = a.firstOrNull()
            if (x is JSONArray) {
                for (i in 0 until x.length()) parseChat(x.opt(i))
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
        }

        fun closeInput() {
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
            input.visibility = View.GONE
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

        start(firstUrl)
        if (!host && firstUrl.isNotEmpty()) {
            h.postDelayed({ applyState(firstPlaying, firstTime) }, 2500)
        }

        go.setOnClickListener {
            val u = link.text.toString().trim()
            if (u.isNotEmpty()) {
                start(u)
                val o = JSONObject()
                o.put("mode", "link")
                o.put("url", u)
                Net.socket?.emit("set-source", o)
                h.postDelayed({ sendAction(true, 0L) }, 800)
            }
            showControls()
        }
        pp.setOnClickListener {
            val will = !mp.isPlaying
            if (will) mp.play() else mp.pause()
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

        if (host) {
            val beat = object : Runnable {
                override fun run() {
                    if (started) sendAction(mp.isPlaying, mp.time)
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
