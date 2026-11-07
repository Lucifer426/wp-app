package com.watchparty.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

class BgView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val hh = height.toFloat()
        t += 0.016f

        p.shader = LinearGradient(
            0f, 0f, 0f, hh,
            0xFF0B0B0F.toInt(), 0xFF1A0508.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, hh, p)

        val cx = w / 2 + (Math.sin(t * 0.8) * w * 0.3).toFloat()
        p.shader = RadialGradient(
            cx, hh * 0.3f, w * 0.9f,
            0x55D90429, 0x00000000,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, hh, p)

        p.shader = null
        p.color = 0x22FFFFFF
        canvas.drawRect(0f, 0f, 60f, hh, p)
        canvas.drawRect(w - 60f, 0f, w, hh, p)

        p.color = 0x55FFFFFF
        val n = (hh / 90f).toInt() + 2
        for (k in 0 until n) {
            val y = ((k * 90f + t * 70f) % (hh + 90f)) - 90f
            canvas.drawRoundRect(RectF(18f, y, 42f, y + 36f), 6f, 6f, p)
            canvas.drawRoundRect(
                RectF(w - 42f, y, w - 18f, y + 36f), 6f, 6f, p
            )
        }
        postInvalidateOnAnimation()
    }
}
