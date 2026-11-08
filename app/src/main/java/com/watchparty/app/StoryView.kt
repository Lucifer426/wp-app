package com.watchparty.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class StoryView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val t0 = System.nanoTime()
    private var t = 0f
    private val rnd = Random(11)
    private val label = "عاقبت فرار از مدرسه"

    private var scene = 0
    private var land = 0f
    private var out0 = 0f
    private var out1 = 0f
    private var veil = 0f
    private var approach = 0f
    private var danger = 0.25f
    private var panic = 0f
    private var doorOpen = 0f
    private var carIn = 0f
    private var flash = 0f

    private val cn = 240
    private val crX = FloatArray(cn) { rnd.nextFloat() }
    private val crR = FloatArray(cn) { rnd.nextFloat() }
    private val crP = FloatArray(cn) { rnd.nextFloat() * 6.28f }
    private val palette = intArrayOf(
        0xFFE8C9A0.toInt(), 0xFFC9573A.toInt(), 0xFF3E6FA3.toInt(),
        0xFFD9B44A.toInt(), 0xFF7A4B8C.toInt(), 0xFFEFEFEF.toInt()
    )
    private val crC = IntArray(cn) { palette[rnd.nextInt(palette.size)] }
    private val ns = 70
    private val stX = FloatArray(ns) { rnd.nextFloat() }
    private val stY = FloatArray(ns) { rnd.nextFloat() * 0.55f }
    private val stP = FloatArray(ns) { rnd.nextFloat() * 6.28f }

    private fun anim(
        ms: Long, ip: TimeInterpolator?,
        upd: (Float) -> Unit, end: () -> Unit
    ) {
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = ms
        if (ip != null) a.interpolator = ip
        a.addUpdateListener { upd(it.animatedValue as Float) }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(an: Animator) {
                end()
            }
        })
        a.start()
    }

    fun landDragons(done: () -> Unit) {
        anim(1900, DecelerateInterpolator(), { land = it }, done)
    }

    fun flyOut(right: Boolean, done: () -> Unit) {
        anim(
            1600, AccelerateInterpolator(1.3f),
            { if (right) out1 = it else out0 = it }, done
        )
    }

    fun toChase(done: () -> Unit) {
        anim(700, null, { veil = it }) {
            scene = 1
            approach = 0f
            anim(700, null, { veil = 1f - it }) { }
            done()
        }
    }

    fun arrive(done: () -> Unit) {
        anim(3800, AccelerateDecelerateInterpolator(), { approach = it }, done)
    }

    fun panicMode() {
        anim(1400, null, { panic = it; danger = 0.25f + 0.5f * it }) { }
    }

    fun openDoor(done: () -> Unit) {
        anim(2400, null, { doorOpen = it }) {
            anim(1500, AccelerateInterpolator(), { carIn = it }) {
                anim(450, null, { flash = it }, done)
            }
        }
    }

    private fun rad(d: Float) = d * 0.017453292f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        t = (System.nanoTime() - t0) / 1e9f
        if (scene == 0) drawArena(canvas, w, h) else drawChase(canvas, w, h)
        p.shader = null
        if (veil > 0f) {
            p.color = ((veil * 255).toInt() shl 24)
            canvas.drawRect(0f, 0f, w, h, p)
        }
        if (flash > 0f) {
            p.color = ((flash * 255).toInt() shl 24) or 0xFFF2D0
            canvas.drawRect(0f, 0f, w, h, p)
        }
        postInvalidateOnAnimation()
    }

    private fun drawArena(cv: Canvas, w: Float, h: Float) {
        val z = 1f + 0.05f * min(t / 8f, 1f)
        cv.save()
        cv.scale(z, z, w / 2f, h * 0.62f)
        p.shader = LinearGradient(
            0f, 0f, 0f, h * 0.3f, 0xFF2A1840.toInt(), 0xFFE0773C.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, 0f, w, h * 0.3f, p)
        p.shader = RadialGradient(
            w * 0.5f, h * 0.2f, w * 0.7f, 0x99FFC46B.toInt(),
            0x00FFC46B.toInt(), Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, 0f, w, h * 0.4f, p)
        p.shader = LinearGradient(
            0f, h * 0.17f, 0f, h * 0.58f, 0xFF6A5650.toInt(),
            0xFF2A2025.toInt(), Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, h * 0.17f, w, h * 0.58f, p)
        p.shader = null
        p.color = 0xFF1B1418.toInt()
        val an = 8
        for (i in 0 until an) {
            val ax = (i + 0.5f) * w / an
            rect.set(ax - w * 0.04f, h * 0.18f, ax + w * 0.04f, h * 0.235f)
            cv.drawRoundRect(rect, w * 0.04f, w * 0.04f, p)
        }
        p.color = 0x44000000
        for (r in 0..5) {
            val ly = h * (0.25f + r * 0.055f)
            cv.drawRect(0f, ly, w, ly + 3f, p)
        }
        for (i in 0 until cn) {
            val rr = crR[i]
            val sz = w * (0.012f + 0.018f * rr)
            val y = h * (0.26f + 0.29f * rr)
            val x = crX[i] * w
            val cheer = 0.5f + 0.5f * sin(t * 1.6f + crX[i] * 7f)
            val bob = sin(t * 5f + crP[i]) * sz * 0.5f * cheer
            p.color = crC[i]
            cv.drawCircle(x, y + bob, sz, p)
            rect.set(x - sz * 1.1f, y + bob + sz, x + sz * 1.1f, y + bob + sz * 3f)
            cv.drawRoundRect(rect, sz * 0.5f, sz * 0.5f, p)
            if (i % 4 == 0) {
                p.strokeWidth = sz * 0.5f
                p.style = Paint.Style.STROKE
                cv.drawLine(
                    x + sz, y + bob + sz * 1.4f,
                    x + sz * 1.6f, y + bob - sz * (1.2f + cheer), p
                )
                p.style = Paint.Style.FILL
            }
        }
        p.shader = LinearGradient(
            0f, h * 0.55f, 0f, h * 0.6f, 0xFF4B3C3A.toInt(),
            0xFF241B1E.toInt(), Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, h * 0.55f, w, h * 0.6f, p)
        p.shader = null
        for (i in 0..4) {
            val tx = w * (0.1f + 0.2f * i)
            val f = 0.8f + 0.2f * sin(t * 9f + i * 2f)
            p.shader = RadialGradient(
                tx, h * 0.54f, w * 0.12f * f, 0xAAFFB04A.toInt(),
                0x00FFB04A.toInt(), Shader.TileMode.CLAMP
            )
            cv.drawCircle(tx, h * 0.54f, w * 0.12f * f, p)
            p.shader = null
            p.color = 0xFFFFD27A.toInt()
            cv.drawCircle(tx, h * 0.545f, 10f, p)
        }
        p.shader = LinearGradient(
            0f, h * 0.6f, 0f, h, 0xFFD9B27C.toInt(), 0xFF8F6A3C.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, h * 0.6f, w, h, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = 0x33000000
        rect.set(-w * 0.3f, h * 0.64f, w * 1.3f, h * 1.3f)
        cv.drawOval(rect, p)
        rect.set(-w * 0.1f, h * 0.72f, w * 1.1f, h * 1.15f)
        cv.drawOval(rect, p)
        p.style = Paint.Style.FILL

        val gy = h * 0.82f
        val ds = w * 0.105f
        for (side in 0..1) {
            val out = if (side == 0) out0 else out1
            val homeX = if (side == 0) w * 0.27f else w * 0.73f
            val outDir = if (side == 0) -1f else 1f
            val faceIn = if (side == 0) 1f else -1f
            val dir = if (out > 0.04f) outDir else faceIn
            val startY = -h * 0.3f
            val y = startY + (gy - startY) * land - out * h * 1.1f
            val x = homeX + outDir * out * w * 0.9f
            val s = ds * (1f - 0.55f * out)
            if (out < 0.6f) {
                val al = 0.35f * land * (1f - out)
                p.color = ((al * 255).toInt() shl 24)
                rect.set(homeX - ds * 1.8f, gy + ds * 0.7f, homeX + ds * 1.8f, gy + ds * 1.0f)
                cv.drawOval(rect, p)
            }
            val settle = (1f - land) * (1f - land)
            val flying = out > 0.02f
            val amp = if (flying) 1f else 0.35f + 0.65f * settle
            val speed = if (flying) 14f else 3.2f + 11f * settle
            val f = sin(t * speed + side) * amp
            if (side == 0) {
                drawDragon(
                    cv, x, y, s, dir, f, 0xFF2F8F83.toInt(),
                    0xFF1F5F58.toInt(), 0xFF3FB0A0.toInt(), 0xFF8FA8C8.toInt()
                )
            } else {
                drawDragon(
                    cv, x, y, s, dir, f, 0xFFB5302B.toInt(),
                    0xFF7A1F1C.toInt(), 0xFFD94A3F.toInt(), 0xFFD4A017.toInt()
                )
            }
        }
        cv.restore()
    }

    private fun wing(cv: Canvas, f: Float, col: Int) {
        val tx = -0.9f
        val ty = -1.9f - 1.3f * f
        path.reset()
        path.moveTo(0.1f, -0.45f)
        path.quadTo(-0.3f, -1.4f - 0.8f * f, tx, ty)
        path.quadTo(tx - 0.35f, ty + 0.6f, -1.5f, -1.1f - 0.7f * f)
        path.quadTo(-1.4f, -0.9f - 0.5f * f, -1.4f, -0.6f - 0.3f * f)
        path.quadTo(-1.1f, -0.5f, -0.8f, -0.35f)
        path.close()
        p.color = col
        cv.drawPath(path, p)
        p.color = 0x55000000
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.04f
        cv.drawLine(0.1f, -0.45f, tx, ty, p)
        cv.drawLine(0.1f, -0.45f, -1.5f, -1.1f - 0.7f * f, p)
        p.style = Paint.Style.FILL
    }

    private fun drawDragon(
        cv: Canvas, x: Float, y: Float, s: Float, dir: Float, f: Float,
        body: Int, wingFar: Int, wingNear: Int, armor: Int
    ) {
        val br = 1f + 0.04f * sin(t * 2.2f)
        cv.save()
        cv.translate(x, y)
        cv.scale(dir * s, s)
        wing(cv, -f * 0.9f, wingFar)
        val tw = 0.12f * sin(t * 3f)
        path.reset()
        path.moveTo(-0.9f, -0.2f)
        path.quadTo(-1.7f, 0.25f, -2.1f, -0.3f + tw)
        path.lineTo(-2.45f, -0.55f + tw)
        path.quadTo(-1.7f, 0.6f, -0.8f, 0.35f)
        path.close()
        p.color = body
        cv.drawPath(path, p)
        rect.set(-0.65f, 0.25f, -0.35f, 0.8f)
        cv.drawRoundRect(rect, 0.1f, 0.1f, p)
        rect.set(0.3f, 0.25f, 0.6f, 0.8f)
        cv.drawRoundRect(rect, 0.1f, 0.1f, p)
        rect.set(-1.05f, -0.55f * br, 0.95f, 0.45f)
        cv.drawOval(rect, p)
        p.color = 0x66FFE9B8
        rect.set(-0.8f, 0.0f, 0.7f, 0.42f)
        cv.drawOval(rect, p)
        p.color = body
        path.reset()
        path.moveTo(0.55f, -0.45f)
        path.lineTo(1.0f, -1.3f)
        path.lineTo(1.45f, -1.15f)
        path.lineTo(1.0f, -0.05f)
        path.close()
        cv.drawPath(path, p)
        rect.set(1.05f, -1.5f, 1.95f, -0.95f)
        cv.drawOval(rect, p)
        p.color = 0xFFEFE3C2.toInt()
        path.reset()
        path.moveTo(1.2f, -1.42f)
        path.lineTo(0.85f, -1.9f)
        path.lineTo(1.5f, -1.48f)
        path.close()
        cv.drawPath(path, p)
        for (i in 0..5) {
            val sx = 0.7f - i * 0.3f
            path.reset()
            path.moveTo(sx - 0.07f, -0.52f)
            path.lineTo(sx, -0.72f)
            path.lineTo(sx + 0.07f, -0.52f)
            path.close()
            cv.drawPath(path, p)
        }
        p.color = 0xFFFFD84A.toInt()
        cv.drawCircle(1.6f, -1.28f, 0.08f, p)
        p.color = 0xFF000000.toInt()
        cv.drawCircle(1.62f, -1.28f, 0.035f, p)
        for (k in 0..3) {
            val ph = (t * 0.7f + k * 0.25f) % 1f
            p.color = (((1f - ph) * 0.45f * 255).toInt() shl 24) or 0xBBBBBB
            cv.drawCircle(1.95f + ph * 0.6f, -1.1f - ph * 0.8f, 0.05f + 0.14f * ph, p)
        }
        wing(cv, f, wingNear)

        val wx = 0.15f
        val wy = -0.55f
        path.reset()
        path.moveTo(wx - 0.05f, wy - 0.6f)
        path.lineTo(wx - 0.75f - 0.1f * sin(t * 8f), wy - 0.2f + 0.1f * sin(t * 7f))
        path.lineTo(wx - 0.05f, wy - 0.1f)
        path.close()
        p.color = 0xFFB5102B.toInt()
        cv.drawPath(path, p)
        p.color = armor
        rect.set(wx - 0.13f, wy - 0.65f, wx + 0.13f, wy)
        cv.drawRoundRect(rect, 0.05f, 0.05f, p)
        p.color = 0xFFE0B48C.toInt()
        cv.drawCircle(wx, wy - 0.8f, 0.14f, p)
        p.color = 0xFFC0C6CE.toInt()
        rect.set(wx - 0.16f, wy - 0.97f, wx + 0.16f, wy - 0.8f)
        cv.drawRoundRect(rect, 0.1f, 0.1f, p)
        path.reset()
        path.moveTo(wx, wy - 0.97f)
        path.quadTo(wx - 0.2f, wy - 1.2f - 0.05f * sin(t * 6f), wx - 0.4f, wy - 1.0f)
        path.lineTo(wx - 0.05f, wy - 0.93f)
        path.close()
        p.color = 0xFFD90429.toInt()
        cv.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.05f
        p.color = 0xFF8A6A3A.toInt()
        cv.drawLine(wx + 0.2f, wy - 0.1f, wx + 0.58f, wy - 1.5f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFDDE3EA.toInt()
        path.reset()
        path.moveTo(wx + 0.6f, wy - 1.72f)
        path.lineTo(wx + 0.52f, wy - 1.45f)
        path.lineTo(wx + 0.67f, wy - 1.47f)
        path.close()
        cv.drawPath(path, p)
        p.color = 0xFFD4A017.toInt()
        cv.drawCircle(wx + 0.02f, wy - 0.35f, 0.2f, p)
        p.color = 0xFF5A3A08.toInt()
        cv.drawCircle(wx + 0.02f, wy - 0.35f, 0.07f, p)
        cv.restore()
    }

    private fun drawCar(
        cv: Canvas, cx: Float, cy: Float, wd: Float,
        kind: Int, text: Boolean, tilt: Float
    ) {
        cv.save()
        cv.rotate(tilt, cx, cy)
        val fl = 0.75f + 0.25f * sin(t * 28f + kind * 2f + cx)
        p.shader = RadialGradient(
            cx, cy + wd * 0.25f, wd * 0.65f, 0x99FF7A2B.toInt(),
            0x00FF7A2B.toInt(), Shader.TileMode.CLAMP
        )
        cv.drawCircle(cx, cy + wd * 0.25f, wd * 0.65f, p)
        p.shader = null
        for (sd in intArrayOf(-1, 1)) {
            val jx = cx + sd * wd * 0.33f
            path.reset()
            path.moveTo(jx - wd * 0.06f, cy + wd * 0.12f)
            path.lineTo(jx + wd * 0.06f, cy + wd * 0.12f)
            path.lineTo(jx, cy + wd * (0.12f + 0.26f * fl))
            path.close()
            p.color = 0xFFFF8A2B.toInt()
            cv.drawPath(path, p)
            path.reset()
            path.moveTo(jx - wd * 0.03f, cy + wd * 0.12f)
            path.lineTo(jx + wd * 0.03f, cy + wd * 0.12f)
            path.lineTo(jx, cy + wd * (0.12f + 0.15f * fl))
            path.close()
            p.color = 0xFFFFE08A.toInt()
            cv.drawPath(path, p)
            p.color = 0xFF0E0E12.toInt()
            rect.set(
                cx + sd * wd * 0.40f - wd * 0.07f, cy - wd * 0.04f,
                cx + sd * wd * 0.40f + wd * 0.07f, cy + wd * 0.15f
            )
            cv.drawRoundRect(rect, wd * 0.03f, wd * 0.03f, p)
        }
        val body = if (kind == 0) 0xFF6FA8DC.toInt() else 0xFF3A3F4B.toInt()
        val dark = if (kind == 0) 0xFF4F86B8.toInt() else 0xFF262A33.toInt()
        val c0 = if (kind == 0) 0.44f else 0.40f
        val c1 = if (kind == 0) 0.35f else 0.30f
        val top = if (kind == 0) 0.47f else 0.40f
        p.color = dark
        path.reset()
        path.moveTo(cx - c0 * wd, cy - 0.2f * wd)
        path.lineTo(cx - c1 * wd, cy - top * wd)
        path.lineTo(cx + c1 * wd, cy - top * wd)
        path.lineTo(cx + c0 * wd, cy - 0.2f * wd)
        path.close()
        cv.drawPath(path, p)
        p.color = body
        rect.set(cx - 0.5f * wd, cy - 0.22f * wd, cx + 0.5f * wd, cy + 0.10f * wd)
        cv.drawRoundRect(rect, 0.06f * wd, 0.06f * wd, p)
        p.color = dark
        rect.set(cx - 0.5f * wd, cy + 0.04f * wd, cx + 0.5f * wd, cy + 0.12f * wd)
        cv.drawRoundRect(rect, 0.03f * wd, 0.03f * wd, p)
        val wy0 = cy - 0.215f * wd
        val wy1 = cy - (top - 0.03f) * wd
        val a0 = if (kind == 0) 0.40f else 0.36f
        val a1 = if (kind == 0) 0.32f else 0.28f
        path.reset()
        path.moveTo(cx - a0 * wd, wy0)
        path.lineTo(cx - a1 * wd, wy1)
        path.lineTo(cx + a1 * wd, wy1)
        path.lineTo(cx + a0 * wd, wy0)
        path.close()
        p.color = 0xFF16202B.toInt()
        cv.drawPath(path, p)
        if (text) {
            p.typeface = Typeface.DEFAULT_BOLD
            p.textAlign = Paint.Align.CENTER
            p.textSize = 100f
            val tw = p.measureText(label)
            p.textSize = 100f * (wd * 0.56f) / tw
            p.color = 0xFFFFE066.toInt()
            cv.drawText(label, cx, (wy0 + wy1) / 2f + p.textSize * 0.28f, p)
            p.typeface = Typeface.DEFAULT
        }
        for (sd in intArrayOf(-1, 1)) {
            val lx = cx + sd * wd * 0.41f
            val ly = cy - 0.14f * wd
            val gl = wd * (0.13f + 0.05f * panic)
            p.shader = RadialGradient(
                lx, ly, gl, 0xAAFF1E2D.toInt(), 0x00FF1E2D.toInt(),
                Shader.TileMode.CLAMP
            )
            cv.drawCircle(lx, ly, gl, p)
            p.shader = null
            p.color = 0xFFFF3040.toInt()
            rect.set(lx - wd * 0.08f, ly - wd * 0.03f, lx + wd * 0.08f, ly + wd * 0.03f)
            cv.drawRoundRect(rect, wd * 0.015f, wd * 0.015f, p)
        }
        p.color = 0xFFF4F4F4.toInt()
        rect.set(cx - 0.09f * wd, cy - 0.05f * wd, cx + 0.09f * wd, cy + 0.01f * wd)
        cv.drawRect(rect, p)
        cv.restore()
    }

    private fun drawCastle(cv: Canvas, w: Float, h: Float, gy: Float, k: Float) {
        cv.save()
        cv.translate(w / 2f, gy)
        cv.scale(k, k)
        p.shader = LinearGradient(
            0f, -0.5f * h, 0f, 0f, 0xFF232B4A.toInt(), 0xFF10142A.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(-0.9f * w, -0.5f * h, 0.9f * w, 0f, p)
        p.shader = null
        p.color = 0xFF232B4A.toInt()
        for (i in -8..8) {
            cv.drawRect(
                i * 0.1f * w - 0.03f * w, -0.54f * h,
                i * 0.1f * w + 0.03f * w, -0.5f * h, p
            )
        }
        p.color = 0xFF1A2040.toInt()
        cv.drawRect(-0.28f * w, -0.78f * h, 0.28f * w, -0.5f * h, p)
        path.reset()
        path.moveTo(-0.32f * w, -0.78f * h)
        path.lineTo(0f, -1.0f * h)
        path.lineTo(0.32f * w, -0.78f * h)
        path.close()
        p.color = 0xFF2A1633.toInt()
        cv.drawPath(path, p)
        val wf = 0.7f + 0.3f * sin(t * 6f)
        val winCol = ((wf * 255).toInt() shl 24) or 0xFFC46B
        for (sd in intArrayOf(-1, 1)) {
            val tx = sd * 0.68f * w
            p.color = 0xFF1A2040.toInt()
            cv.drawRect(tx - 0.14f * w, -0.88f * h, tx + 0.14f * w, 0f, p)
            path.reset()
            path.moveTo(tx - 0.18f * w, -0.88f * h)
            path.lineTo(tx, -1.12f * h)
            path.lineTo(tx + 0.18f * w, -0.88f * h)
            path.close()
            p.color = 0xFF2A1633.toInt()
            cv.drawPath(path, p)
            p.color = winCol
            cv.drawRect(tx - 0.025f * w, -0.7f * h, tx + 0.025f * w, -0.62f * h, p)
            cv.drawRect(sd * 0.1f * w - 0.02f * w, -0.7f * h, sd * 0.1f * w + 0.02f * w, -0.62f * h, p)
        }

        val gw = 0.23f * w
        val gh = 0.40f * h
        path.reset()
        path.moveTo(-gw, 0f)
        path.lineTo(-gw, -gh)
        path.quadTo(-gw, -0.52f * h, 0f, -0.56f * h)
        path.quadTo(gw, -0.52f * h, gw, -gh)
        path.lineTo(gw, 0f)
        path.close()
        val arch = Path(path)
        cv.save()
        cv.clipPath(arch)
        p.color = 0xFF07060C.toInt()
        cv.drawRect(-gw, -0.6f * h, gw, 0f, p)
        val o = doorOpen
        if (o > 0f) {
            p.shader = RadialGradient(
                0f, -0.25f * h, 0.55f * h, 0xFFFFF2CC.toInt(),
                0xFFFF9A3C.toInt(), Shader.TileMode.CLAMP
            )
            p.alpha = (o * 255).toInt()
            cv.drawRect(-gw, -0.6f * h, gw, 0f, p)
            p.shader = null
            p.alpha = 255
            for (r in 0..6) {
                val ang = -90f + (r - 3) * 18f + 5f * sin(t + r)
                val a2 = ang + 4f
                path.reset()
                path.moveTo(0f, -0.25f * h)
                path.lineTo(cos(rad(ang)) * 2f * h, -0.25f * h + sin(rad(ang)) * 2f * h)
                path.lineTo(cos(rad(a2)) * 2f * h, -0.25f * h + sin(rad(a2)) * 2f * h)
                path.close()
                p.color = (((0.22f * o) * 255).toInt() shl 24) or 0xFFF2CC
                cv.drawPath(path, p)
            }
        }
        val lw = gw * (1f - o)
        val lwx = max(lw, 1f)
        p.shader = LinearGradient(
            -gw, 0f, -gw + lwx, 0f, 0xFF24150F.toInt(), 0xFF3A2218.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(-gw, -0.6f * h, -gw + lw, 0f, p)
        p.shader = LinearGradient(
            gw - lwx, 0f, gw, 0f, 0xFF3A2218.toInt(), 0xFF24150F.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(gw - lw, -0.6f * h, gw, 0f, p)
        p.shader = null
        p.color = 0xFF14100E.toInt()
        for (b in 1..4) {
            val by = -b * 0.1f * h
            cv.drawRect(-gw, by, -gw + lw, by + 0.012f * h, p)
            cv.drawRect(gw - lw, by, gw, by + 0.012f * h, p)
        }
        if (o < 0.9f) {
            val glow = 0.6f + 0.4f * sin(t * 3f)
            val rc = ((glow * 255).toInt() shl 24) or 0x8F5BFF
            p.style = Paint.Style.STROKE
            p.strokeWidth = 7f
            p.color = rc
            for (side in intArrayOf(-1, 1)) {
                val rx = side * (gw - lw / 2f)
                cv.save()
                cv.translate(rx, -0.3f * h)
                cv.scale(1f - o, 1f)
                cv.drawCircle(0f, 0f, gw * 0.38f, p)
                cv.drawCircle(0f, 0f, gw * 0.22f, p)
                path.reset()
                path.moveTo(0f, -gw * 0.32f)
                path.lineTo(-gw * 0.28f, gw * 0.2f)
                path.lineTo(gw * 0.28f, gw * 0.2f)
                path.close()
                cv.drawPath(path, p)
                cv.restore()
            }
            if (o < 0.1f) {
                p.strokeWidth = 5f
                cv.drawLine(0f, -0.55f * h, 0f, 0f, p)
            }
            p.style = Paint.Style.FILL
        }
        cv.restore()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 18f
        p.color = 0xFF4A4256.toInt()
        cv.drawPath(arch, p)
        p.style = Paint.Style.FILL
        p.color = 0xFF1A1A28.toInt()
        cv.drawRect(-gw * 1.25f, 0f, gw * 1.25f, 0.02f * h, p)

        for (sd in intArrayOf(-1, 1)) {
            val tx = sd * (gw + 0.09f * w)
            val ty = -0.32f * h
            val f = 0.8f + 0.2f * sin(t * 8f + sd * 3f)
            p.shader = RadialGradient(
                tx, ty, 0.22f * w * f, 0xAAFFB04A.toInt(), 0x00FFB04A.toInt(),
                Shader.TileMode.CLAMP
            )
            cv.drawCircle(tx, ty, 0.22f * w * f, p)
            p.shader = null
            p.color = 0xFF3A2A20.toInt()
            cv.drawRect(tx - 6f, ty, tx + 6f, ty + 0.1f * h, p)
            p.color = 0xFFFFD27A.toInt()
            rect.set(tx - 0.02f * w, ty - 0.05f * h * f, tx + 0.02f * w, ty + 0.01f * h)
            cv.drawOval(rect, p)
        }
        p.color = 0x22B4C0E8
        for (m in 0..1) {
            val mx = sin(t * 0.5f + m) * 0.1f * w
            rect.set(-0.8f * w + mx, -0.05f * h + m * 0.02f * h, 0.8f * w + mx, 0.04f * h)
            cv.drawOval(rect, p)
        }
        cv.restore()
    }

    private fun drawChase(cv: Canvas, w: Float, h: Float) {
        val shake = if (panic > 0.3f) sin(t * 45f) * 7f * panic else 0f
        cv.save()
        cv.translate(shake, shake * 0.5f)
        p.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF03050E.toInt(), 0xFF0A1230.toInt(), 0xFF1E2A55.toInt()),
            null, Shader.TileMode.CLAMP
        )
        cv.drawRect(-20f, -20f, w + 20f, h + 20f, p)
        p.shader = null
        for (i in 0 until ns) {
            val a = (110 + 100 * sin(t * 2f + stP[i])).toInt()
            p.color = (a shl 24) or 0xFFFFFF
            cv.drawCircle(stX[i] * w, stY[i] * h, 2.2f, p)
        }
        val mx = w * 0.22f
        val my = h * 0.13f
        p.shader = RadialGradient(
            mx, my, w * 0.45f, 0x66BFD4FF.toInt(), 0x00BFD4FF.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawCircle(mx, my, w * 0.45f, p)
        p.shader = null
        p.color = 0xFFEAF1FF.toInt()
        cv.drawCircle(mx, my, w * 0.085f, p)
        p.color = 0x22506080
        cv.drawCircle(mx - w * 0.025f, my - w * 0.015f, w * 0.02f, p)

        val gy = h * (0.58f + 0.08f * approach)
        p.shader = LinearGradient(
            0f, gy, 0f, h, 0xFF0C1022.toInt(), 0xFF05060C.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(-20f, gy, w + 20f, h + 20f, p)
        p.shader = null
        val k = 0.16f + 0.84f * approach * approach
        drawCastle(cv, w, h, gy, k)

        for (i in 0 until 8) {
            val ph = (t * (0.3f + i * 0.04f) + i * 0.17f) % 1f
            val fy = ph * h * 1.1f
            val al = (sin(ph * 3.1416f) * 48).toInt()
            p.color = (al shl 24) or 0xB4C0E8
            rect.set(-w * 0.2f + (i % 3) * w * 0.3f, fy, w * 0.6f + (i % 3) * w * 0.3f, fy + h * 0.05f)
            cv.drawOval(rect, p)
        }

        val d = danger
        drawCar(
            cv, w * 0.5f + sin(t * 2.1f) * w * 0.02f, h * (1.16f - 0.1f * d),
            w * (0.70f + 0.1f * d), 1, false, sin(t * 1.7f) * 2f
        )
        val pw = w * 0.60f * (1f - 0.72f * carIn)
        val px = w * 0.5f + sin(t * 1.3f) * w * 0.012f
        val py = h * 0.80f + sin(t * 3f) * h * 0.008f - carIn * h * 0.30f
        drawCar(cv, px, py, pw, 0, true, sin(t * 1.5f) * 3f + panic * sin(t * 30f) * 2f)
        for (sd in intArrayOf(-1, 1)) {
            drawCar(
                cv,
                w * (0.5f + sd * (0.36f - 0.07f * d)) + sin(t * 2f + sd) * w * 0.01f,
                h * (1.02f - 0.13f * d) + sin(t * 3.3f + sd) * h * 0.008f,
                w * (0.46f + 0.2f * d), 1, false, sd * (4f + 4f * d)
            )
        }
        if (panic > 0.3f && carIn < 0.2f) {
            val bx = px + pw * 0.34f
            val by = py - pw * 0.62f
            p.color = 0xFFFFD84A.toInt()
            cv.drawCircle(bx, by, pw * 0.07f, p)
            p.color = 0xFF3A1010.toInt()
            p.typeface = Typeface.DEFAULT_BOLD
            p.textAlign = Paint.Align.CENTER
            p.textSize = pw * 0.11f
            cv.drawText("!", bx, by + pw * 0.04f, p)
            p.typeface = Typeface.DEFAULT
        }
        cv.restore()
        p.shader = RadialGradient(
            w / 2f, h / 2f, h * 0.75f, 0x00000000, 0xAA000000.toInt(),
            Shader.TileMode.CLAMP
        )
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }
}
