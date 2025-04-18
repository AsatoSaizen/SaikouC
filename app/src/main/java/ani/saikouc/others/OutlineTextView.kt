package ani.saikouc.others

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import ani.saikouc.R

class OutlineTextView : AppCompatTextView {

    private val defaultStrokeWidth = 0F
    private var isDrawing: Boolean = false

    private var strokeColor: Int = 0
    private var strokeWidth: Float = 0.toFloat()

    constructor(context: Context) : super(context) {
        initResources(context, null)

    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initResources(context, attrs)

    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initResources(context, attrs)

    }

    private fun initResources(context: Context?, attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context?.obtainStyledAttributes(attrs, R.styleable.OutlineTextView)
            strokeColor = a!!.getColor(
                R.styleable.OutlineTextView_outlineColor,
                currentTextColor
            )
            strokeWidth = a.getFloat(
                R.styleable.OutlineTextView_outlineWidth,
                defaultStrokeWidth
            )

            a.recycle()
        } else {
            strokeColor = currentTextColor
            strokeWidth = defaultStrokeWidth
        }
        setStrokeWidth(strokeWidth)
    }

    private val Float.toPx
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
        )

    private fun setStrokeWidth(width: Float) {
        strokeWidth = width.toPx
    }

    override fun invalidate() {
        if (isDrawing) return
        super.invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        if (strokeWidth > 0) {
            isDrawing = true
            super.onDraw(canvas)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            val colorTmp = paint.color
            setTextColor(strokeColor)
            super.onDraw(canvas)

            setTextColor(colorTmp)
            paint.style = Paint.Style.FILL

            isDrawing = false
        } else {
            super.onDraw(canvas)
        }
    }

}
