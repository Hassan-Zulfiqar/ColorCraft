package com.hassan.colorcraft.ui.coloring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import com.hassan.colorcraft.util.FloodFillEngine
import kotlin.math.hypot
import kotlin.math.min

class ColoringCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var firstLoadedBitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private var matrix = Matrix()
    private var fitScale = 1f
    private var isZoomedIn = false
    private var fillColor = Color.BLACK

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val floodFillEngine = FloodFillEngine()

    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var hadMultiTouch = false

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val values = FloatArray(9)
                matrix.getValues(values)
                val currentScale = values[Matrix.MSCALE_X]
                val desiredScale = currentScale * detector.scaleFactor
                val clampedScale = desiredScale.coerceIn(fitScale, fitScale * MAX_SCALE_MULTIPLIER)
                val appliedFactor = clampedScale / currentScale

                matrix.postScale(appliedFactor, appliedFactor, detector.focusX, detector.focusY)
                clampMatrix()
                invalidate()
                return true
            }
        }
    )

    var onUndoRedoStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyFitToScreenMatrix()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        canvas.drawBitmap(bmp, matrix, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = System.currentTimeMillis()
                lastX = event.x
                lastY = event.y
                isDragging = false
                hadMultiTouch = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                hadMultiTouch = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !hadMultiTouch) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    val totalDistance = hypot(event.x - downX, event.y - downY)
                    if (isDragging || totalDistance > touchSlop) {
                        isDragging = true
                        matrix.postTranslate(dx, dy)
                        clampMatrix()
                        invalidate()
                    }
                }
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging && !hadMultiTouch) {
                    handleTap(event.x, event.y)
                }
                isDragging = false
                hadMultiTouch = false
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                hadMultiTouch = false
            }
        }
        return true
    }

    fun loadBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        this.firstLoadedBitmap = bitmap
        undoStack.clear()
        redoStack.clear()
        isZoomedIn = false
        if (width > 0 && height > 0) {
            applyFitToScreenMatrix()
        } else {
            matrix = Matrix()
        }
        notifyUndoRedoState()
        invalidate()
    }

    fun setFillColor(color: Int) {
        fillColor = color
    }

    fun setOriginalBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
    }

    fun undo(): Boolean {
        val previous = undoStack.removeLastOrNull() ?: return false
        bitmap?.let { redoStack.addLast(it) }
        bitmap = previous
        notifyUndoRedoState()
        invalidate()
        return true
    }

    fun redo(): Boolean {
        val next = redoStack.removeLastOrNull() ?: return false
        bitmap?.let { undoStack.addLast(it) }
        bitmap = next
        notifyUndoRedoState()
        invalidate()
        return true
    }

    fun resetToOriginal() {
        val original = originalBitmap ?: return
        undoStack.clear()
        redoStack.clear()
        bitmap = original.copy(original.config ?: Bitmap.Config.ARGB_8888, true)
        notifyUndoRedoState()
        invalidate()
    }

    fun toggleZoom() {
        if (bitmap == null) return
        if (width <= 0 || height <= 0) return

        val viewCenterX = width / 2f
        val viewCenterY = height / 2f

        val inverse = Matrix()
        if (!matrix.invert(inverse)) return
        val focalBitmapPoint = FloatArray(2)
        inverse.mapPoints(focalBitmapPoint, floatArrayOf(viewCenterX, viewCenterY))

        val targetScale = if (isZoomedIn) fitScale else FIXED_ZOOM_SCALE
        isZoomedIn = !isZoomedIn

        val newMatrix = Matrix()
        newMatrix.postScale(targetScale, targetScale)
        val mappedFocal = FloatArray(2)
        newMatrix.mapPoints(mappedFocal, focalBitmapPoint)
        newMatrix.postTranslate(viewCenterX - mappedFocal[0], viewCenterY - mappedFocal[1])

        matrix = newMatrix
        clampMatrix()
        invalidate()
    }

    fun getCurrentBitmap(): Bitmap? = bitmap

    private fun applyFitToScreenMatrix() {
        val bmp = bitmap ?: return
        if (width <= 0 || height <= 0 || bmp.width <= 0 || bmp.height <= 0) return

        val scale = min(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        fitScale = scale
        val scaledWidth = bmp.width * scale
        val scaledHeight = bmp.height * scale
        val dx = (width - scaledWidth) / 2f
        val dy = (height - scaledHeight) / 2f

        val newMatrix = Matrix()
        newMatrix.postScale(scale, scale)
        newMatrix.postTranslate(dx, dy)
        matrix = newMatrix
        isZoomedIn = false
        invalidate()
    }

    private fun clampMatrix() {
        val bmp = bitmap ?: return
        if (width <= 0 || height <= 0) return

        val values = FloatArray(9)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val scaledWidth = bmp.width * scaleX
        val scaledHeight = bmp.height * scaleY

        val minTx: Float
        val maxTx: Float
        if (scaledWidth <= width) {
            minTx = 0f
            maxTx = width - scaledWidth
        } else {
            minTx = width - scaledWidth
            maxTx = 0f
        }

        val minTy: Float
        val maxTy: Float
        if (scaledHeight <= height) {
            minTy = 0f
            maxTy = height - scaledHeight
        } else {
            minTy = height - scaledHeight
            maxTy = 0f
        }

        val clampedTx = values[Matrix.MTRANS_X].coerceIn(min(minTx, maxTx), maxOfFloat(minTx, maxTx))
        val clampedTy = values[Matrix.MTRANS_Y].coerceIn(min(minTy, maxTy), maxOfFloat(minTy, maxTy))

        values[Matrix.MTRANS_X] = clampedTx
        values[Matrix.MTRANS_Y] = clampedTy
        matrix.setValues(values)
    }

    private fun maxOfFloat(a: Float, b: Float): Float = if (a > b) a else b

    private fun handleTap(viewX: Float, viewY: Float) {
        val bmp = bitmap ?: return
        val inverse = Matrix()
        if (!matrix.invert(inverse)) return
        val pts = floatArrayOf(viewX, viewY)
        inverse.mapPoints(pts)
        val bitmapX = pts[0].toInt()
        val bitmapY = pts[1].toInt()
        if (bitmapX in 0 until bmp.width && bitmapY in 0 until bmp.height) {
            performFill(bitmapX, bitmapY)
        }
    }

    private fun performFill(x: Int, y: Int) {
        val current = bitmap ?: return
        val filled = floodFillEngine.floodFill(current, x, y, fillColor, originalBitmap = originalBitmap) ?: return
        pushUndo(current)
        bitmap = filled
        redoStack.clear()
        notifyUndoRedoState()
        invalidate()
    }

    private fun pushUndo(bmp: Bitmap) {
        if (undoStack.size >= MAX_UNDO_STACK_SIZE) {
            undoStack.removeFirst()
        }
        undoStack.addLast(bmp)
    }

    private fun notifyUndoRedoState() {
        onUndoRedoStateChanged?.invoke(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    companion object {
        private const val MAX_UNDO_STACK_SIZE = 15
        private const val FIXED_ZOOM_SCALE = 2.5f
        private const val MAX_SCALE_MULTIPLIER = 5f
    }
}
