package com.watchparty.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject

class StoryActivity : AppCompatActivity() {
    private lateinit var sv: StoryView
    private lateinit var title: TextView
    private lateinit var cardHost: TextView
    private lateinit var cardView: TextView
    private lateinit var panel: LinearLayout
    private lateinit var prompt: TextView
    private lateinit var input: EditText
    private lateinit var okBtn: Button
    private lateinit var status: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var imm: InputMethodManager
    private val h = Handler(Looper.getMainLooper())
    private var role = "host"
    private var nameV = ""
    private var roomV = ""
    private var maxV = 5
    private var joining = false
    private var busy = false
    private var step = 0
    private var onOk: (String) -> Unit = {}
    private val red = 0xFFD90429.toInt()

    private fun card(
        root: FrameLayout, txt: String, cx: Int, top: Int, wd: Int
    ): TextView {
        val t = TextView(this)
        t.text = txt
        t.textSize = 18f
        t.setTextColor(Color.WHITE)
        t.gravity = Gravity.CENTER
        t.setPadding(20, 24, 20, 24)
        val bg = GradientDrawable()
        bg.setColor(0xDD0B0B0F.toInt())
        bg.setStroke(4, red)
        bg.cornerRadius = 36f
        t.background = bg
        val lp = FrameLayout.LayoutParams(wd, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = cx - wd / 2
        lp.topMargin = top
        root.addView(t, lp)
        t.visibility = View.GONE
        return t
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefs = getSharedPreferences("fp", MODE_PRIVATE)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val dm = resources.displayMetrics
        val sw = dm.widthPixels
        val sh = dm.heightPixels

        val root = FrameLayout(this)
        sv = StoryView(this)
        root.addView(
            sv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        title = TextView(this)
        title.text = "نقش شما؟"
        title.textSize = 32f
        title.setTextColor(Color.WHITE)
        title.setShadowLayer(18f, 0f, 0f, red)
        title.gravity = Gravity.CENTER
        title.visibility = View.GONE
        val tlp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tlp.topMargin = (sh * 0.10f).toInt()
        root.addView(title, tlp)

        cardHost = card(root, "سازنده اتاق", (sw * 0.73f).toInt(), (sh * 0.57f).toInt(), (sw * 0.38f).toInt())
        cardView = card(root, "بیننده", (sw * 0.27f).toInt(), (sh * 0.57f).toInt(), (sw * 0.38f).toInt())
        cardHost.setOnClickListener { pick("host") }
        cardView.setOnClickListener { pick("viewer") }

        panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(40, 36, 40, 36)
        val pbg = GradientDrawable()
        pbg.setColor(0xEE0B0B0F.toInt())
        pbg.setStroke(4, red)
        pbg.cornerRadius = 40f
        panel.background = pbg
        panel.visibility = View.GONE

        prompt = TextView(this)
        prompt.textSize = 22f
        prompt.setTextColor(Color.WHITE)
        prompt.gravity = Gravity.CENTER
        panel.addView(prompt)

        input = EditText(this)
        input.setTextColor(Color.WHITE)
        input.textSize = 20f
        input.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        input.setPadding(32, 24, 32, 24)
        val ibg = GradientDrawable()
        ibg.setColor(0x33FFFFFF)
        ibg.cornerRadius = 24f
        input.background = ibg
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        val ilp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        ilp.topMargin = 24
        panel.addView(input, ilp)

        okBtn = Button(this)
        okBtn.text = "تأیید"
        okBtn.setTextColor(Color.WHITE)
        val obg = GradientDrawable()
        obg.setColor(red)
        obg.cornerRadius = 40f
        okBtn.background = obg
        val olp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        olp.topMargin = 24
        panel.addView(okBtn, olp)

        val plp = FrameLayout.LayoutParams(
            (sw * 0.84f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
        )
        plp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        plp.topMargin = (sh * 0.10f).toInt()
        root.addView(panel, plp)

        status = TextView(this)
        status.setTextColor(0xFFFFE066.toInt())
        status.textSize = 16f
        status.gravity = Gravity.CENTER
        val slp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        slp.topMargin = (sh * 0.045f).toInt()
        root.addView(status, slp)
        setContentView(root)

        okBtn.setOnClickListener { submit() }
        input.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (busy) return
                    when {
                        joining -> {
                            joining = false
                            Net.socket?.disconnect()
                            status.text = ""
                            askRoom()
                        }
                        step == 3 -> askRoom()
                        step == 2 -> askName(true)
                        else -> finish()
                    }
                }
            }
        )

        val skip = intent.getBooleanExtra("skip", false)
        if (skip) {
            role = prefs.getString("role", "host") ?: "host"
            nameV = prefs.getString("name", "") ?: ""
            if (nameV.isEmpty()) nameV = "ناشناس"
            sv.jumpToEnd()
            h.postDelayed({ askRoom() }, 300)
        } else {
            h.postDelayed({ sv.landDragons { showRoles() } }, 2000)
        }
    }

    private fun submit() {
        val v = input.text.toString().trim()
        imm.hideSoftInputFromWindow(input.windowToken, 0)
        onOk(v)
    }

    private fun showPrompt(
        label: String, preset: String, numeric: Boolean, ok: (String) -> Unit
    ) {
        onOk = ok
        status.text = ""
        prompt.text = label
        input.inputType =
            if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        input.setText(preset)
        input.setSelection(input.text.length)
        panel.alpha = 0f
        panel.visibility = View.VISIBLE
        panel.animate().alpha(1f).setDuration(500).start()
        input.requestFocus()
        h.postDelayed({ imm.showSoftInput(input, 0) }, 500)
    }

    private fun hidePanel() {
        panel.animate().alpha(0f).setDuration(300)
            .withEndAction { panel.visibility = View.GONE }.start()
    }

    private fun showRoles() {
        title.alpha = 0f
        title.visibility = View.VISIBLE
        title.animate().alpha(1f).setDuration(600).start()
        for (c in listOf(cardHost, cardView)) {
            c.alpha = 0f
            c.visibility = View.VISIBLE
            c.animate().alpha(1f).setStartDelay(300).setDuration(600).start()
        }
    }

    private fun pick(r: String) {
        role = r
        cardHost.isEnabled = false
        cardView.isEnabled = false
        for (v in listOf(title, cardHost, cardView)) {
            v.animate().alpha(0f).setDuration(300)
                .withEndAction { v.visibility = View.GONE }.start()
        }
        sv.flyOut(r == "host") {
            sv.toChase {
                sv.arrive { askName(false) }
            }
        }
    }

    private fun askName(again: Boolean) {
        step = 1
        val preset = if (nameV.isNotEmpty()) nameV else (prefs.getString("name", "") ?: "")
        showPrompt("نام شما", preset, false) { v ->
            nameV = if (v.isEmpty()) "ناشناس" else v
            hidePanel()
            if (again) {
                h.postDelayed({ askRoom() }, 350)
            } else {
                sv.panicMode()
                h.postDelayed({ askRoom() }, 1700)
            }
        }
    }

    private fun askRoom() {
        step = 2
        val preset = if (roomV.isNotEmpty()) roomV else (prefs.getString("room", "") ?: "")
        showPrompt("رمز اتاق", preset, false) { v ->
            if (v.isEmpty()) {
                status.text = "رمز اتاق را بنویس"
            } else {
                roomV = v
                if (role == "host") askMax() else doJoin()
            }
        }
    }

    private fun askMax() {
        step = 3
        showPrompt("سقف نفرات (۲ تا ۵)", prefs.getString("max", "5") ?: "5", true) { v ->
            maxV = (v.toIntOrNull() ?: 5).coerceIn(2, 5)
            doJoin()
        }
    }

    private fun fail(msg: String) {
        runOnUiThread {
            if (!joining) return@runOnUiThread
            joining = false
            Net.socket?.disconnect()
            askRoom()
            status.text = msg
        }
    }

    private fun doJoin() {
        hidePanel()
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
                    sv.openDoor {
                        startActivity(Intent(this, PlayerActivity::class.java))
                        finish()
                    }
                }
            }
        }
        s.connect()
    }
}
