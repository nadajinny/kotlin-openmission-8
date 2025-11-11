package com.example.tagmoa.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.doOnLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.tagmoa.R

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val path = Path()
    private val firstCurveStart = PointF()
    private val firstCurveEnd = PointF()
    private val firstCurveControlPoint1 = PointF()
    private val firstCurveControlPoint2 = PointF()
    private val secondCurveStart = PointF()
    private val secondCurveEnd = PointF()
    private val secondCurveControlPoint1 = PointF()
    private val secondCurveControlPoint2 = PointF()
    private val highlightBounds = Rect()

    private val navPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(context, R.color.cbn_background)
        setShadowLayer(
            resources.getDimension(R.dimen.cbn_shadow_radius),
            0f,
            resources.getDimension(R.dimen.cbn_shadow_dy),
            ContextCompat.getColor(context, R.color.cbn_shadow)
        )
    }
    private val highlightDefaultColor = ContextCompat.getColor(context, R.color.cbn_highlight)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = highlightDefaultColor
    }

    private val itemsLayoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        resources.getDimensionPixelSize(R.dimen.cbn_nav_height),
        Gravity.BOTTOM
    )
    private val itemTouchPadding = resources.getDimensionPixelSize(R.dimen.cbn_icon_touch_padding)
    private val iconSize = resources.getDimensionPixelSize(R.dimen.cbn_icon_size)
    private val layoutHeight = resources.getDimension(R.dimen.cbn_layout_height)
    private val navHeight = resources.getDimension(R.dimen.cbn_nav_height)
    private val bottomNavOffsetY = layoutHeight - navHeight
    private val curveBottomOffset = resources.getDimension(R.dimen.cbn_bottom_curve_offset)
    private val fabSize = resources.getDimension(R.dimen.cbn_fab_size)
    private val fabRadius = fabSize / 2f
    private val fabTopOffset = resources.getDimension(R.dimen.cbn_fab_top_offset)
    private val curveExtraWidth = resources.getDimension(R.dimen.cbn_curve_extra_width)
    private val curveHalfWidth = fabRadius * 2 + curveExtraWidth
    private val firstCurveControl1XOffset =
        resources.getDimension(R.dimen.cbn_first_curve_control_1_x_offset)
    private val firstCurveControl1YOffset =
        resources.getDimension(R.dimen.cbn_first_curve_control_1_y_offset)
    private val firstCurveControl2XOffset =
        resources.getDimension(R.dimen.cbn_first_curve_control_2_x_offset)
    private val firstCurveControl2YOffset =
        resources.getDimension(R.dimen.cbn_first_curve_control_2_y_offset)
    private val secondCurveControl1XOffset =
        resources.getDimension(R.dimen.cbn_second_curve_control_1_x_offset)
    private val secondCurveControl1YOffset =
        resources.getDimension(R.dimen.cbn_second_curve_control_1_y_offset)
    private val secondCurveControl2XOffset =
        resources.getDimension(R.dimen.cbn_second_curve_control_2_x_offset)
    private val secondCurveControl2YOffset =
        resources.getDimension(R.dimen.cbn_second_curve_control_2_y_offset)
    private val highlightCenterY = fabTopOffset + fabRadius

    private val itemsLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        clipToPadding = false
        setPadding(0, 0, 0, itemTouchPadding / 2)
    }

    private val inactiveIcons = mutableListOf<Drawable>()
    private val activeIcons = mutableListOf<Drawable>()
    private val itemViews = mutableListOf<BottomNavItemView>()
    private var items: List<CurvedBottomNavItem> = emptyList()
    private var listener: ((CurvedBottomNavItem) -> Unit)? = null

    private var menuCellWidth: Int = 0
    private var currentCenterX = -1f
    private var displayCenterX = -1f
    private var selectedIndex = -1
    private var pendingIndex = 0
    private var isAnimating = false
    private var animator: ValueAnimator? = null
    private var animationDuration = 320L

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        addView(itemsLayout, itemsLayoutParams)
    }

    fun setItems(items: List<CurvedBottomNavItem>, defaultIndex: Int = 0) {
        require(items.isNotEmpty()) { "CurvedBottomNavigationView requires at least one item." }
        animator?.cancel()
        this.items = items
        pendingIndex = defaultIndex.coerceIn(items.indices)
        selectedIndex = -1
        itemViews.clear()
        inactiveIcons.clear()
        activeIcons.clear()
        itemsLayout.removeAllViews()

        val ripple = TypedValue()
        context.theme.resolveAttribute(
            androidx.appcompat.R.attr.selectableItemBackgroundBorderless,
            ripple,
            true
        )

        items.forEachIndexed { index, item ->
            val baseDrawable = AppCompatResources.getDrawable(context, item.iconRes)
                ?.mutate()
                ?: error("Icon resource ${item.iconRes} is invalid.")
            val inactiveDrawable = baseDrawable.constantState?.newDrawable()?.mutate()
                ?: baseDrawable.mutate()
            DrawableCompat.setTint(
                inactiveDrawable,
                ContextCompat.getColor(context, R.color.cbn_icon_inactive)
            )

            val activeDrawable = baseDrawable.constantState?.newDrawable()?.mutate()
                ?: baseDrawable.mutate()
            DrawableCompat.setTint(
                activeDrawable,
                ContextCompat.getColor(context, R.color.cbn_icon_active)
            )

            inactiveIcons += inactiveDrawable
            activeIcons += activeDrawable

            val rippleDrawable = if (ripple.resourceId != 0) {
                ContextCompat.getDrawable(context, ripple.resourceId)
            } else {
                null
            }

            val itemView = BottomNavItemView(context).apply {
                setMenuIcon(inactiveDrawable)
                background = rippleDrawable
                setPadding(itemTouchPadding, itemTouchPadding, itemTouchPadding, itemTouchPadding)
                contentDescription = item.contentDescription?.let { context.getString(it) }
                setOnClickListener { handleItemClick(index) }
            }

            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            params.weight = 1f
            itemsLayout.addView(itemView, params)
            itemViews += itemView
        }

        doOnLayout { initializeSelection() }
    }

    fun setOnItemSelectedListener(listener: (CurvedBottomNavItem) -> Unit) {
        this.listener = listener
    }

    fun selectItem(@IdRes itemId: Int, animate: Boolean = false) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return
        if (!animate || menuCellWidth == 0) {
            pendingIndex = index
            animator?.cancel()
            updateSelectionInstant(index)
            listener?.invoke(items[index])
        } else {
            handleItemClick(index)
        }
    }

    private fun initializeSelection() {
        if (items.isEmpty()) return
        if (width == 0) return
        menuCellWidth = width / items.size
        updateSelectionInstant(pendingIndex)
    }

    private fun updateSelectionInstant(index: Int) {
        if (items.isEmpty() || menuCellWidth == 0) return
        itemViews.forEach { it.visibility = View.VISIBLE }
        itemViews[index].visibility = View.INVISIBLE
        selectedIndex = index
        currentCenterX = menuCellWidth * index + menuCellWidth / 2f
        computeCurve(currentCenterX)
        invalidate()
    }

    private fun handleItemClick(index: Int) {
        if (items.isEmpty()) return
        listener?.invoke(items[index])
        if (index == selectedIndex || isAnimating) {
            return
        }
        startSelectionAnimation(index)
    }

    private fun startSelectionAnimation(targetIndex: Int) {
        val previousIndex = selectedIndex
        val startX = currentCenterX
        val endX = menuCellWidth * targetIndex + menuCellWidth / 2f
        if (startX == -1f) {
            updateSelectionInstant(targetIndex)
            return
        }

        itemViews.getOrNull(previousIndex)?.visibility = View.VISIBLE
        itemViews[targetIndex].visibility = View.INVISIBLE
        selectedIndex = targetIndex

        animator?.cancel()
        animator = ValueAnimator.ofFloat(startX, endX).apply {
            duration = animationDuration
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                currentCenterX = it.animatedValue as Float
                computeCurve(currentCenterX)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    isAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    isAnimating = false
                }
            })
        }
        animator?.start()
    }

    private fun computeCurve(requestedCenterX: Float) {
        val minCenter = curveHalfWidth
        val maxCenter = (width - curveHalfWidth).coerceAtLeast(minCenter)
        val safeCenterX = requestedCenterX.coerceIn(minCenter, maxCenter)
        displayCenterX = safeCenterX

        val firstStartX = safeCenterX - curveHalfWidth
        val secondEndX = safeCenterX + curveHalfWidth
        val curveBottomY = layoutHeight - curveBottomOffset

        firstCurveStart.set(firstStartX, bottomNavOffsetY)
        firstCurveEnd.set(safeCenterX, curveBottomY)
        firstCurveControlPoint1.set(
            firstCurveStart.x + firstCurveControl1XOffset,
            bottomNavOffsetY + firstCurveControl1YOffset
        )
        firstCurveControlPoint2.set(
            firstCurveEnd.x - firstCurveControl2XOffset,
            firstCurveEnd.y - firstCurveControl2YOffset
        )

        secondCurveStart.set(firstCurveEnd.x, firstCurveEnd.y)
        secondCurveEnd.set(secondEndX, bottomNavOffsetY)
        secondCurveControlPoint1.set(
            secondCurveStart.x + secondCurveControl1XOffset,
            secondCurveStart.y - secondCurveControl1YOffset
        )
        secondCurveControlPoint2.set(
            secondCurveEnd.x - secondCurveControl2XOffset,
            bottomNavOffsetY + secondCurveControl2YOffset
        )

        path.reset()
        path.moveTo(0f, bottomNavOffsetY)
        path.lineTo(firstCurveStart.x, firstCurveStart.y)
        path.cubicTo(
            firstCurveControlPoint1.x,
            firstCurveControlPoint1.y,
            firstCurveControlPoint2.x,
            firstCurveControlPoint2.y,
            firstCurveEnd.x,
            firstCurveEnd.y
        )
        path.cubicTo(
            secondCurveControlPoint1.x,
            secondCurveControlPoint1.y,
            secondCurveControlPoint2.x,
            secondCurveControlPoint2.y,
            secondCurveEnd.x,
            secondCurveEnd.y
        )
        path.lineTo(width.toFloat(), bottomNavOffsetY)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (items.isEmpty() || w == 0) return
        menuCellWidth = w / items.size
        val targetIndex = if (selectedIndex in items.indices) selectedIndex else pendingIndex
        updateSelectionInstant(targetIndex)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = paddingTop + paddingBottom + layoutHeight.toInt()
        val resolvedHeight = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, resolvedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (items.isEmpty() || currentCenterX == -1f) {
            return
        }
        canvas.drawPath(path, navPaint)
        canvas.drawCircle(displayCenterX, highlightCenterY, fabRadius, highlightPaint)

        val activeDrawable = activeIcons.getOrNull(selectedIndex) ?: return
        val half = iconSize / 2
        highlightBounds.set(
            (displayCenterX - half).toInt(),
            (highlightCenterY - half).toInt(),
            (displayCenterX + half).toInt(),
            (highlightCenterY + half).toInt()
        )
        activeDrawable.bounds = highlightBounds
        activeDrawable.draw(canvas)
    }

    fun getItemCenterX(@IdRes itemId: Int): Float {
        if (items.isEmpty() || width == 0) return -1f
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return -1f
        val cellWidth = if (menuCellWidth > 0) menuCellWidth else width / items.size
        return index * cellWidth + cellWidth / 2f
    }

    fun setHighlightOverlayColor(@ColorInt color: Int?) {
        val target = color ?: highlightDefaultColor
        if (highlightPaint.color != target) {
            highlightPaint.color = target
            invalidate()
        }
    }
}
