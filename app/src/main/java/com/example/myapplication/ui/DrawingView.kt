package com.example.myapplication.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 18f
    }

    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    private var drawPath = Path()
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null

    private var drawingEnabled = false
    private var eraserMode = false

    fun setDrawingEnabled(enabled: Boolean) {
        drawingEnabled = enabled
    }

    fun setStrokeWidth(width: Float) {
        drawPaint.strokeWidth = width.coerceAtLeast(3f)
    }

    fun setEraserMode(enabled: Boolean) {
        eraserMode = enabled
        if (eraserMode) {
            drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            drawPaint.xfermode = null
            drawPaint.color = Color.BLACK
        }
    }

    fun clearCanvas() {
        drawPath.reset()
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    fun loadBitmap(bitmap: Bitmap) {
        if (width <= 0 || height <= 0) return
        canvasBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true).copy(Bitmap.Config.ARGB_8888, true)
        drawCanvas = Canvas(canvasBitmap!!)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        if (canvasBitmap == null) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)
            return
        }

        if (canvasBitmap?.width != w || canvasBitmap?.height != h) {
            val oldBitmap = canvasBitmap
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)
            oldBitmap?.let {
                val scaled = Bitmap.createScaledBitmap(it, w, h, true)
                drawCanvas?.drawBitmap(scaled, 0f, 0f, bitmapPaint)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
        }
        canvas.drawPath(drawPath, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath = Path()
                drawPath.moveTo(x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(x, y)
            }

            MotionEvent.ACTION_UP -> {
                drawPath.lineTo(x, y)
                drawCanvas?.drawPath(drawPath, drawPaint)
                drawPath = Path()
            }

            else -> return false
        }

        invalidate()
        return true
    }
}