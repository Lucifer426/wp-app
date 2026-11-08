package com.watchparty.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var scene: IntroView
    private lateinit var card: LinearLayout
    private lateinit var go: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF04060F.toInt()
        val root = FrameLayout(this)
        scene = IntroView(this)
        root.addView(
            scene,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(40, 44, 40, 44)
        val cbg = GradientDrawable()
        cbg.setColor(0xDD0B0B0F.toInt())
        cbg.setStroke(3, 0xFFD90429.toInt())
        cbg.cornerRadius = 40f
        card.background = cbg

        val tv = TextView(this)
        tv.text = "تماشای فیلم بصورت آنلاین و گروهی"
        tv.textSize = 19f
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        card.addView(tv)

        go = Button(this)
        go.text = "بزن بریم"
        go.textSize = 18f
        go.setTextColor(Color.WHITE)
        val gbg = GradientDrawable()
        gbg.setColor(0xFFD90429.toInt())
        gbg.cornerRadius = 60f
        go.background = gbg
        val glp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        glp.topMargin = 36
        card.addView(go, glp)

        val dm = resources.displayMetrics
        val clp = FrameLayout.LayoutParams(
            (dm.widthPixels * 0.54f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        clp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        clp.topMargin = (dm.heightPixels * 0.52f).toInt()
        root.addView(card, clp)
        setContentView(root)

        go.setOnClickListener {
            go.isEnabled = false
            card.animate().alpha(0f).setDuration(500).start()
            scene.zoomIn {
                startActivity(Intent(this, JoinActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scene.reset()
        card.alpha = 1f
        go.isEnabled = true
    }
}
