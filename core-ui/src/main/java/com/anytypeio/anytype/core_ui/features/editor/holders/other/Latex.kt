package com.anytypeio.anytype.core_ui.features.editor.holders.other

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.anytypeio.anytype.core_models.ThemeColor
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_ui.databinding.ItemBlockLatexEditorBinding
import com.anytypeio.anytype.core_ui.extensions.setBlockBackgroundColor
import com.anytypeio.anytype.core_ui.extensions.veryLight
import com.anytypeio.anytype.core_ui.features.editor.BlockViewDiffUtil
import com.anytypeio.anytype.core_ui.features.editor.BlockViewHolder
import com.anytypeio.anytype.core_ui.features.editor.EditorTouchProcessor
import com.anytypeio.anytype.core_ui.features.editor.decoration.DecoratableViewHolder
import com.anytypeio.anytype.core_ui.features.editor.decoration.EditorDecorationContainer
import com.anytypeio.anytype.core_ui.tools.DefaultTextWatcher
import com.anytypeio.anytype.core_ui.views.TexView
import com.anytypeio.anytype.core_ui.widgets.LatexInputWidget
import com.anytypeio.anytype.core_utils.ext.dimen
import com.anytypeio.anytype.core_utils.ext.imm
import com.anytypeio.anytype.core_utils.text.BackspaceKeyDetector
import com.anytypeio.anytype.presentation.editor.editor.listener.ListenerType
import com.anytypeio.anytype.presentation.editor.editor.model.BlockView
import com.anytypeio.anytype.presentation.editor.editor.model.Focusable
import timber.log.Timber

class Latex(
    val binding: ItemBlockLatexEditorBinding
) : BlockViewHolder(binding.root),
    BlockViewHolder.DragAndDropHolder,
    BlockViewHolder.IndentableHolder,
    DecoratableViewHolder {

    // Views
    val menu: TextView = binding.templateMenu
    private val root: View = itemView
    val content: LatexInputWidget = binding.snippet
    private val inputContainer: HorizontalScrollView = binding.snippetContainer
    private val texView: TexView = binding.tex
    private val emptyTexView: TextView = binding.emptyTex
    private val frame: FrameLayout = binding.content

    // Constants
    private val emptyTexText = binding.root.context.getString(R.string.blockEmbedLatexEmpty)
    private val spannable = SpannableString(emptyTexText)
    private val start = emptyTexText.indexOf("[icon]")
    private val end = start + "[icon]".length
    private val defaultBackgroundColor = content.context.resources.getColor(R.color.shape_tertiary, null)
    private val texLogo: Drawable? = ContextCompat.getDrawable(binding.root.context, R.drawable.tex)

    private var previousInput : String = ""

    // Editor touch processor
    val editorTouchProcessor = EditorTouchProcessor(
        fallback = { e ->
            Timber.v("LatexClass, aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            Timber.v("LatexClass, editorTouchProcessor, fallback: $e")
            itemView.onTouchEvent(e)
        }
    )

    override val decoratableContainer: EditorDecorationContainer
        get() = binding.decorationContainer

    fun select(item: BlockView.Selectable) {
        Timber.d("LatexClass, Select, item: $item")
        binding.selected.isSelected = item.isSelected
    }

    fun setFocus(item: Focusable) {
        if (item.isFocused) {
            Timber.d("LatexClass, setting focus")
            focus()
        } else {
            Timber.d("LatexClass, clearing focus")
            content.clearFocus()
        }
    }

    fun focus() {
        Timber.d("Requesting focus")
        content.apply {
            post {
                if (!hasFocus()) {
                    if (requestFocus()) {
                        context.imm().showSoftInput(this, InputMethodManager.SHOW_FORCED)
                        menu.visibility = View.VISIBLE
                    } else {
                        Timber.d("Couldn't gain focus")
                    }
                } else {
                    Timber.d("Already had focus")
                }
            }
        }
    }

    private fun setCursor(item: BlockView.Latex) {
        if (item.isFocused) {
            Timber.d("Setting cursor: $item")
            item.cursor?.let {
                val length = content.text?.length ?: 0
                if (it in 0..length) {
                    content.setSelection(it)
                }
            }
        }
    }

    init {
        // Set touch listener for latex content
        root.setOnTouchListener { v, e ->
            Timber.d("LatexClass, content setOnTouchListener, v: $v, e: $e")
            editorTouchProcessor.process(v, e)
        }
        // Initialize tex view
        texView.setColor(itemView.resources.getColor(R.color.text_primary, null))

        // Set up texLogo with ImageSpan
        texLogo?.let { logo ->
            logo.setBounds(0, 0, logo.intrinsicWidth, logo.intrinsicHeight)
            val imageSpan = ImageSpan(logo, ImageSpan.ALIGN_BASELINE)
            spannable.setSpan(imageSpan, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        emptyTexView.text = spannable
    }

    fun bind(
        item: BlockView.Latex,
        onTextChanged: (String, Editable) -> Unit,
        onSelectionChanged: (String, IntRange) -> Unit,
        onFocusChanged: (String, Boolean) -> Unit,
        clicked: (ListenerType) -> Unit,
        onTextInputClicked: (String) -> Unit
    ) {
        indentize(item)
        setIsSelected(item)
        setLatex(item.text)
        setBackground(item.background)

        when (item.mode) {
            BlockView.Mode.READ -> {
                content.setText(item.text)
                content.enableReadMode()
                select(item)
                setBackgroundColor(item.background)
            }
            BlockView.Mode.EDIT -> {
                content.enableEditMode()
                select(item)
                content.clearTextWatchers()

                if (item.text.isBlank()) {
                    content.setText(item.text)
                    texView.visibility = View.GONE
                    emptyTexView.visibility = View.VISIBLE
                } else {
                    content.setText(item.text)
                    setLatex(item.text)
                    emptyTexView.visibility = View.GONE
                    texView.visibility = View.VISIBLE
                }

                menu.visibility = View.VISIBLE
                setBackgroundColor(item.background)
                setCursor(item)
                setFocus(item)

                content.addTextChangedListener(DefaultTextWatcher { text ->
                    Timber.v("LatexClass, content.addTextChangedListener, text: $text")
                    onTextChanged(item.id, text)
                    setLatex(text.toString())
                })

                content.setOnFocusChangeListener { _, focused ->
                    Timber.v("LatexClass, content, setOnFocusChangeListener, is focused: $focused")
                    item.isFocused = focused
                    onFocusChanged(item.id, focused)
                    handleSoftInput(focused)
                }
                content.selectionWatcher = {
                    onSelectionChanged(item.id, it)
                }
            }
        }

        // Click listeners
        //setupClickListeners(item, clicked, onTextInputClicked)

        content.setOnClickListener {
            Timber.d("LatexClass, onTextInputClicked")
            onTextInputClicked(item.id)
            handleSoftInput(true)
        }

        menu.setOnClickListener {
            clicked(ListenerType.Latex.SelectLatexTemplate(item.id))
        }
    }

    // Helper functions
    private fun setIsSelected(item: BlockView.Latex) {
        Timber.v("LatexClass, is selected")
        binding.selected.isSelected = item.isSelected
        showExpandableView()
    }

    private fun setLatex(latex: String) {
        Timber.i("LatexClass, setLatex triggered")
        Timber.i("LatexClass, previousInput: $previousInput")
        Timber.i("LatexClass, currentInput: $latex")
        if (previousInput != latex && latex.isNotBlank()) { // Avoid parsing LaTeX, it is expensive
            texView.parseLaTeX(latex)
            previousInput = latex
        } else {
            texView.setLaTeX()
        }
        if (latex.isBlank()) {
            texView.visibility = View.GONE
            emptyTexView.visibility = View.VISIBLE
            menu.visibility = View.VISIBLE
        } else {
            emptyTexView.visibility = View.GONE
            texView.visibility = View.VISIBLE
        }
    }

    private fun setBackground(backgroundColor: ThemeColor) {
        itemView.setBlockBackgroundColor(backgroundColor)
    }

    private fun setBackgroundColor(background: ThemeColor) {
        val color = if (background != ThemeColor.DEFAULT) {
            root.resources.veryLight(background, 0)
        } else {
            defaultBackgroundColor
        }
        (binding.content.background as? ColorDrawable)?.color = color
    }

    private fun handleSoftInput(focused: Boolean) {
        if (focused) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N || Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                Timber.v("LatexClass, show soft input")
                imm().showSoftInput(content, InputMethodManager.SHOW_FORCED)
            }
        }
        else {
            Timber.v("LatexClass, hideExpandableView")
            hideExpandableView()
        }
    }

    private fun setupClickListeners(item: BlockView.Latex, clicked: (ListenerType) -> Unit, onTextInputClicked: (String) -> Unit) {
        val clickListener = View.OnClickListener {
            Timber.v("LatexClass, setOnClickListener, is focused")
            onTextInputClicked(item.id)
            handleSoftInput(true)
            menu.visibility = View.VISIBLE
        }

        content.setOnClickListener(clickListener)
        menu.setOnClickListener { clicked(ListenerType.Latex.SelectLatexTemplate(item.id)) }
        frame.setOnClickListener(clickListener)
        emptyTexView.setOnClickListener(clickListener)
        texView.setOnClickListener(clickListener)
    }

    override fun indentize(item: BlockView.Indentable) {
        binding.tex.updatePadding(
            left = dimen(R.dimen.default_document_content_padding_start) + item.indent * dimen(R.dimen.indent)
        )
    }

    fun enableBackspaceDetector(
        onEmptyBlockBackspaceClicked: () -> Unit,
        onNonEmptyBlockBackspaceClicked: () -> Unit
    ) {
        content.setOnKeyListener(
            BackspaceKeyDetector {
                if (content.text.toString().isEmpty()) {
                    onEmptyBlockBackspaceClicked()
                } else {
                    onNonEmptyBlockBackspaceClicked()
                }
            }
        )
    }

    override fun applyDecorations(decorations: List<BlockView.Decoration>) {
        decoratableContainer.decorate(decorations) { rect ->
            binding.content.updateLayoutParams<FrameLayout.LayoutParams> {
                marginStart = rect.left
                marginEnd = rect.right
                bottomMargin = rect.bottom
            }
        }
    }

    private fun showExpandableView() {
        if (texView.visibility == View.VISIBLE || emptyTexView.visibility == View.VISIBLE) {
            inputContainer.visibility = View.VISIBLE
            menu.visibility = View.VISIBLE
            content.visibility = View.VISIBLE
        }
    }

    private fun hideExpandableView() {
        val currentLatexViewText = content.text.toString()
        Timber.v("LatexClass, currentLatexViewText: $currentLatexViewText")
        if (texView.visibility == View.VISIBLE && currentLatexViewText.isNotEmpty() && texView.render != null) {
            inputContainer.visibility = View.GONE
            menu.visibility = View.GONE
        } else if (emptyTexView.visibility == View.VISIBLE) {
            inputContainer.visibility = View.VISIBLE
            menu.visibility = View.GONE
            content.visibility = View.GONE
        }
    }

    fun processChangePayload(
        payloads: List<BlockViewDiffUtil.Payload>,
        item: BlockView.Latex,
        onTextChanged: (String, Editable) -> Unit,
        onSelectionChanged: (String, IntRange) -> Unit
    ) {
        payloads.forEach { payload ->
            Timber.d("Processing $payload for new view:\n$item")

            when {
                payload.textChanged() -> {
                    Timber.v("latex block changed! {text changed}")
                    content.pauseTextWatchers {
                        content.setText(item.text)
                        setLatex(item.text)
                    }
                }
                payload.readWriteModeChanged() -> {
                    Timber.d("LatexClass, readWriteModeChanged")
                    content.pauseTextWatchers {
                        if (item.mode == BlockView.Mode.EDIT) {
                            content.apply {
                                clearTextWatchers()
                                addTextChangedListener(DefaultTextWatcher { text -> onTextChanged(item.id, text) })
                                selectionWatcher = { onSelectionChanged(item.id, it) }
                            }
                            content.enableEditMode()
                        } else {
                            content.enableReadMode()
                        }
                    }
                }
                payload.backgroundColorChanged() -> {
                    Timber.d("LatexClass, backgroundColorChanged")
                    setBackgroundColor(item.background)
                }
                payload.focusChanged() -> {
                    Timber.d("LatexClass", "focus changed")
                    setFocus(item)
                }
                payload.isCursorChanged -> {
                    Timber.d("LatexClass", "cursor changed")
                    setCursor(item = item)
                }
                payload.isIndentChanged -> {
                    Timber.d("LatexClass, isIndentChanged")
                    indentize(item)
                }
                payload.selectionChanged() -> {
                    Timber.d("LatexClass", "selection changed")
                    select(item)
                }
                // Handle other payload types...
            }
        }
    }
}
