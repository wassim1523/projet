package com.example.myapplication.ui.pdf

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

    private var pendingBitmap: Bitmap? = null

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
        ensureBitmap()
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    fun loadBitmap(bitmap: Bitmap) {
        if (width <= 0 || height <= 0) {
            pendingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            return
        }

        val safeBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            .copy(Bitmap.Config.ARGB_8888, true)

        canvasBitmap = safeBitmap
        drawCanvas = Canvas(canvasBitmap!!)
        pendingBitmap = null
        drawPath.reset()
        invalidate()
    }

    fun exportBitmap(): Bitmap? {
        val src = canvasBitmap ?: return null
        return src.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun ensureBitmap() {
        if (width <= 0 || height <= 0) return

        if (canvasBitmap == null || canvasBitmap?.width != width || canvasBitmap?.height != height) {
            val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBitmap)

            canvasBitmap?.let { old ->
                val scaled = Bitmap.createScaledBitmap(old, width, height, true)
                newCanvas.drawBitmap(scaled, 0f, 0f, bitmapPaint)
            }

            canvasBitmap = newBitmap
            drawCanvas = newCanvas
        }
    }

    private fun applyPendingBitmapIfNeeded() {
        val pending = pendingBitmap ?: return
        if (width <= 0 || height <= 0) return

        val safeBitmap = Bitmap.createScaledBitmap(pending, width, height, true)
            .copy(Bitmap.Config.ARGB_8888, true)

        canvasBitmap = safeBitmap
        drawCanvas = Canvas(canvasBitmap!!)
        pendingBitmap = null
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        if (pendingBitmap != null) {
            applyPendingBitmapIfNeeded()
            return
        }

        if (canvasBitmap == null) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)
            return
        }

        if (canvasBitmap?.width != w || canvasBitmap?.height != h) {
            val oldBitmap = canvasBitmap
            val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBitmap)

            oldBitmap?.let {
                val scaled = Bitmap.createScaledBitmap(it, w, h, true)
                newCanvas.drawBitmap(scaled, 0f, 0f, bitmapPaint)
            }

            canvasBitmap = newBitmap
            drawCanvas = newCanvas
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

        ensureBitmap()

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