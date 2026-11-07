package com.watchparty.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView

    private fun field(hint: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(0xFF999999.toInt())
        e.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        e.setPadding(32, 28, 32, 28)
        val bg = GradientDrawable()
        bg.setColor(0x33FFFFFF)
        bg.cornerRadius = 24f
        e.background = bg
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = 24
        e.layoutParams = lp
        return e
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.addView(BgView(this))

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(96, 48, 96, 48)

        val title = TextView(this)
        title.text = "فیلم پارتی"
        title.textSize = 34f
        title.setTextColor(0xFFD90429.toInt())
        title.gravity = Gravity.CENTER
        box.addView(title)

        val name = field("اسم تو")
        val room = field("اسم اتاق")
        val max = field("سقف نفرات (۲ تا ۵)")
        max.setText("5")
        box.addView(name)
        box.addView(room)
        box.addView(max)

        val btn = Button(this)
        btn.text = "ورود"
        btn.setTextColor(Color.WHITE)
        val bb = GradientDrawable()
        bb.setColor(0xFFD90429.toInt())
        bb.cornerRadius = 24f
        btn.background = bb
        val blp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        blp.topMargin = 48
        box.addView(btn, blp)

        status = TextView(this)
        status.setTextColor(0xFFCCCCCC.toInt())
        status.gravity = Gravity.CENTER
        status.setPadding(0, 24, 0, 0)
        box.addView(status)

        root.addView(
            box,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

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
            say("در حال ورود...")
            val o = JSONObject()
            o.put("name", n)
            o.put("room", r)
            o.put("max", m)
            s.emit("join", o)
        }
        s.on(Socket.EVENT_CONNECT_ERROR) {
            say("خطای اتصال: " + it.firstOrNull())
        }
        s.on("full") {
            say("اتاق پر است")
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
