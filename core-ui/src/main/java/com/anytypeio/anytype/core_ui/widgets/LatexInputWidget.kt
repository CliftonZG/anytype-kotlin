package com.anytypeio.anytype.core_ui.widgets

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.DragEvent
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import com.anytypeio.anytype.core_ui.features.editor.EditorTouchProcessor
import com.anytypeio.anytype.core_ui.tools.TextInputTextWatcher
import com.anytypeio.anytype.core_ui.widgets.text.MonospaceTabTextWatcher
import com.anytypeio.anytype.library_syntax_highlighter.Syntax
import com.anytypeio.anytype.library_syntax_highlighter.SyntaxHighlighter
import com.anytypeio.anytype.library_syntax_highlighter.SyntaxTextWatcher
import com.anytypeio.anytype.library_syntax_highlighter.Syntaxes
import com.anytypeio.anytype.library_syntax_highlighter.obtainGenericSyntaxRules
import com.anytypeio.anytype.library_syntax_highlighter.obtainSyntaxRules
import timber.log.Timber

class LatexInputWidget : AppCompatEditText{

    private val watchers: MutableList<TextInputTextWatcher> = mutableListOf()

    var selectionWatcher: ((IntRange) -> Unit)? = null

    val editorTouchProcessor by lazy {
        EditorTouchProcessor(
            fallback = { e -> super.onTouchEvent(e) }
        )
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
        setOnLongClickListener { view -> view != null && !view.hasFocus() }
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        setup()
        setOnLongClickListener { view -> view != null && !view.hasFocus() }
    }

    private fun setup() {
        enableEditMode()
        super.addTextChangedListener(MonospaceTabTextWatcher(paint.measureText(MEASURING_CHAR)))
    }

    fun enableEditMode() {
        Timber.d("LatexInputWidget, enableEditMode")
        setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        setHorizontallyScrolling(false)
        maxLines = Integer.MAX_VALUE
        setTextIsSelectable(true)
    }

    fun enableReadMode() {
        Timber.d("LatexInputWidget, enableReadMode")
        setRawInputType(InputType.TYPE_NULL)
        maxLines = Integer.MAX_VALUE
        setHorizontallyScrolling(false)
        setTextIsSelectable(false)
    }

    override fun addTextChangedListener(watcher: TextWatcher) {
        if (watcher is TextInputTextWatcher) {
            watchers.add(watcher)
        }
        super.addTextChangedListener(watcher)
    }

    override fun removeTextChangedListener(watcher: TextWatcher) {
        if (watcher is TextInputTextWatcher) {
            watchers.remove(watcher)
        }
        super.removeTextChangedListener(watcher)
    }

    fun clearTextWatchers() {
        watchers.forEach { super.removeTextChangedListener(it) }
        watchers.clear()
    }

    fun pauseTextWatchers(block: () -> Unit) = synchronized(this) {
        lockTextWatchers()
        block()
        unlockTextWatchers()
    }

    private fun lockTextWatchers() {
        watchers.forEach { watcher ->
            watcher.lock()
        }
    }

    private fun unlockTextWatchers() {
        watchers.forEach { watcher ->
            watcher.unlock()
        }
    }

    /**
     * Send selection event only for blocks in focus state
     */
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (isFocused) {
            Timber.Forest.d("New selection: $selStart - $selEnd")
            selectionWatcher?.invoke(selStart..selEnd)
        }
        super.onSelectionChanged(selStart, selEnd)
    }

    override fun onDragEvent(event: DragEvent?): Boolean = true

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Timber.d("LatexInputWidget, onTouchEvent")
        if (hasFocus()) return super.onTouchEvent(event)
        return editorTouchProcessor.process(this, event)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        Timber.d("LatexInputWidget, onFocusChanged")
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }
}

private const val MEASURING_CHAR = " "