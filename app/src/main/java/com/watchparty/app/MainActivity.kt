package com.watchparty.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var socket: Socket? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(48, 96, 48, 48)
        scroll.addView(box)

        val name = EditText(this)
        name.hint = "Name"
        val room = EditText(this)
        room.hint = "Room"
        val max = EditText(this)
        max.hint = "Max (2-5)"
        max.setText("5")
        val btn = Button(this)
        btn.text = "Join"
        val link = EditText(this)
        link.hint = "Movie link"
        val vlcBtn = Button(this)
        vlcBtn.text = "Play with VLC"
        val mxBtn = Button(this)
        mxBtn.text = "Open with MX Player"
        status = TextView(this)
        status.text = "Ready"

        box.addView(name)
        box.addView(room)
        box.addView(max)
        box.addView(btn)
        box.addView(link)
        box.addView(vlcBtn)
        box.addView(mxBtn)
        box.addView(status)
        setContentView(scroll)

        btn.setOnClickListener {
            val m = max.text.toString().toIntOrNull() ?: 5
            connect(name.text.toString(), room.text.toString(), m)
        }
        vlcBtn.setOnClickListener {
            val i = Intent(this, PlayerActivity::class.java)
            i.putExtra("url", link.text.toString().trim())
            startActivity(i)
        }
        mxBtn.setOnClickListener {
            openMx(link.text.toString().trim())
        }
    }

    private fun openMx(url: String) {
        val pkgs = listOf(
            "com.mxtech.videoplayer.pro",
            "com.mxtech.videoplayer.ad"
        )
        for (p in pkgs) {
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.setDataAndType(Uri.parse(url), "video/*")
                i.setPackage(p)
                startActivity(i)
                return
            } catch (e: Exception) {
            }
        }
        say("MX Player not found")
    }

    private fun say(t: String) {
        runOnUiThread { status.text = t }
    }

    private fun connect(n: String, r: String, m: Int) {
        socket?.disconnect()
        val s = IO.socket("http://86.54.42.247")
        socket = s
        s.on(Socket.EVENT_CONNECT) {
            say("Connected, joining...")
            val o = JSONObject()
            o.put("name", n)
            o.put("room", r)
            o.put("max", m)
            s.emit("join", o)
        }
        s.on(Socket.EVENT_CONNECT_ERROR) {
            say("Connection error: " + it.firstOrNull())
        }
        s.on("joined") {
            say("Joined: " + it.firstOrNull())
        }
        s.connect()
    }

    override fun onDestroy() {
        socket?.disconnect()
        super.onDestroy()
    }
}
