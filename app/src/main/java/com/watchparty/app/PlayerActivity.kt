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
            tv.text = if (who.isEmpty()) text else who + ": " + text
            chatList.addView(tv)
            if (chatList.childCount > 40) chatList.removeViewAt(0)
            chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
            h.postDelayed({ chatList.removeView(tv) }, 8000)
        }
    }

    private fun parseChat(o: Any?) {
        if (o is JSONObject) {
            val who = o.optString("name", o.optString("user", ""))
            var t = o.optString("text", "")
            if (t.isEmpty()) t = o.optString("msg", "")
            if (t.isEmpty()) t = o.optString("message", "")
            if (t.isNotEmpty()) addChat(who, t)
        } else if (o is String && o.isNotEmpty()) {
            addChat("", o)
        }
    }

    private fun setCount(o: Any?) {
        var n = -1
        if (o is Int) n = o
        if (o is Double) n = o.toInt()
        if (o is JSONObject) {
            n = o.optInt("count", o.optInt("n", o.optInt("total", -1)))
        }
        if (n >= 0) {
            runOnUiThread { countView.text = "Viewers: " + n }
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

        val pp = Button(this)
        pp.text = "II / >"
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        if (host) ctl.addView(pp, lp)

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
        input.imeOptions =
            EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
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

        val s = Net.socket
        s?.on("chat") { a -> parseChat(a.firstOrNull()) }
        s?.on("count") { a -> setCount(a.firstOrNull()) }
        s?.on("history") { a ->
            val x = a.firstOrNull()
            if (x is JSONArray) {
                for (i in 0 until x.length()) parseChat(x.opt(i))
            }
        }

        input.setOnEditorActionListener { _, _, _ ->
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                val o = JSONObject()
                o.put("text", t)
                o.put("msg", t)
                s?.emit("chat", o)
                input.setText("")
            }
            true
        }

        start(firstUrl)
        go.setOnClickListener {
            start(link.text.toString().trim())
            showControls()
        }
        pp.setOnClickListener {
            if (mp.isPlaying) mp.pause() else mp.play()
            showControls()
        }
        video.setOnClickListener {
            val imm =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
            showControls()
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
