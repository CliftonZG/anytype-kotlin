package com.anytypeio.anytype.core_ui.widgets.text

import android.R.id.copy
import android.R.id.paste
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcelable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.DragEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.withTranslation
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_ui.common.InlineLatexSpan
import com.anytypeio.anytype.core_ui.common.Span
import com.anytypeio.anytype.core_ui.features.editor.EditorTouchProcessor
import com.anytypeio.anytype.core_ui.features.editor.holders.ext.toIMECode
import com.anytypeio.anytype.core_ui.tools.ClipboardInterceptor
import com.anytypeio.anytype.core_ui.tools.CustomBetterLinkMovementMethod
import com.anytypeio.anytype.core_ui.tools.LockableFocusChangeListener
import com.anytypeio.anytype.core_ui.tools.MentionTextWatcher
import com.anytypeio.anytype.core_ui.tools.TextInputTextWatcher
import com.anytypeio.anytype.core_ui.widgets.text.highlight.HighlightAttributeReader
import com.anytypeio.anytype.core_ui.widgets.text.highlight.HighlightDrawer
import com.anytypeio.anytype.core_utils.ext.showKeyboard
import com.anytypeio.anytype.core_utils.text.OnEnterActionListener
import com.anytypeio.anytype.presentation.editor.editor.model.BlockView
import kotlinx.parcelize.Parcelize
import timber.log.Timber

class TextInputWidget : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
        setupHighlightHelpers(context, attrs)
        setOnLongClickListener { view -> view != null && !view.hasFocus() }
        context.obtainStyledAttributes(attrs, R.styleable.TextInputWidget).apply {
            ignoreDragAndDrop = getBoolean(R.styleable.TextInputWidget_ignoreDragAndDrop, false)
            pasteAsPlainTextOnly =
                getBoolean(R.styleable.TextInputWidget_onlyPasteAsPlaneText, false)
            recycle()
        }
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        setup()
        setupHighlightHelpers(context, attrs)
        setOnLongClickListener { view -> view != null && !view.hasFocus() }
        context.obtainStyledAttributes(attrs, R.styleable.TextInputWidget).apply {
            ignoreDragAndDrop = getBoolean(R.styleable.TextInputWidget_ignoreDragAndDrop, false)
            recycle()
        }
    }

    private var ignoreDragAndDrop = false
    private var pasteAsPlainTextOnly = false
    private var inReadMode = false
    private var isInlineLatexProcessed = false

    val editorTouchProcessor by lazy {
        EditorTouchProcessor(
            fallback = { e -> super.onTouchEvent(e) }
        )
    }

    private val watchers: MutableList<TextInputTextWatcher> = mutableListOf()

    private var highlightDrawer: HighlightDrawer? = null

    var selectionWatcher: ((IntRange) -> Unit)? = null

    var clipboardInterceptor: ClipboardInterceptor? = null

    /**
     * Returns a bool value, indicating whether or not to absorb the click
     */
    var backButtonWatcher: (() -> Boolean)? = null

    private var isSelectionWatcherBlocked = false

    private var inputAction: BlockView.InputAction = DEFAULT_INPUT_WIDGET_ACTION

    fun setInputAction(action: BlockView.InputAction) {
        if (inputAction != action) {
            inputAction = action
        }
    }

    private fun setup() {
        enableEditMode()
    }

    fun enableEditMode() {
        Timber.d("enableEditMode")
        setRawInputType(
            InputType.TYPE_CLASS_TEXT
                    or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        )
        imeOptions = inputAction.toIMECode()
        setTextIsSelectable(true)
        inReadMode = false
    }

    fun enableReadMode() {
        Timber.d("enableReadMode")
        pauseTextWatchers {
            inReadMode = true
            setHorizontallyScrolling(false)
            setTextIsSelectable(false)
        }
    }

    override fun dispatchDragEvent(event: DragEvent?): Boolean {
        return super.dispatchDragEvent(event)
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event != null
            && event.keyCode == KeyEvent.KEYCODE_BACK
            && event.action == KeyEvent.ACTION_UP
            && backButtonWatcher?.invoke() == true
        ) {
            true
        } else {
            super.onKeyPreIme(keyCode, event)
        }
    }

    private fun setupHighlightHelpers(context: Context, attrs: AttributeSet) {
        HighlightAttributeReader(context, attrs).let { reader ->
            highlightDrawer = HighlightDrawer(
                horizontalPadding = reader.horizontalPadding,
                verticalPadding = reader.verticalPadding,
                drawable = reader.drawable,
                drawableLeft = reader.drawableLeft,
                drawableMid = reader.drawableMid,
                drawableRight = reader.drawableRight
            )
        }
    }

    override fun addTextChangedListener(watcher: TextWatcher) {
        Timber.d("addTextChangedListener")
        if (watcher is TextInputTextWatcher) {
            watchers.add(watcher)
        }
        super.addTextChangedListener(watcher)
    }

    override fun removeTextChangedListener(watcher: TextWatcher) {
        Timber.d("removeTextChangedListener")
        if (watcher is TextInputTextWatcher) {
            watchers.remove(watcher)
        }
        super.removeTextChangedListener(watcher)
    }

    fun dismissMentionWatchers() {
        watchers.filterIsInstance(MentionTextWatcher::class.java).forEach { it.onDismiss() }
    }

    fun pauseTextWatchers(block: () -> Unit) = synchronized(this) {
        Timber.d("pauseTextWatchers")
        lockTextWatchers()
        block()
        unlockTextWatchers()
    }

    fun pauseSelectionWatcher(block: () -> Unit) = synchronized(this) {
        Timber.d("pauseSelectionWatcher")
        isSelectionWatcherBlocked = true
        block()
        isSelectionWatcherBlocked = false
    }

    fun pauseFocusChangeListener(block: () -> Unit) = synchronized(this) {
        Timber.d("pauseFocusChangeListener")
        val listener = onFocusChangeListener
        if (listener is LockableFocusChangeListener) {
            listener.lock()
        }
        block()
        if (listener is LockableFocusChangeListener) {
            listener.unlock()
        }
    }

    private fun lockTextWatchers() {
        Timber.d("lockTextWatchers")
        watchers.forEach { it.lock() }
    }

    private fun unlockTextWatchers() {
        Timber.d("unlockTextWatchers")
        watchers.forEach { it.unlock() }
    }

    /**
     * Send selection event only for blocks in focus state
     */
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (isFocused && !isSelectionWatcherBlocked) {
            Timber.d("New selection: $selStart - $selEnd")
            selectionWatcher?.invoke(selStart..selEnd)

            // TO-DO.............. Inline Latex Rendering
            Timber.d("text is Spanned: ${text is Spanned}")
            Timber.d("current text: $text")
        }
        super.onSelectionChanged(selStart, selEnd)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        Timber.d("onTextContextMenuItem")
        if (clipboardInterceptor == null) {
            return super.onTextContextMenuItem(id)
        }

        var consumed = false

        when (id) {
            paste -> {
                if (pasteAsPlainTextOnly) {
                    super.onTextContextMenuItem(android.R.id.pasteAsPlainText)
                    consumed = true
                } else {
                    clipboardInterceptor?.onClipboardAction(
                        ClipboardInterceptor.Action.Paste(
                            selection = selectionStart..selectionEnd
                        )
                    )
                    consumed = true
                }
            }
            copy -> {
                clipboardInterceptor?.onClipboardAction(
                    ClipboardInterceptor.Action.Copy(
                        selection = selectionStart..selectionEnd
                    )
                )
                consumed = true
            }
        }

        return if (!consumed) {
            super.onTextContextMenuItem(id)
        } else {
            consumed
        }
    }

    override fun onDraw(canvas: Canvas) {
        Timber.d("onDraw and isFocused: $isFocused")
        // Extracting the text from the Spanned object
        var spannedText = text as Spanned
        if (text is Spanned && layout != null) {
            if (!isFocused) {
                if (!isInlineLatexProcessed) {
                    // Search for LaTeX expressions within $...$
                    val latexPattern = """\$(.*?)\$""".toRegex()
                    val matches = latexPattern.findAll(spannedText.toString())

                    // Create a SpannableStringBuilder from the current Spanned text (which will modify the original text)
                    val spannableString = SpannableStringBuilder(spannedText)

                    // Loop through all LaTeX matches and replace them with the corresponding LaTeX span
                    for (match in matches) {
                        val latexExpression = match.groupValues[1] // Extract LaTeX expression inside $...$

                        // Render LaTeX to Spannable
                        val latexSpan = Span.Latex()
                        val latexSpannable = latexSpan.convertLatexToSpannable(latexExpression, 500, 40F, android.graphics.Color.WHITE)

                        // Replace the LaTeX expression in the SpannableStringBuilder
                        val start = match.range.first
                        val end = match.range.last + 1
                        spannableString.setSpan(latexSpannable, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    text = spannableString
                    spannedText = text as Spanned
                    Timber.d("SpannableContent, ${text.toString()}")
                    spannedText.getSpans(0, text!!.length, InlineLatexSpan::class.java).forEach { inlineLatexSpan ->
                        Timber.d("SpannableSpan, Found InlineLatexSpan: $inlineLatexSpan")
                        val start = spannedText.getSpanStart(inlineLatexSpan)
                        val end = spannedText.getSpanEnd(inlineLatexSpan)
                        val layoutLine = layout.getLineForOffset(start)
                        val lineBaseline = layout.getLineBaseline(layoutLine) // Get the baseline for that line
                        val lineHeight = layout.getLineBottom(layoutLine) - layout.getLineTop(layoutLine) // Get the height of the line

                        val x = layout.getPrimaryHorizontal(start)
                        Timber.d("SpannableSpan, x: $x")
                        val y = layout.getLineBaseline(layoutLine).toFloat()
                        Timber.d("SpannableSpan, y: $y")

                        val adjustedY = lineBaseline - 200 - lineHeight / 2 // Center the LaTeX span vertically in the line
                        val adjustedX = x + paddingLeft
                        //canvas.drawText("Hello, world!", 5F, 5F, paint)
                        // Use the render object to draw the LaTeX
                        inlineLatexSpan.draw(canvas, text, start, end, adjustedX, adjustedY.toInt(), adjustedY.toInt(), adjustedY.toInt() + lineHeight.toInt(), Paint())
                    }
                    //requestLayout()
                    isInlineLatexProcessed = true
                }
            } else {
                if (isInlineLatexProcessed) {
                    spannedText.getSpans(0, text!!.length, InlineLatexSpan::class.java).forEach { inlineLatexSpan ->
                        Timber.d("SpannableSpan, Found InlineLatexSpan: $inlineLatexSpan")
                        val spannableString = SpannableStringBuilder(spannedText)
                        spannableString.removeSpan(inlineLatexSpan)
                        Timber.d("spannableString = $spannableString")
                        text = spannableString
                    }
                    //requestLayout()
                    isInlineLatexProcessed = false
                }
            }
            requestLayout()
            //invalidate()
            //postInvalidate()
        }
        canvas.withTranslation(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat()) {
            highlightDrawer?.draw(canvas, text as Spanned, layout, context.resources)
        }
        super.onDraw(canvas)
    }

    fun setLinksClickable() {
        makeLinksActive()
    }

    fun setDefaultMovementMethod() {
        movementMethod = defaultMovementMethod
    }

    fun setFocus() {
        Timber.d("setFocus")
        showKeyboard()
    }

    fun enableEnterKeyDetector(
        onEnterClicked: (IntRange) -> Unit
    ) {
        setOnEditorActionListener(OnEnterActionListener(onEnter = { tv ->
            onEnterClicked.invoke(tv.selectionStart..tv.selectionEnd)
        }))
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (hasFocus() && !inReadMode) return super.onTouchEvent(event)
        return editorTouchProcessor.process(this, event)
    }

    override fun onDragEvent(event: DragEvent?): Boolean {
        return if (ignoreDragAndDrop)
            true
        else
            super.onDragEvent(event)
    }

    /**
     *  Makes all links in the TextView object active.
     */
    private fun makeLinksActive() {
        Linkify.addLinks(this, Linkify.WEB_URLS)
        movementMethod = CustomBetterLinkMovementMethod
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return WidgetState(superState, inReadMode)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val restoredState = state as? WidgetState ?: return super.onRestoreInstanceState(state)

        super.onRestoreInstanceState(restoredState.superSavedState ?: restoredState)
        inReadMode = restoredState.isInReadMode
    }

    companion object {
        val DEFAULT_INPUT_WIDGET_ACTION = BlockView.InputAction.NewLine
    }
}

@Parcelize
private class WidgetState(
    val superSavedState: Parcelable?,
    val isInReadMode: Boolean,
) : Parcelable