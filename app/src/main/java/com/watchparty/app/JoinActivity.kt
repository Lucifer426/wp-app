package com.watchparty.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject

class JoinActivity : AppCompatActivity() {
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
        val prefs = getSharedPreferences("fp", MODE_PRIVATE)
        val red = 0xFFD90429.toInt()
        val root = FrameLayout(this)
        root.addView(BgView(this))

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(96, 48, 96, 48)

        val title = TextView(this)
        title.text = "ورود به اتاق"
        title.textSize = 30f
        title.setTextColor(red)
        title.gravity = Gravity.CENTER
        box.addView(title)

        val rg = RadioGroup(this)
        rg.orientation = RadioGroup.HORIZONTAL
        rg.gravity = Gravity.CENTER
        val rbHost = RadioButton(this)
        rbHost.id = View.generateViewId()
        rbHost.text = "سازنده اتاق"
        rbHost.setTextColor(Color.WHITE)
        rbHost.buttonTintList = ColorStateList.valueOf(red)
        val rbView = RadioButton(this)
        rbView.id = View.generateViewId()
        rbView.text = "بیننده"
        rbView.setTextColor(Color.WHITE)
        rbView.buttonTintList = ColorStateList.valueOf(red)
        rg.addView(rbHost)
        rg.addView(rbView)
        box.addView(rg)

        val name = field("اسم تو")
        name.setText(prefs.getString("name", ""))
        val room = field("اسم اتاق")
        room.setText(prefs.getString("room", ""))
        val max = field("سقف نفرات (۲ تا ۵)")
        max.setText(prefs.getString("max", "5"))
        box.addView(name)
        box.addView(room)
        box.addView(max)

        if (prefs.getString("role", "host") == "host") rg.check(rbHost.id)
        else rg.check(rbView.id)
        max.visibility =
            if (rg.checkedRadioButtonId == rbHost.id) View.VISIBLE else View.GONE
        rg.setOnCheckedChangeListener { _, id ->
            max.visibility = if (id == rbHost.id) View.VISIBLE else View.GONE
        }

        val btn = Button(this)
        btn.text = "ورود"
        btn.setTextColor(Color.WHITE)
        val bb = GradientDrawable()
        bb.setColor(red)
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
            val role = if (rg.checkedRadioButtonId == rbHost.id) "host" else "viewer"
            val m = max.text.toString().toIntOrNull() ?: 5
            prefs.edit()
                .putString("name", name.text.toString())
                .putString("room", room.text.toString())
                .putString("max", max.text.toString())
                .putString("role", role)
                .apply()
            join(name.text.toString(), room.text.toString(), m, role)
        }
    }

    private fun say(t: String) {
        runOnUiThread { status.text = t }
    }

    private fun join(n: String, r: String, m: Int, role: String) {
        if (r.trim().isEmpty()) {
            say("اسم اتاق را بنویس")
            return
        }
        val s = Net.create()
        s.on(Socket.EVENT_CONNECT) {
            say("در حال ورود...")
            val o = JSONObject()
            o.put("name", n)
            o.put("room", r)
            o.put("role", role)
            if (role == "host") o.put("max", m)
            s.emit("join", o)
        }
        s.on(Socket.EVENT_CONNECT_ERROR) {
            say("خطای اتصال: " + it.firstOrNull())
        }
        s.on("full") {
            say("اتاق پر است")
        }
        s.on("host-exists") {
            say("این اتاق سازنده دارد. چند ثانیه صبر کن یا به‌عنوان بیننده وارد شو")
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
                runOnUiThread {
                    startActivity(Intent(this, PlayerActivity::class.java))
                }
            }
        }
        s.connect()
    }
}
