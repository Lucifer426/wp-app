package com.watchparty.app

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class PlayerActivity : AppCompatActivity() {
    private var libVLC: LibVLC? = null
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        val top = LinearLayout(this)
        top.orientation = LinearLayout.VERTICAL
        top.setBackgroundColor(0x88000000.toInt())
        top.setPadding(24, 24, 24, 24)

        val role = TextView(this)
        role.setTextColor(0xFFFFFFFF.toInt())
        role.text = if (host) "You are the host" else "You are a viewer"
        top.addView(role)

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
        root.addView(
            top,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val pp = Button(this)
        pp.text = "Play / Pause"
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.bottomMargin = 48
        if (host) root.addView(pp, lp)
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
        }

        start(firstUrl)
        go.setOnClickListener { start(link.text.toString().trim()) }
        pp.setOnClickListener {
            if (mp.isPlaying) mp.pause() else mp.play()
        }
    }

    override fun onDestroy() {
        player?.stop()
        player?.detachViews()
        player?.release()
        libVLC?.release()
        Net.socket?.disconnect()
        super.onDestroy()
    }
}
