package com.watchparty.app

import android.os.Bundle
import android.view.View
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
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(48, 96, 48, 48)

        val name = EditText(this)
        name.hint = "اسم تو"
        val room = EditText(this)
        room.hint = "اسم اتاق"
        val max = EditText(this)
        max.hint = "سقف نفرات (۲ تا ۵)"
        max.setText("5")
        val btn = Button(this)
        btn.text = "ورود"
        status = TextView(this)
        status.text = "آماده"

        box.addView(name)
        box.addView(room)
        box.addView(max)
        box.addView(btn)
        box.addView(status)
        setContentView(box)

        btn.setOnClickListener {
            connect(
                name.text.toString(),
                room.text.toString(),
                max.text.toString().toIntOrNull() ?: 5
            )
        }
    }

    private fun say(t: String) {
        runOnUiThread { status.text = t }
    }

    private fun connect(n: String, r: String, m: Int) {
        socket?.disconnect()
        val s = IO.socket("http://86.54.42.247")
        socket = s
        s.on(Socket.EVENT_CONNECT) {
            say("وصل شد، در حال ورود...")
            val o = JSONObject()
            o.put("name", n)
            o.put("room", r)
            o.put("max", m)
            s.emit("join", o)
        }
        s.on(Socket.EVENT_CONNECT_ERROR) {
            say("خطای اتصال: " + it.firstOrNull())
        }
        s.on("joined") {
            say("وارد اتاق شدی: " + it.firstOrNull())
        }
        s.connect()
    }

    override fun onDestroy() {
        socket?.disconnect()
        super.onDestroy()
    }
}
