package com.anwesh.uiprojects.xballrotview

/**
 * Created by anweshmishra on 23/05/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.app.Activity
import android.content.Context

val nodes : Int = 5
val lines : Int = 2
val balls : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("283593")
val backColor : Int = Color.parseColor("#BDBDBD")
val rFactor : Float = 3.2f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawXRotBall(sc1 : Float, sc2 : Float, size : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        save()
        rotate(45f * sc1.divideScale(j, lines))
        drawLine(0f, -size, 0f, size, paint)
        drawBall(sc2.divideScale(j, lines), size, paint)
        restore()
    }
}

fun Canvas.drawBall(sc : Float, size : Float, paint : Paint) {
    for (j in 0..(balls - 1)) {
        save()
        translate(0f, size * (1 - 2 * j) * sc.divideScale(j, balls))
        drawCircle(0f, 0f, size / rFactor, paint)
        restore()
    }
}

fun Canvas.drawXBRNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    save()
    translate(w / 2, gap * (i + 1))
    drawXRotBall(sc1, sc2, size, paint)
    restore()
}

class XBallRotView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, balls * lines)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class XBRNode(var i : Int, val state : State = State()) {

        private var next : XBRNode? = null
        private var prev : XBRNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = XBRNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawXBRNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : XBRNode {
            var curr : XBRNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class XBallRot(var i : Int, val state : State = State()) {

        private val root : XBRNode = XBRNode(0)
        private var curr : XBRNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : XBallRotView) {

        private val animator : Animator = Animator(view)
        private val xbr : XBallRot = XBallRot(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            xbr.draw(canvas, paint)
            animator.animate {
                xbr.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            xbr.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : XBallRotView {
            val view : XBallRotView = XBallRotView(activity)
            activity.setContentView(view)
            return view
        }
    }
}