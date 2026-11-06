package com.passworddisk.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

/**
 * 扫码取景遮罩。
 *
 * ZXing 自带的 ViewfinderView 只画一块方形暗角 + 一条跳动的红线，没有圆角、
 * 没有四角标记，在大屏上显得很粗糙。这里自绘一套：
 *
 * - 圆角取景窗（挖空处的暗角用 EVEN_ODD 路径一次画完，避免混合模式在部分机型上失效）；
 * - 四角白色圆头标记，指示扫码范围；
 * - 一条自上而下的绿色扫描线，带渐变光晕，告诉用户相机正在工作。
 *
 * 解码本身仍由 ZXing 处理整幅预览图，取景窗只是视觉引导，尺寸不需要与它严格一致。
 */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val windowRect = RectF()
    private val scrimPath = Path()
    private val cornerPath = Path()

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3000000.toInt()
    }
    private val windowRadius = dp(26f)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = 0x4DFFFFFF
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3.5f)
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = BRAND_GREEN
    }

    private val scanGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var scanProgress = 0f
    private var animator: ValueAnimator? = null

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val side = min(w - dp(72f), dp(272f)).coerceAtMost(h * 0.46f)
        val left = (w - side) / 2f
        // 略低于正中：上方还有标题，视觉重心才不会偏上
        val top = (h - side) / 2f + dp(4f)
        windowRect.set(left, top, left + side, top + side)

        scrimPath.reset()
        scrimPath.fillType = Path.FillType.EVEN_ODD
        scrimPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        scrimPath.addRoundRect(windowRect, windowRadius, windowRadius, Path.Direction.CW)

        val inset = dp(2f)
        val beamTop = windowRect.top + inset
        val beamBottom = windowRect.bottom - inset
        scanLinePaint.shader = LinearGradient(
            windowRect.left, 0f, windowRect.right, 0f,
            intArrayOf(Color.TRANSPARENT, BRAND_GREEN, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        scanGlowPaint.shader = LinearGradient(
            0f, beamTop, 0f, beamBottom,
            intArrayOf(Color.TRANSPARENT, 0x330E9F6E, 0x330E9F6E, Color.TRANSPARENT),
            floatArrayOf(0f, 0.32f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (windowRect.isEmpty) return

        canvas.drawPath(scrimPath, scrimPaint)
        canvas.drawRoundRect(windowRect, windowRadius, windowRadius, borderPaint)
        drawCorners(canvas)
        drawScanBeam(canvas)
    }

    /**
     * 四角标记：沿着取景窗的圆角画一段弧，再向两侧各延伸一小段直线。
     * 若直接在直角位置画 L 形线段，圆角处的线段会落到挖空区域之外，看起来像错位的方框。
     */
    private fun drawCorners(canvas: Canvas) {
        val inset = dp(1.5f)
        val r = windowRadius
        val len = dp(22f)
        val left = windowRect.left + inset
        val top = windowRect.top + inset
        val right = windowRect.right - inset
        val bottom = windowRect.bottom - inset
        val arcRect = RectF()

        cornerPath.reset()

        // 左上
        cornerPath.moveTo(left, top + r + len)
        cornerPath.lineTo(left, top + r)
        arcRect.set(left, top, left + 2 * r, top + 2 * r)
        cornerPath.arcTo(arcRect, 180f, 90f, false)
        cornerPath.lineTo(left + r + len, top)

        // 右上
        cornerPath.moveTo(right - r - len, top)
        cornerPath.lineTo(right - r, top)
        arcRect.set(right - 2 * r, top, right, top + 2 * r)
        cornerPath.arcTo(arcRect, 270f, 90f, false)
        cornerPath.lineTo(right, top + r + len)

        // 右下
        cornerPath.moveTo(right, bottom - r - len)
        cornerPath.lineTo(right, bottom - r)
        arcRect.set(right - 2 * r, bottom - 2 * r, right, bottom)
        cornerPath.arcTo(arcRect, 0f, 90f, false)
        cornerPath.lineTo(right - r - len, bottom)

        // 左下
        cornerPath.moveTo(left + r + len, bottom)
        cornerPath.lineTo(left + r, bottom)
        arcRect.set(left, bottom - 2 * r, left + 2 * r, bottom)
        cornerPath.arcTo(arcRect, 90f, 90f, false)
        cornerPath.lineTo(left, bottom - r - len)

        canvas.drawPath(cornerPath, cornerPaint)
    }

    private fun drawScanBeam(canvas: Canvas) {
        val inset = dp(6f)
        val top = windowRect.top + inset
        val bottom = windowRect.bottom - inset
        val y = top + (bottom - top) * scanProgress

        // 一条扫过取景窗的柔光带 + 中间的高亮线
        canvas.save()
        canvas.clipRect(windowRect.left + dp(2f), top, windowRect.right - dp(2f), bottom)
        canvas.drawRect(windowRect.left, y - dp(26f), windowRect.right, y + dp(26f), scanGlowPaint)
        canvas.drawRect(windowRect.left + dp(10f), y, windowRect.right - dp(10f), y + dp(2f), scanLinePaint)
        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startScanning()
    }

    override fun onDetachedFromWindow() {
        stopScanning()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startScanning() else stopScanning()
    }

    private fun startScanning() {
        if (animator != null || !isAttachedToWindow || visibility != VISIBLE) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2100L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                scanProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopScanning() {
        animator?.cancel()
        animator = null
    }

    private companion object {
        const val BRAND_GREEN = 0xFF0E9F6E.toInt()
    }
}
