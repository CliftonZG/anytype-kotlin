package com.anytypeio.anytype.core_ui.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.anytypeio.anytype.core_ui.features.editor.EditorTouchProcessor
import io.nano.tex.Graphics2D
import io.nano.tex.LaTeX
import io.nano.tex.TeXRender
import timber.log.Timber

class TexView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var textSize = 60
    private var textColor = android.graphics.Color.DKGRAY
    var render: TeXRender? = null
    private val g2 = Graphics2D()

    val editorTouchProcessor by lazy {
        EditorTouchProcessor(
            fallback = { e -> super.onTouchEvent(e) }
        )
    }

    fun TeXView(context: Context?) {
        this.TeXView(context)
    }

    fun TeXView(context: Context?, attrs: AttributeSet?) {
        this.TeXView(context, attrs)
    }

    fun TeXView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        this.TeXView(context, attrs, defStyleAttr)
    }

    fun TeXView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        this.TeXView(context, attrs, defStyleAttr, defStyleRes)
    }

    fun parseLaTeX(ltx: String) {
        try {
            var w = width
            if (w == 0) w = 2048
            Log.v("TeXView.kt", "parseLaTeX, parsing the current latex")
            render = LaTeX.instance()
                .parse(ltx, w, textSize.toFloat(), 10f, textColor)
            Log.v("TeXView.kt", "parseLaTeX, render and parse complete")
        } catch (e: Exception) {
            Log.e("TeXView.kt", "parseLaTeX: " + e.message)
        }
    }

    fun setLaTeX() {
        try {
            requestLayout()
            Log.v("TeXView.kt", "setLaTeX, requestLayout complete")
        } catch (e: Exception) {
            Log.e("TeXView.kt", "setLaTeX: " + e.message)
        }
    }

    fun setTextSize(size: Int) {
        try {
            Log.v("TeXView.kt", "setTextSize(int $size)")
            textSize = size
            if (render != null) render!!.textSize = size.toFloat()
            requestLayout()
        } catch (e: java.lang.Exception) {
            Log.e("TeXView.kt", "setTextSize error: " + e.message)
        }
    }

    fun setColor(color: Int) {
        try {
            Log.v("TeXView.kt", "setColor(int $color)")
            textColor = color
            if (render != null) render!!.setForeground(color)
            requestLayout()
        } catch (e: java.lang.Exception) {
            Log.e("TeXView.kt", "setTextSize error: " + e.message)
        }
    }

    fun invalidateRender() {
        try {
            if (render != null) {
                render!!.invalidateDrawingCache()
            } else {
                Log.v("TeXView.kt", "invalidateRender, TeXRender is null, skipping...")
            }
            invalidate()
        } catch (e: java.lang.Exception) {
            Log.e("TeXView.kt", "invalidateRender error: " + e.message)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        try {
            Log.v("TeXView.kt", "onMeasure(int widthMeasureSpec, int heightMeasureSpec)")
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            if (render == null) return
            val h = render!!.height
            setMeasuredDimension(measuredWidth, h + paddingTop + paddingBottom)
        } catch (e: java.lang.Exception) {
            Log.e("TeXView.kt", "onMeasure error: " + e.message)
        }
    }

    override fun onDraw(canvas: Canvas) {
        try {
            Log.v("TeXView.kt", "onDraw(Canvas canvas)")
            if (render == null) return
            g2.canvas = canvas
            render!!.draw(g2, paddingLeft, paddingTop)
        } catch (e: java.lang.Exception) {
            Log.e("TeXView.kt", "onDraw error: " + e.message)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Timber.d("TexView.kt, onTouchEvent")
        if (hasFocus()) return super.onTouchEvent(event)
        return editorTouchProcessor.process(this, event)
    }
}