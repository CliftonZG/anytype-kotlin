package com.anytypeio.anytype.core_ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.FileUriExposedException
import android.provider.Browser
import android.text.Annotation
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.*
import android.util.Log
import android.view.View
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_ui.extensions.color
import com.anytypeio.anytype.core_ui.widgets.text.TextInputWidget
import com.anytypeio.anytype.core_utils.ext.timber
import com.anytypeio.anytype.core_utils.ext.toast
import com.shekhargulati.urlcleaner.UrlCleaner
import com.shekhargulati.urlcleaner.UrlCleanerException
import io.nano.tex.Graphics2D
import io.nano.tex.LaTeX
import io.nano.tex.TeXRender
import timber.log.Timber
import kotlin.math.roundToInt

// InlineLatexSpan is a custom span that holds the LaTeX rendering details
class InlineLatexSpan(private val render: TeXRender, private val plainText: String) : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        // Use the getWidth() from TeXRender to determine the span's width
        val width = render.width
        val height = render.height
        val depth = render.depth

        // If FontMetricsInt is provided, adjust the ascent and descent
        if (fm != null) {
            Timber.d("InlineLatexSpan, fm != null")
            fm.ascent = -height          // Ascent is negative height (distance from baseline to top)
            fm.descent = depth           // Descent is the depth (distance from baseline to bottom)
            fm.top = fm.ascent           // Top is the ascent
            fm.bottom = fm.ascent + depth  // Bottom is the sum of ascent and depth
        }

        // Return the width of the rendered LaTeX
        Timber.d("InlineLatexSpan: getSize - LaTeX width: $width, height: $height, depth: $depth")
        return width
    }

    fun getPlainText(): String {
        return plainText
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        // Log drawing details for debugging
        Timber.d("Canvas size: ${canvas.width}x${canvas.height}")

        // Calculate the LaTeX height and depth
        val height = render.height
        val depth = render.depth

        val adjustedY = y - height + depth

        Timber.d("InlineLatexSpan: draw - Drawing LaTeX at x: $x, y: $adjustedY, with render: $render")

        try {
            Timber.d("InlineLatexSpan draw")
            render.draw(Graphics2D(canvas), x.toInt(), adjustedY)
        } catch (e: Exception) {
            Timber.e("InlineLatexSpan draw: ${e.message}")
        }
        Timber.d("finished rendering")
    }
}

interface Span {
    class Bold : StyleSpan(Typeface.BOLD), Span
    class Italic : StyleSpan(Typeface.ITALIC), Span
    class Strikethrough : StrikethroughSpan(), Span
    class TextColor(color: Int, val value: String) : ForegroundColorSpan(color), Span

    class Latex : Span {
        var render: TeXRender? = null
        // Function to convert LaTeX string to Spannable
        fun convertLatexToSpannable(latex: String, width: Int, textSize: Float, textColor: Int): InlineLatexSpan? {
            Timber.d("convertLatexToSpannable, latex: $latex")
            try {
                render = LaTeX.instance().parse(latex, width, textSize, 10F, textColor)
            } catch (e: Exception) {
                Timber.e("parseLaTeX: ${e.message}")
            }

            if (render == null) {
                Timber.w("")
                return null
            } else {
                Timber.d("finished parsing latex")
                return InlineLatexSpan(render!!, latex)
            }
        }
        private fun calculateDynamicWidth(latex: String, textSize: Float, paddingLeft: Int, paddingRight: Int): Int {
            // Here, we'll estimate the width using Paint.measureText()
            val paint = Paint()
            paint.textSize = textSize.toFloat()

            // We calculate the width of the LaTeX content.
            // You can optionally add padding or margins as needed.
            return (paint.measureText(latex) + paddingLeft + paddingRight).toInt()
        }
    }

    class Url(
        val url: String,
        val color: Int,
        private val underlineHeight: Float
    ) :
        ClickableSpan(), Span {

        override fun updateDrawState(ds: TextPaint) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val alphaText = (Color.alpha(color) * 0.65f).roundToInt()
                val alphaUnderline = (Color.alpha(color) * 0.35f).roundToInt()
                ds.color =
                    Color.argb(alphaText, Color.red(color), Color.green(color), Color.blue(color))
                ds.underlineColor =
                    Color.argb(alphaUnderline, Color.red(color), Color.green(color), Color.blue(color))
                ds.underlineThickness = underlineHeight
            } else {
                ds.color = color
                super.updateDrawState(ds)
            }
        }

        override fun onClick(widget: View) {
            val intent = createIntent(widget.context, url)
            try {
                widget.context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.timber()
                normalizeUrl(widget.context, url)
            } catch (e: FileUriExposedException) {
                e.timber()
            } catch (e: NullPointerException) {
                e.timber()
                widget.context.toast("Url was null or empty")
            }
        }

        private fun normalizeUrl(context: Context, url: String) {
            try {
                val normalizedUrl = UrlCleaner.normalizeUrl(url)
                val intent = createIntent(context, normalizedUrl)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.timber()
                    context.toast("Couldn't open url:$normalizedUrl")
                }
            } catch (e: UrlCleanerException) {
                e.timber()
                context.toast("Couldn't parse url")
            } catch (e: IllegalArgumentException) {
                e.timber()
                context.toast("Couldn't parse url")
            } catch (e: NullPointerException) {
                e.timber()
                context.toast("Couldn't parse url. String was null")
            }
        }

        private fun createIntent(context: Context, url: String): Intent {
            val uri = Uri.parse(url)
            return Intent(Intent.ACTION_VIEW, uri).apply {
                putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            }
        }
    }

    class Font(family: String) : TypefaceSpan(family), Span

    class Keyboard(value: String) : Annotation(KEYBOARD_KEY, value), Span {
        companion object {
            const val KEYBOARD_KEY = "keyboard"
        }
    }

    class Highlight(color: String) : Annotation(HIGHLIGHT_KEY, color), Span {
        companion object {
            const val HIGHLIGHT_KEY = "highlight"
        }
    }

    class ObjectLink(
        val context: Context,
        val link: String?,
        val color: Int,
        val click: ((String) -> Unit)?,
        val isArchived: Boolean
    ) :
        ClickableSpan(), Span {

        private val textColorArchive = context.color(R.color.text_tertiary)

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            if (isArchived) {
                ds.color = textColorArchive
            } else {
                ds.color = color
            }
        }

        override fun onClick(widget: View) {
            if (!link.isNullOrBlank() && !isArchived) {
                (widget as? TextInputWidget)?.enableReadMode()
                click?.invoke(link)
            } else {
                Timber.e("Can't proceed with ObjectLinkSpan click, link is null or blank or archived")
            }
        }
    }
}