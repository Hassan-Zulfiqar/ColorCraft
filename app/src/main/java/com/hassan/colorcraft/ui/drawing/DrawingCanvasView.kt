package com.hassan.colorcraft.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class DrawTool { BRUSH, PENCIL, ERASER }

class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var matrix = Matrix()

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var currentTool: DrawTool = DrawTool.BRUSH
    private var strokeColor: Int = Color.BLACK
    private var strokeWidthPx: Float = DEFAULT_STROKE_WIDTH
    private lateinit var toolPaint: Paint

    private var currentStrokePath: Path? = null
    private var lastPathX = 0f
    private var lastPathY = 0f
    private var strokeStartViewX = 0f
    private var strokeStartViewY = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()

    var onUndoRedoStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    private var pendingBlankBitmapCreation = false

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        rebuildToolPaint()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pendingBlankBitmapCreation && w > 0 && h > 0) {
            pendingBlankBitmapCreation = false
            bitmap = createBlankBitmapForCurrentSize()
            finishLoadBitmap()
        } else {
            applyFitToViewMatrix()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        canvas.drawBitmap(bmp, matrix, bitmapPaint)

        val path = currentStrokePath
        if (path != null) {
            canvas.save()
            canvas.concat(matrix)
            canvas.drawPath(path, toolPaint)
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                strokeStartViewX = event.x
                strokeStartViewY = event.y

                val point = viewPointToBitmapPoint(event.x, event.y)
                if (point != null) {
                    lastPathX = point[0]
                    lastPathY = point[1]
                    val path = Path()
                    path.moveTo(lastPathX, lastPathY)
                    currentStrokePath = path
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val path = currentStrokePath ?: return true
                val point = viewPointToBitmapPoint(event.x, event.y) ?: return true

                val newX = point[0]
                val newY = point[1]
                val midX = (lastPathX + newX) / 2f
                val midY = (lastPathY + newY) / 2f
                path.quadTo(lastPathX, lastPathY, midX, midY)
                lastPathX = newX
                lastPathY = newY

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val path = currentStrokePath
                currentStrokePath = null

                if (path != null) {
                    val distanceMoved = hypot(event.x - strokeStartViewX, event.y - strokeStartViewY)
                    if (distanceMoved <= touchSlop) {
                        path.lineTo(lastPathX, lastPathY)
                    }
                    commitStroke(path)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                currentStrokePath = null
                invalidate()
                return true
            }
        }
        return true
    }

    fun loadBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            this.bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            finishLoadBitmap()
        } else if (width > 0 && height > 0) {
            this.bitmap = createBlankBitmapForCurrentSize()
            finishLoadBitmap()
        } else {
            pendingBlankBitmapCreation = true
        }
    }

    private fun finishLoadBitmap() {
        undoStack.clear()
        redoStack.clear()
        currentStrokePath = null
        if (width > 0 && height > 0) {
            applyFitToViewMatrix()
        } else {
            matrix = Matrix()
        }
        notifyUndoRedoState()
        invalidate()
    }

    private fun createBlankBitmapForCurrentSize(): Bitmap {
        var w = width
        var h = height
        val largerEdge = max(w, h).toFloat()
        if (largerEdge > MAX_CANVAS_EDGE_PX) {
            val scaleFactor = MAX_CANVAS_EDGE_PX / largerEdge
            w = (w * scaleFactor).toInt()
            h = (h * scaleFactor).toInt()
        }
        return createBlankBitmap(w, h)
    }

    fun setTool(tool: DrawTool) {
        currentTool = tool
        rebuildToolPaint()
    }

    fun setStrokeColor(color: Int) {
        strokeColor = color
        rebuildToolPaint()
    }

    fun setStrokeWidth(width: Float) {
        strokeWidthPx = width
        rebuildToolPaint()
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

    fun clearCanvas() {
        val current = bitmap ?: return
        bitmap = createBlankBitmap(current.width, current.height)
        undoStack.clear()
        redoStack.clear()
        currentStrokePath = null
        notifyUndoRedoState()
        invalidate()
    }

    fun getCurrentBitmap(): Bitmap? = bitmap

    private fun createBlankBitmap(w: Int, h: Int): Bitmap {
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
        }
    }

    private fun applyFitToViewMatrix() {
        val bmp = bitmap ?: return
        if (width <= 0 || height <= 0 || bmp.width <= 0 || bmp.height <= 0) return

        val scale = min(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        val scaledWidth = bmp.width * scale
        val scaledHeight = bmp.height * scale
        val dx = (width - scaledWidth) / 2f
        val dy = (height - scaledHeight) / 2f

        val newMatrix = Matrix()
        newMatrix.postScale(scale, scale)
        newMatrix.postTranslate(dx, dy)
        matrix = newMatrix
        invalidate()
    }

    private fun viewPointToBitmapPoint(viewX: Float, viewY: Float): FloatArray? {
        val inverse = Matrix()
        if (!matrix.invert(inverse)) return null
        val pts = floatArrayOf(viewX, viewY)
        inverse.mapPoints(pts)
        return pts
    }

    private fun commitStroke(path: Path) {
        val current = bitmap ?: return
        pushUndo(current.copy(current.config ?: Bitmap.Config.ARGB_8888, true))

        val canvas = Canvas(current)
        canvas.drawPath(path, toolPaint)

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

    private fun rebuildToolPaint() {
        toolPaint = when (currentTool) {
            DrawTool.BRUSH -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = strokeColor
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                xfermode = null
            }

            DrawTool.PENCIL -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = strokeColor
                strokeWidth = strokeWidthPx * 0.4f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                xfermode = null
            }

            DrawTool.ERASER -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
    }

    companion object {
        private const val MAX_UNDO_STACK_SIZE = 15
        private const val MAX_CANVAS_EDGE_PX = 1600
        private const val DEFAULT_STROKE_WIDTH = 12f
    }
}
