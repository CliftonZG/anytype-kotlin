package com.anytypeio.anytype.core_ui.features.editor.holders.other

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.N_MR1
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.TextView
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
import com.anytypeio.anytype.core_ui.widgets.LatexInputWidget
import com.anytypeio.anytype.core_utils.ext.dimen
import com.anytypeio.anytype.core_utils.ext.imm
import com.anytypeio.anytype.presentation.editor.editor.listener.ListenerType
import com.anytypeio.anytype.presentation.editor.editor.model.BlockView
import com.anytypeio.anytype.presentation.editor.editor.model.Focusable
import com.judemanutd.katexview.KatexView
import timber.log.Timber

class Latex(
    val binding: ItemBlockLatexEditorBinding
) : BlockViewHolder(binding.root),
    BlockViewHolder.DragAndDropHolder,
    BlockViewHolder.IndentableHolder,
    DecoratableViewHolder
{

    val menu: TextView
        get() = binding.templateMenu
    val root: View
        get() = itemView
    val content: LatexInputWidget
        get() = binding.snippet
    val contentContainer: HorizontalScrollView
        get() = binding.snippetContainer
    val latexView: KatexView = binding.latexView

    val editorTouchProcessor = EditorTouchProcessor(
        fallback = { e ->
            Log.v("LatexClass", "editorTouchProcessor, fallback: $e")
            itemView.onTouchEvent(e)
        }
    )

    override val decoratableContainer: EditorDecorationContainer
        get() = binding.decorationContainer

    init {
        latexView.setTextColor(
            itemView.resources.getColor(R.color.text_primary, null)
        )
        content.setOnTouchListener { v, e ->
            Timber.d("LatexClass, setOnTouchListener, v: $v, e: $e")
            editorTouchProcessor.process(v, e)
        }
        setLatex("Here your Equation will be rendered with TEX. Click to Edit.")
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


        if (item.mode == BlockView.Mode.READ) {
            Log.v("LatexClass", "bind, readMode")
            content.setText(item.text)
            content.enableReadMode()
            select(item)
            setBackgroundColor(item.background)
        } else {
            Log.v("LatexClass", "bind, editMode")
            content.enableEditMode()

            select(item)

            content.clearTextWatchers()

            content.setText(item.text)

            setBackgroundColor(item.background)

            setCursor(item)
            setFocus(item)

            content.addTextChangedListener(
                DefaultTextWatcher { text ->
                    Log.v("LatexClass", "content.addTextChangedListener, text: $text")
                    onTextChanged(item.id, text)
                    setLatex(text.toString())
                }
            )

            content.setOnFocusChangeListener { _, focused ->
                Log.v("LatexClass", "content, setOnFocusChangeListener, is focused: $focused")
                clicked(ListenerType.Latex.EnableEditMode(true))
                item.isFocused = focused
                onFocusChanged(item.id, focused)
                if (Build.VERSION.SDK_INT == N || Build.VERSION.SDK_INT == N_MR1) {
                    if (focused) {
                        Log.v("LatexClass", "setOnFocusChangeListener, is focused")
                        imm().showSoftInput(content, InputMethodManager.SHOW_FORCED)
                    } else Log.v("LatexClass", "setOnFocusChangeListener, is not focused")
                }
                if (!focused) {
                    Log.v("LatexClass", "hideExpandableView")
                    hideExpandableView()
                }
            }

            // TODO add backspace detector

            content.selectionWatcher = { onSelectionChanged(item.id, it) }
        }

        content.setOnClickListener {
            Log.v("LatexClass", "setOnClickListener, is focused")
            clicked(ListenerType.Latex.EnableEditMode(true))
            onTextInputClicked(item.id)
            if (Build.VERSION.SDK_INT == N || Build.VERSION.SDK_INT == N_MR1) {
                content.context.imm().showSoftInput(content, InputMethodManager.SHOW_FORCED)
            }
        }

        menu.setOnClickListener {
            clicked(ListenerType.Latex.SelectLatexTemplate(item.id))
        }

        latexView.setOnClickListener {
            Log.v("LatexClass", "latexView.setOnClickListener")
            clicked(ListenerType.Latex.EnableEditMode(true))
            showExpandableView()
            content.requestFocus()
            val lastCharPosition: Int? = content.getText()?.length
            if (lastCharPosition != null) content.setSelection(lastCharPosition) // Set cursor to last char
        }

        latexView.setOnFocusChangeListener { _, focused ->
            Log.v("LatexClass", "latexView, setOnFocusChangeListener, is focused: $focused")
            clicked(ListenerType.Latex.EnableEditMode(true))
            item.isFocused = focused
            onFocusChanged(item.id, focused)
            if (Build.VERSION.SDK_INT == N || Build.VERSION.SDK_INT == N_MR1) {
                if (focused) {
                    Log.v("LatexClass", "setOnFocusChangeListener, is focused")
                    imm().showSoftInput(content, InputMethodManager.SHOW_FORCED)
                } else Log.v("LatexClass", "setOnFocusChangeListener, is not focused")
            }
            if (!focused) {
                Log.v("LatexClass", "hideExpandableView")
                hideExpandableView()
            }
        }
    }

    private fun setIsSelected(item: BlockView.Latex) {
        Log.v("LatexClass", "is selected")
        binding.selected.isSelected = item.isSelected
    }

    private fun setLatex(latex: String) {
        Log.i("LatexClass", "setLatex triggered")
        val encode = "$$ $latex $$"
        try {
            binding.latexView.setText(encode)
        } catch (e: Exception) {
            Timber.e(e, "Error while setting latex text")
        }
    }

    private fun setBackground(backgroundColor: ThemeColor) {
        itemView.setBlockBackgroundColor(backgroundColor)
    }

    fun processChangePayload(
        payloads: List<BlockViewDiffUtil.Payload>,
        item: BlockView.Latex,
        onTextChanged: (String, Editable) -> Unit,
        onSelectionChanged: (String, IntRange) -> Unit,
    ) = payloads.forEach { payload ->
        Timber.d("Processing $payload for new view:\n$item")

        Log.v("LatexClass", "payloads: $payloads")
        Log.v("LatexClass", "payloads: $payload")

        if (payload.textChanged()) {
            Log.v("latex", "payload: latex block changed!! {text changed}")
            content.pauseTextWatchers {
                content.setText(item.text)
                setLatex(item.text)
            }
        }

        if (payload.isTextChanged) {
            Log.v("LatexClass", "payload: latex block changed!! {istext changed}")
            content.pauseTextWatchers {
                content.setText(item.text)
                setLatex(item.text)
            }
        }

        if (payload.latexChanged()) {
            Log.v("LatexClass", "payload: latex block changed!! {latex changed}")
            content.pauseTextWatchers {
                content.setText(item.text)
                setLatex(item.text)
            }
        }

        if (payload.isLatexChanged) {
            Log.v("LatexClass", "payload: latex block changed!! {isLatex changed}")
            content.pauseTextWatchers {
                content.setText(item.text)
                setLatex(item.text)
            }
        }

        if (payload.readWriteModeChanged()) {
            Timber.d("LatexClass, readWriteModeChanged")
            content.pauseTextWatchers {
                if (item.mode == BlockView.Mode.EDIT) {
                    content.apply {
                        clearTextWatchers()
                        addTextChangedListener(
                            DefaultTextWatcher { text -> onTextChanged(item.id, text) }
                        )
                        selectionWatcher = { onSelectionChanged(item.id, it) }
                    }
                    content.enableEditMode()
                } else {
                    content.enableReadMode()
                }
            }
        }

        if (payload.selectionChanged()) {
            Log.v("LatexClass", "selection changed")
            select(item)
        }

        if (payload.focusChanged()) {
            Log.v("LatexClass", "focus changed")
            setFocus(item)
        }

        if (payload.isCursorChanged) {
            Log.v("LatexClass", "cursor changed")
            setCursor(item = item)
        }

        if (payload.backgroundColorChanged()) {
            Timber.d("LatexClass, backgroundColorChanged")
            setBackgroundColor(item.background)
        }

        if (payload.isIndentChanged) {
            Timber.d("LatexClass, isIndentChanged")
            indentize(item)
        }

        if (payload.isLatexChanged) setLatex(item.latex)
        if (payload.isSelectionChanged) setIsSelected(item)
        if (payload.isBackgroundColorChanged) setBackground(item.background)
    }

    fun select(item: BlockView.Selectable) {
        Timber.d("LatexClass, Select, item: $item")
        binding.selected.isSelected = item.isSelected
    }

    fun setFocus(item: Focusable) {
        if (item.isFocused) {
            Log.v("LatexClass", "setting focus")
            focus()
        } else {
            Log.v("LatexClass", "clearing focus")
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

    private fun setBackgroundColor(background: ThemeColor) {
        if (background != ThemeColor.DEFAULT) {
            (binding.content.background as? ColorDrawable)?.color = root.resources.veryLight(background, 0)
        } else {
            val defaultBackgroundColor =
                content.context.resources.getColor(R.color.shape_tertiary, null)
            (binding.content.background as? ColorDrawable)?.color = defaultBackgroundColor
        }
    }

    override fun indentize(item: BlockView.Indentable) {
        binding.latexView.updatePadding(
            left = dimen(R.dimen.default_document_content_padding_start) + item.indent * dimen(R.dimen.indent)
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
        if (latexView.getVisibility() == View.VISIBLE) {
            contentContainer.setVisibility(View.VISIBLE)
            menu.setVisibility(View.VISIBLE)
        }
    }

    private fun hideExpandableView() {
        val currentLatexViewText = latexView.getText()
        Log.v("LatexClass", "currentLatexViewText: $currentLatexViewText")
        if (latexView.getVisibility() == View.VISIBLE && currentLatexViewText != "" && currentLatexViewText != "$$  $$" && currentLatexViewText != "$$$$") {
            contentContainer.setVisibility(View.GONE)
            menu.setVisibility(View.GONE)
        } else {
            Log.v("LatexClass", "skipped hideExpandableView")
        }
    }
}