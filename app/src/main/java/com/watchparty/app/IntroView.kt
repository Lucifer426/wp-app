package com.watchparty.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import java.util.Random
import kotlin.math.max
import kotlin.math.sin

class IntroView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val t0 = System.nanoTime()
    private var t = 0f
    private var zoom = 1f
    private var fade = 0f
    private val rnd = Random(7)
    private val n = 70
    private val sx = FloatArray(n) { rnd.nextFloat() }
    private val sy = FloatArray(n) { rnd.nextFloat() * 0.5f }
    private val sp = FloatArray(n) { rnd.nextFloat() * 6.28f }
    private val nb = 6
    private val bph = FloatArray(nb) { rnd.nextFloat() * 6.28f }
    private val bsp = FloatArray(nb) { 0.04f + rnd.nextFloat() * 0.05f }
    private val byy = FloatArray(nb) { 0.06f + rnd.nextFloat() * 0.3f }
    private val bsz = FloatArray(nb) { 0.6f + rnd.nextFloat() * 0.9f }
    private val sky = FloatArray(14) { 0.03f + rnd.nextFloat() * 0.07f }

    fun reset() {
        zoom = 1f
        fade = 0f
    }

    fun zoomIn(done: () -> Unit) {
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 1100
        a.addUpdateListener {
            val f = it.animatedFraction
            zoom = 1f + 2.2f * f * f
            fade = max(0f, (f - 0.6f) / 0.4f)
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(an: Animator) {
                done()
            }
        })
        a.start()
    }

    private fun drawBat(
        cv: Canvas, x: Float, y: Float, s: Float, flap: Float, dir: Float
    ) {
        cv.save()
        cv.translate(x, y)
        cv.scale(dir, 1f)
        p.color = 0xFF03040A.toInt()
        for (side in 0..1) {
            cv.save()
            if (side == 1) cv.scale(-1f, 1f)
            path.reset()
            path.moveTo(0f, 0f)
            path.quadTo(s * 0.5f, -s * (0.5f + 0.5f * flap), s * 1.4f, -s * 0.8f * flap)
            path.quadTo(s * 1.0f, s * 0.05f, s * 0.8f, s * 0.15f)
            path.quadTo(s * 0.55f, s * 0.35f, s * 0.35f, s * 0.12f)
            path.quadTo(s * 0.18f, s * 0.3f, 0f, s * 0.2f)
            path.close()
            cv.drawPath(path, p)
            cv.restore()
        }
        rect.set(-s * 0.13f, -s * 0.22f, s * 0.13f, s * 0.32f)
        cv.drawOval(rect, p)
        path.reset()
        path.moveTo(-s * 0.12f, -s * 0.15f)
        path.lineTo(-s * 0.1f, -s * 0.4f)
        path.lineTo(-s * 0.02f, -s * 0.2f)
        path.close()
        path.moveTo(s * 0.12f, -s * 0.15f)
        path.lineTo(s * 0.1f, -s * 0.4f)
        path.lineTo(s * 0.02f, -s * 0.2f)
        path.close()
        cv.drawPath(path, p)
        cv.restore()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        t = (System.nanoTime() - t0) / 1e9f
        val cam = sin(t * 0.35f) * w * 0.015f
        canvas.save()
        canvas.scale(zoom, zoom, w * 0.5f, h * 0.66f)

        p.shader = LinearGradient(
            0f, 0f, 0f, h * 0.8f,
            intArrayOf(
                0xFF04060F.toInt(), 0xFF0B1330.toInt(), 0xFF1B2447.toInt()
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, p)
        p.shader = null

        for (i in 0 until n) {
            val a = (120 + 100 * sin(t * 2f + sp[i])).toInt()
            p.color = (a shl 24) or 0xFFFFFF
            canvas.drawCircle(sx[i] * w, sy[i] * h, 2.2f, p)
        }

        val mx = w * 0.78f + cam * 0.5f
        val my = h * 0.12f
        val mr = w * 0.09f
        p.shader = RadialGradient(
            mx, my, w * 0.42f, 0x66BFD4FF.toInt(), 0x00BFD4FF.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(mx, my, w * 0.42f, p)
        p.shader = null
        p.color = 0xFFEAF1FF.toInt()
        canvas.drawCircle(mx, my, mr, p)
        p.color = 0x22506080
        canvas.drawCircle(mx - mr * 0.3f, my - mr * 0.2f, mr * 0.22f, p)
        canvas.drawCircle(mx + mr * 0.35f, my + mr * 0.3f, mr * 0.15f, p)
        canvas.drawCircle(mx + mr * 0.1f, my - mr * 0.5f, mr * 0.1f, p)

        for (k in 0 until 3) {
            val cx = ((t * (0.01f + k * 0.006f) + k * 0.4f) % 1.5f - 0.25f) * w
            val cy = h * (0.08f + k * 0.07f)
            p.color = 0x16FFFFFF
            rect.set(cx - w * 0.3f, cy, cx + w * 0.3f, cy + h * 0.025f)
            canvas.drawOval(rect, p)
        }

        p.color = 0xFF080C1C.toInt()
        for (i in 0 until 14) {
            val bx = i * w / 12f - w * 0.05f + cam * 1.5f
            canvas.drawRect(bx, h * (0.26f - sky[i]), bx + w / 12f + 2f, h * 0.9f, p)
        }

        p.color = 0xFF0F0A18.toInt()
        path.reset()
        path.moveTo(w * 0.12f + cam, h * 0.30f)
        path.lineTo(-w * 0.05f, h * 0.36f)
        path.lineTo(-w * 0.05f, h * 0.93f)
        path.lineTo(w * 0.12f + cam, h * 0.86f)
        path.close()
        canvas.drawPath(path, p)
        path.reset()
        path.moveTo(w * 0.88f + cam, h * 0.30f)
        path.lineTo(w * 1.05f, h * 0.36f)
        path.lineTo(w * 1.05f, h * 0.93f)
        path.lineTo(w * 0.88f + cam, h * 0.86f)
        path.close()
        canvas.drawPath(path, p)

        val fl = w * 0.12f + cam
        val fr = w * 0.88f + cam
        p.shader = LinearGradient(
            0f, h * 0.27f, 0f, h * 0.86f,
            0xFF2A1B2E.toInt(), 0xFF150E1C.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawRect(fl, h * 0.27f, fr, h * 0.86f, p)
        p.shader = null

        p.color = 0xFF3A2540.toInt()
        canvas.drawRect(fl - w * 0.02f, h * 0.255f, fr + w * 0.02f, h * 0.285f, p)
        path.reset()
        path.moveTo(w * 0.30f + cam, h * 0.255f)
        path.lineTo(w * 0.50f + cam, h * 0.20f)
        path.lineTo(w * 0.70f + cam, h * 0.255f)
        path.close()
        canvas.drawPath(path, p)

        val bl = w * 0.2f + cam
        val br = w * 0.8f + cam
        val bt = h * 0.285f
        val bb = h * 0.37f
        p.color = 0xFF120810.toInt()
        rect.set(bl, bt, br, bb)
        canvas.drawRoundRect(rect, 14f, 14f, p)
        for (i in 0..14) {
            val bx = bl + (br - bl) * i / 14f
            val on = 0.5f + 0.5f * sin(t * 6f - i * 0.8f)
            p.color = ((60 + 195 * on).toInt() shl 24) or 0xFFD36B
            canvas.drawCircle(bx, bt + 8f, 5f, p)
            canvas.drawCircle(bx, bb - 8f, 5f, p)
        }
        val flick = ((t * 2.3f).toInt() % 6 == 4) && ((t * 2.3f) % 1f < 0.45f)
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        p.textSize = w * 0.085f
        p.color = if (flick) 0x66FF2D4D else 0xFFFF2D4D.toInt()
        p.setShadowLayer(if (flick) 4f else 28f, 0f, 0f, 0xFFFF2D4D.toInt())
        canvas.drawText(
            "فیلم پارتی", (bl + br) / 2f,
            bt + (bb - bt) * 0.5f + p.textSize * 0.32f, p
        )
        p.clearShadowLayer()

        for (cx0 in floatArrayOf(0.16f, 0.84f)) {
            val cx = w * cx0 + cam
            p.shader = LinearGradient(
                cx - w * 0.025f, 0f, cx + w * 0.025f, 0f,
                0xFF50405A.toInt(), 0xFF1E1526.toInt(), Shader.TileMode.CLAMP
            )
            canvas.drawRect(cx - w * 0.025f, h * 0.37f, cx + w * 0.025f, h * 0.84f, p)
            p.shader = null
        }

        p.color = 0xFF2B2230.toInt()
        canvas.drawRect(w * 0.14f + cam, h * 0.86f, w * 0.86f + cam, h * 0.885f, p)
        p.color = 0xFF231B29.toInt()
        canvas.drawRect(w * 0.10f + cam, h * 0.885f, w * 0.90f + cam, h * 0.915f, p)
        p.color = 0xFF1B1520.toInt()
        canvas.drawRect(0f, h * 0.915f, w, h, p)

        val l = w * 0.2f + cam
        val r = w * 0.8f + cam
        val topY = h * 0.44f
        val bot = h * 0.86f
        val rad = (r - l) / 2f
        path.reset()
        path.moveTo(l, bot)
        path.lineTo(l, topY + rad)
        rect.set(l, topY, r, topY + 2f * rad)
        path.arcTo(rect, 180f, 180f)
        path.lineTo(r, bot)
        path.close()
        p.shader = LinearGradient(
            0f, topY, 0f, bot,
            0xFFFFC46B.toInt(), 0xFF8A3A1C.toInt(), Shader.TileMode.CLAMP
        )
        p.alpha = (230 + 25 * sin(t * 9f) * sin(t * 3.1f)).toInt()
        canvas.drawPath(path, p)
        p.shader = null
        p.alpha = 255
        p.style = Paint.Style.STROKE
        p.strokeWidth = 8f
        p.color = 0xFF3A2540.toInt()
        canvas.drawPath(path, p)
        p.style = Paint.Style.FILL

        path.reset()
        path.moveTo(l, bot)
        path.lineTo(r, bot)
        path.lineTo(r + w * 0.15f, h)
        path.lineTo(l - w * 0.15f, h)
        path.close()
        p.shader = LinearGradient(
            0f, bot, 0f, h,
            0x66FFC46B.toInt(), 0x00FFC46B.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, p)
        p.shader = null

        val lx = w * 0.5f + cam
        val ly = h * 0.405f
        val pulse = 0.6f + 0.4f * sin(t * 4f)
        val lr = w * 0.14f * pulse + 10f
        p.shader = RadialGradient(
            lx, ly, lr, 0xAAFFC46B.toInt(), 0x00FFC46B.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(lx, ly, lr, p)
        p.shader = null
        p.color = 0xFFFFE2A8.toInt()
        canvas.drawCircle(lx, ly, w * 0.018f, p)

        for (side in 0..1) {
            val px = (if (side == 0) w * 0.06f else w * 0.94f) + cam * 2.5f
            p.color = 0xFF0A0710.toInt()
            canvas.drawRect(px - 6f, h * 0.56f, px + 6f, h * 0.93f, p)
            val f3 = 0.8f + 0.2f * sin(t * 7f + side * 2f)
            p.shader = RadialGradient(
                px, h * 0.55f, w * 0.2f * f3,
                0x88FFD08A.toInt(), 0x00FFD08A.toInt(), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(px, h * 0.55f, w * 0.2f * f3, p)
            p.shader = null
            p.color = 0xFFFFE9B8.toInt()
            canvas.drawCircle(px, h * 0.55f, 14f, p)
        }

        for (k in 0 until 3) {
            val fx = ((t * (0.02f + k * 0.01f) + k * 0.33f) % 1.6f - 0.3f) * w
            val fy = h * (0.90f + k * 0.025f)
            p.shader = RadialGradient(
                fx, fy, w * 0.5f, 0x33AAB4D8.toInt(), 0x00AAB4D8.toInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(fx, fy, w * 0.5f, p)
        }
        p.shader = null

        for (i in 0 until nb) {
            val prog = (t * bsp[i] + bph[i]) % 1.4f - 0.2f
            val bx = if (i % 2 == 0) prog * w else (1f - prog) * w
            val by = (byy[i] + 0.025f * sin(t * 1.7f + bph[i])) * h +
                sin(t * 3f + bph[i]) * 10f
            drawBat(
                canvas, bx, by, w * 0.05f * bsz[i],
                sin(t * 13f + bph[i]), if (i % 2 == 0) 1f else -1f
            )
        }

        canvas.restore()

        p.shader = RadialGradient(
            w / 2f, h / 2f, h * 0.75f, 0x00000000, 0xAA000000.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, p)
        p.shader = null
        if (fade > 0f) {
            p.color = ((fade * 255).toInt() shl 24)
            canvas.drawRect(0f, 0f, w, h, p)
        }
        postInvalidateOnAnimation()
    }
}
