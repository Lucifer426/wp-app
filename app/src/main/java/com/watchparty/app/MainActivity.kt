package com.watchparty.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(48, 96, 48, 48)

        val name = EditText(this)
        name.hint = "Name"
        val room = EditText(this)
        room.hint = "Room"
        val max = EditText(this)
        max.hint = "Max (2-5)"
        max.setText("5")
        val btn = Button(this)
        btn.text = "Join"
        status = TextView(this)
        status.text = "Ready"

        box.addView(name)
        box.addView(room)
        box.addView(max)
        box.addView(btn)
        box.addView(status)
        setContentView(box)

        btn.setOnClickListener {
            val m = max.text.toString().toIntOrNull() ?: 5
            join(name.text.toString(), room.text.toString(), m)
        }
    }

    private fun say(t: String) {
        runOnUiThread { status.text = t }
    }

    private fun join(n: String, r: String, m: Int) {
        val s = Net.create()
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
            val d = it.firstOrNull() as? JSONObject
            Net.joinedInfo = d
            Net.isHost = d?.optBoolean("host", false) ?: false
            runOnUiThread {
                startActivity(Intent(this, PlayerActivity::class.java))
            }
        }
        s.connect()
    }
}
