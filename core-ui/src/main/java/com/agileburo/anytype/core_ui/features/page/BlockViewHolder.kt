package com.agileburo.anytype.core_ui.features.page

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.BufferType
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.agileburo.anytype.core_ui.BuildConfig
import com.agileburo.anytype.core_ui.R
import com.agileburo.anytype.core_ui.common.Markup
import com.agileburo.anytype.core_ui.common.isLinksPresent
import com.agileburo.anytype.core_ui.common.toSpannable
import com.agileburo.anytype.core_ui.extensions.color
import com.agileburo.anytype.core_ui.extensions.invisible
import com.agileburo.anytype.core_ui.extensions.tint
import com.agileburo.anytype.core_ui.extensions.visible
import com.agileburo.anytype.core_ui.features.page.BlockViewDiffUtil.Companion.NUMBER_CHANGED
import com.agileburo.anytype.core_ui.features.page.BlockViewDiffUtil.Companion.SELECTION_CHANGED
import com.agileburo.anytype.core_ui.features.page.BlockViewDiffUtil.Companion.TEXT_CHANGED
import com.agileburo.anytype.core_ui.features.page.BlockViewDiffUtil.Companion.TOGGLE_EMPTY_STATE_CHANGED
import com.agileburo.anytype.core_ui.features.page.BlockViewDiffUtil.Payload
import com.agileburo.anytype.core_ui.tools.DefaultSpannableFactory
import com.agileburo.anytype.core_ui.tools.DefaultTextWatcher
import com.agileburo.anytype.core_ui.widgets.text.EditorLongClickListener
import com.agileburo.anytype.core_ui.widgets.text.TextInputWidget
import com.agileburo.anytype.core_utils.const.MimeTypes
import com.agileburo.anytype.core_utils.ext.addDot
import com.agileburo.anytype.core_utils.ext.dimen
import com.agileburo.anytype.core_utils.ext.imm
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.item_block_bookmark.view.*
import kotlinx.android.synthetic.main.item_block_bookmark_error.view.*
import kotlinx.android.synthetic.main.item_block_bookmark_placeholder.view.*
import kotlinx.android.synthetic.main.item_block_bulleted.view.*
import kotlinx.android.synthetic.main.item_block_checkbox.view.*
import kotlinx.android.synthetic.main.item_block_code_snippet.view.*
import kotlinx.android.synthetic.main.item_block_contact.view.*
import kotlinx.android.synthetic.main.item_block_file.view.*
import kotlinx.android.synthetic.main.item_block_file_error.view.*
import kotlinx.android.synthetic.main.item_block_file_placeholder.view.*
import kotlinx.android.synthetic.main.item_block_file_uploading.view.*
import kotlinx.android.synthetic.main.item_block_header_one.view.*
import kotlinx.android.synthetic.main.item_block_header_three.view.*
import kotlinx.android.synthetic.main.item_block_header_two.view.*
import kotlinx.android.synthetic.main.item_block_highlight.view.*
import kotlinx.android.synthetic.main.item_block_numbered.view.*
import kotlinx.android.synthetic.main.item_block_page.view.*
import kotlinx.android.synthetic.main.item_block_picture.view.*
import kotlinx.android.synthetic.main.item_block_picture_error.view.*
import kotlinx.android.synthetic.main.item_block_picture_placeholder.view.*
import kotlinx.android.synthetic.main.item_block_picture_uploading.view.*
import kotlinx.android.synthetic.main.item_block_task.view.*
import kotlinx.android.synthetic.main.item_block_text.view.*
import kotlinx.android.synthetic.main.item_block_title.view.*
import kotlinx.android.synthetic.main.item_block_toggle.view.*
import kotlinx.android.synthetic.main.item_block_video.view.*
import kotlinx.android.synthetic.main.item_block_video_empty.view.*
import kotlinx.android.synthetic.main.item_block_video_error.view.*
import kotlinx.android.synthetic.main.item_block_video_uploading.view.*
import android.text.format.Formatter as FileSizeFormatter

/**
 * Viewholder for rendering different type of blocks (i.e its UI-models).
 * @see BlockView
 * @see BlockAdapter
 */
sealed class BlockViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    interface IndentableHolder {
        fun indentize(item: BlockView.Indentable)
    }

    class Paragraph(
        view: View,
        onMarkupActionClicked: (Markup.Type) -> Unit
    ) : BlockViewHolder(view), TextHolder, IndentableHolder {

        override val root: View = itemView
        override val content: TextInputWidget = itemView.textContent

        init {
            setup(onMarkupActionClicked)
        }

        fun bind(
            item: BlockView.Paragraph,
            onTextChanged: (String, Editable) -> Unit,
            onSelectionChanged: (String, IntRange) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {

            indentize(item)

            if (item.mode == BlockView.Mode.READ) {
                enableReadOnlyMode()
                setText(item)
                setTextColor(item)
                select(item)
            } else {
                enableEditMode()

                select(item)

                content.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                content.clearTextWatchers()

                if (item.marks.isLinksPresent()) {
                    content.setLinksClickable()
                }

                setText(item)
                setTextColor(item)
                setFocus(item)

                setupTextWatcher(onTextChanged, item)

                content.setOnFocusChangeListener { _, focused ->
                    item.focused = focused
                    onFocusChanged(item.id, focused)
                }
                content.selectionDetector = { onSelectionChanged(item.id, it) }
            }
        }

        private fun setText(item: BlockView.Paragraph) {
            content.setText(item.toSpannable(), BufferType.SPANNABLE)
        }

        private fun setTextColor(
            item: BlockView.Paragraph
        ) {
            if (item.color != null) {
                setTextColor(item.color)
            } else {
                setTextColor(content.context.color(R.color.black))
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            content.updatePadding(
                left = dimen(R.dimen.default_document_content_padding_start) + item.indent * dimen(R.dimen.indent)
            )
        }
    }

    class Title(view: View) : BlockViewHolder(view), TextHolder {

        private val icon = itemView.logo

        override val root: View = itemView
        override val content: TextInputWidget = itemView.title

        init {
            content.setSpannableFactory(DefaultSpannableFactory())
        }

        fun bind(
            item: BlockView.Title,
            onTitleTextChanged: (Editable) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onPageIconClicked: () -> Unit
        ) {
            if (item.mode == BlockView.Mode.READ) {
                enableReadOnlyMode()
                content.setText(item.text, BufferType.EDITABLE)
                icon.text = item.emoji ?: EMPTY_EMOJI
            } else {
                enableEditMode()
                focus(item.focused)
                content.setText(item.text, BufferType.EDITABLE)
                if (!item.text.isNullOrEmpty()) content.setSelection(item.text.length)
                setupTextWatcher({ _, editable -> onTitleTextChanged(editable) }, item)
                content.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                    if (hasFocus) showKeyboard()
                }
                with(icon) {
                    text = item.emoji ?: EMPTY_EMOJI
                    setOnClickListener { onPageIconClicked() }
                }
            }
        }

        private fun showKeyboard() {
            content.postDelayed(KEYBOARD_SHOW_DELAY) {
                imm().showSoftInput(content, SHOW_IMPLICIT)
            }
        }

        fun processPayloads(
            payloads: List<Payload>,
            item: BlockView.Title
        ) {
            payloads.forEach { payload ->
                if (payload.changes.contains(TEXT_CHANGED)) {
                    content.pauseTextWatchers {
                        if (content.text.toString() != item.text) {
                            content.setText(item.text, BufferType.EDITABLE)
                        }
                    }
                }
                if (payload.readWriteModeChanged()) {
                    if (item.mode == BlockView.Mode.EDIT)
                        enableEditMode()
                    else
                        enableReadOnlyMode()
                }
            }
        }

        fun focus(focused: Boolean) {
            if (focused) {
                content.requestFocus()
                showKeyboard()
            } else
                content.clearFocus()
        }

        override fun processChangePayload(
            payloads: List<Payload>, item: BlockView
        ) {
            check(item is BlockView.Title)
            payloads.forEach { payload ->
                if (payload.focusChanged())
                    focus(item.focused)
                if (payload.readWriteModeChanged()) {
                    if (item.mode == BlockView.Mode.EDIT)
                        enableEditMode()
                    else
                        enableReadOnlyMode()
                }
            }
        }

        override fun enableBackspaceDetector(
            onEmptyBlockBackspaceClicked: () -> Unit,
            onNonEmptyBlockBackspaceClicked: () -> Unit
        ) = Unit

        companion object {
            private const val EMPTY_EMOJI = ""
        }
    }

    class HeaderOne(view: View) : BlockViewHolder(view), TextHolder, IndentableHolder {

        private val header = itemView.headerOne
        override val root: View = itemView
        override val content: TextInputWidget
            get() = header

        fun bind(
            item: BlockView.HeaderOne,
            onTextChanged: (String, Editable) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {

            indentize(item)

            if (item.mode == BlockView.Mode.READ) {
                enableReadOnlyMode()
                header.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                header.setText(item.text)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }

                select(item)
            } else {

                enableEditMode()

                select(item)

                with(header) {

                    setOnLongClickListener(
                        EditorLongClickListener(
                            t = item.id,
                            click = onLongClickListener
                        )
                    )

                    clearTextWatchers()

                    setOnFocusChangeListener { _, hasFocus ->
                        onFocusChanged(item.id, hasFocus)
                    }

                    setText(item.text)

                    addTextChangedListener(
                        DefaultTextWatcher { text ->
                            onTextChanged(item.id, text)
                        }
                    )
                }

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            header.updatePadding(
                left = dimen(R.dimen.default_document_content_padding_start) + item.indent * dimen(R.dimen.indent)
            )
        }
    }

    class HeaderTwo(view: View) : BlockViewHolder(view), TextHolder, IndentableHolder {

        private val header = itemView.headerTwo
        override val content: TextInputWidget
            get() = header
        override val root: View = itemView

        fun bind(
            item: BlockView.HeaderTwo,
            onTextChanged: (String, Editable) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {

            indentize(item)

            if (item.mode == BlockView.Mode.READ) {

                enableReadOnlyMode()

                select(item)

                header.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                header.setText(item.text)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }

            } else {

                enableEditMode()

                select(item)

                header.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                header.clearTextWatchers()

                header.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                }

                header.setText(item.text)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }

                header.addTextChangedListener(
                    DefaultTextWatcher { text ->
                        onTextChanged(item.id, text)
                    }
                )
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            header.updatePadding(
                left = dimen(R.dimen.default_document_content_padding_start) + item.indent * dimen(R.dimen.indent)
            )
        }
    }

    class HeaderThree(view: View) : BlockViewHolder(view), TextHolder, IndentableHolder {

        private val header = itemView.headerThree
        override val content: TextInputWidget
            get() = header
        override val root: View = itemView

        fun bind(
            item: BlockView.HeaderThree,
            onTextChanged: (String, Editable) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {

            indentize(item)

            if (item.mode == BlockView.Mode.READ) {

                enableReadOnlyMode()

                select(item)

                header.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                header.setText(item.text)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }
            } else {

                enableEditMode()

                select(item)

                header.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                header.clearTextWatchers()

                header.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                }

                header.setText(item.text)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }

                header.addTextChangedListener(
                    DefaultTextWatcher { text ->
                        onTextChanged(item.id, text)
                    }
                )
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            header.updatePadding(
                left = dimen(R.dimen.default_document_content_padding_start) + item.indent * dimen(R.dimen.indent)
            )
        }
    }

    class Code(view: View) : BlockViewHolder(view) {

        private val code = itemView.snippet

        fun bind(item: BlockView.Code) {
            code.text = item.snippet
        }
    }

    class Checkbox(
        view: View,
        onMarkupActionClicked: (Markup.Type) -> Unit
    ) : BlockViewHolder(view), TextHolder, IndentableHolder {

        var mode = BlockView.Mode.EDIT

        private val checkbox: ImageView = itemView.checkboxIcon
        private val container = itemView.checkboxBlockContentContainer
        override val content: TextInputWidget = itemView.checkboxContent
        override val root: View = itemView

        init {
            setup(onMarkupActionClicked)
        }

        fun bind(
            item: BlockView.Checkbox,
            onTextChanged: (String, Editable) -> Unit,
            onCheckboxClicked: (String) -> Unit,
            onSelectionChanged: (String, IntRange) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {
            indentize(item)

            if (item.mode == BlockView.Mode.READ) {
                enableReadOnlyMode()

                select(item)

                updateTextColor(
                    context = itemView.context,
                    view = content,
                    isSelected = checkbox.isActivated
                )
                checkbox.isActivated = item.isChecked
                if (item.marks.isNotEmpty())
                    content.setText(item.toSpannable(), BufferType.SPANNABLE)
                else
                    content.setText(item.text)
            } else {

                enableEditMode()

                select(item)

                content.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                content.clearTextWatchers()
                checkbox.isActivated = item.isChecked

                updateTextColor(
                    context = itemView.context,
                    view = content,
                    isSelected = checkbox.isActivated
                )

                if (item.marks.isLinksPresent()) {
                    content.setLinksClickable()
                }

                if (item.marks.isNotEmpty())
                    content.setText(item.toSpannable(), BufferType.SPANNABLE)
                else
                    content.setText(item.text)

                setFocus(item)

                checkbox.setOnClickListener {
                    if (mode == BlockView.Mode.EDIT) {
                        checkbox.isActivated = !checkbox.isActivated
                        updateTextColor(
                            context = itemView.context,
                            view = content,
                            isSelected = checkbox.isActivated
                        )
                        onCheckboxClicked(item.id)
                    }
                }

                content.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                }

                content.addTextChangedListener(
                    DefaultTextWatcher { text ->
                        onTextChanged(item.id, text)
                    }
                )

                content.selectionDetector = { onSelectionChanged(item.id, it) }
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            checkbox.updatePadding(left = item.indent * dimen(R.dimen.indent))
        }

        override fun enableEditMode() {
            super.enableEditMode()
            mode = BlockView.Mode.EDIT
        }

        override fun enableReadOnlyMode() {
            super.enableReadOnlyMode()
            mode = BlockView.Mode.READ
        }

        override fun select(item: BlockView.Selectable) {
            container.isSelected = item.isSelected
        }

        private fun updateTextColor(context: Context, view: TextView, isSelected: Boolean) =
            view.setTextColor(
                context.color(
                    if (isSelected) R.color.grey_50 else R.color.black
                )
            )
    }

    class Task(view: View) : BlockViewHolder(view) {

        private val checkbox = itemView.taskIcon
        private val content = itemView.taskContent

        fun bind(item: BlockView.Task) {
            checkbox.isSelected = item.checked
            content.text = item.text
        }
    }

    class Bulleted(
        view: View,
        onMarkupActionClicked: (Markup.Type) -> Unit
    ) : BlockViewHolder(view), TextHolder, IndentableHolder {

        private val indent = itemView.bulletIndent
        private val bullet = itemView.bullet
        private val container = itemView.bulletBlockContainer
        override val content: TextInputWidget = itemView.bulletedListContent
        override val root: View = itemView

        init {
            setup(onMarkupActionClicked)
        }

        fun bind(
            item: BlockView.Bulleted,
            onTextChanged: (String, Editable) -> Unit,
            onSelectionChanged: (String, IntRange) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {
            indentize(item)

            if (item.mode == BlockView.Mode.READ) {

                enableReadOnlyMode()

                select(item)

                if (item.marks.isNotEmpty())
                    content.setText(item.toSpannable(), BufferType.SPANNABLE)
                else
                    content.setText(item.text)

                if (item.color != null)
                    setTextColor(item.color)
                else
                    setTextColor(content.context.color(R.color.black))

            } else {

                enableEditMode()

                select(item)

                content.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                content.clearTextWatchers()

                if (item.marks.isLinksPresent()) {
                    content.setLinksClickable()
                }

                if (item.marks.isNotEmpty())
                    content.setText(item.toSpannable(), BufferType.SPANNABLE)
                else
                    content.setText(item.text)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }

                setFocus(item)

                content.addTextChangedListener(
                    DefaultTextWatcher { text ->
                        onTextChanged(item.id, text)
                    }
                )

                content.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                }

                content.selectionDetector = { onSelectionChanged(item.id, it) }
            }
        }

        override fun setTextColor(color: String) {
            super.setTextColor(color)
            bullet.tint(Color.parseColor(color))
        }

        override fun setTextColor(color: Int) {
            super.setTextColor(color)
            bullet.tint(content.context.color(R.color.black))
        }

        override fun indentize(item: BlockView.Indentable) {
            indent.updateLayoutParams { width = item.indent * dimen(R.dimen.indent) }
        }

        override fun select(item: BlockView.Selectable) {
            container.isSelected = item.isSelected
        }
    }

    class Numbered(
        view: View,
        onMarkupActionClicked: (Markup.Type) -> Unit
    ) : BlockViewHolder(view), TextHolder, IndentableHolder {

        private val container = itemView.numberedBlockContentContainer
        private val number = itemView.number
        override val content: TextInputWidget = itemView.numberedListContent
        override val root: View = itemView

        init {
            setup(onMarkupActionClicked)
        }

        fun bind(
            item: BlockView.Numbered,
            onTextChanged: (String, Editable) -> Unit,
            onSelectionChanged: (String, IntRange) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {
            indentize(item)

            if (item.mode == BlockView.Mode.READ) {
                enableReadOnlyMode()

                select(item)

                number.gravity = when (item.number) {
                    in 1..19 -> Gravity.CENTER_HORIZONTAL
                    else -> Gravity.START
                }

                number.text = item.number.addDot()

                content.setText(item.toSpannable(), BufferType.SPANNABLE)

                if (item.color != null)
                    setTextColor(item.color)
                else
                    setTextColor(content.context.color(R.color.black))
            } else {

                enableEditMode()

                select(item)

                content.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                content.clearTextWatchers()
                indentize(item)
                number.gravity = when (item.number) {
                    in 1..19 -> Gravity.CENTER_HORIZONTAL
                    else -> Gravity.START
                }
                number.text = item.number.addDot()

                content.setText(item.toSpannable(), BufferType.SPANNABLE)

                if (item.color != null)
                    setTextColor(item.color)
                else
                    setTextColor(content.context.color(R.color.black))

                setFocus(item)

                content.addTextChangedListener(
                    DefaultTextWatcher { text ->
                        onTextChanged(item.id, text)
                    }
                )

                content.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                }

                content.selectionDetector = { onSelectionChanged(item.id, it) }
            }
        }

        override fun processChangePayload(payloads: List<Payload>, item: BlockView) {
            super.processChangePayload(payloads, item)
            payloads.forEach { payload ->
                if (payload.changes.contains(NUMBER_CHANGED))
                    number.text = "${(item as BlockView.Numbered).number}"
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            number.updateLayoutParams<LinearLayout.LayoutParams> {
                setMargins(
                    item.indent * dimen(R.dimen.indent),
                    0,
                    0,
                    0
                )
            }
        }

        override fun select(item: BlockView.Selectable) {
            container.isSelected = item.isSelected
        }
    }

    class Toggle(
        view: View,
        onMarkupActionClicked: (Markup.Type) -> Unit
    ) : BlockViewHolder(view), TextHolder, IndentableHolder {

        private var mode = BlockView.Mode.EDIT

        private val toggle = itemView.toggle
        private val line = itemView.guideline
        private val placeholder = itemView.togglePlaceholder
        private val container = itemView.toolbarBlockContentContainer
        override val content: TextInputWidget = itemView.toggleContent
        override val root: View = itemView

        init {
            setup(onMarkupActionClicked)
        }

        fun bind(
            item: BlockView.Toggle,
            onTextChanged: (String, Editable) -> Unit,
            onSelectionChanged: (String, IntRange) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit,
            onToggleClicked: (String) -> Unit,
            onTogglePlaceholderClicked: (String) -> Unit,
            onLongClickListener: (String) -> Unit
        ) {

            indentize(item)

            if (item.mode == BlockView.Mode.READ) {

                enableReadOnlyMode()

                select(item)

                content.setText(item.toSpannable(), BufferType.SPANNABLE)

                if (item.color != null)
                    setTextColor(item.color)
                else
                    setTextColor(content.context.color(R.color.black))

                placeholder.isVisible = false

                toggle.apply {
                    rotation = if (item.toggled) EXPANDED_ROTATION else COLLAPSED_ROTATION
                }
            } else {

                enableEditMode()

                select(item)

                content.setOnLongClickListener(
                    EditorLongClickListener(
                        t = item.id,
                        click = onLongClickListener
                    )
                )

                content.clearTextWatchers()
                content.setText(item.toSpannable(), BufferType.SPANNABLE)

                if (item.color != null) {
                    setTextColor(item.color)
                } else {
                    setTextColor(content.context.color(R.color.black))
                }

                setFocus(item)

                setupTextWatcher(onTextChanged, item)

                content.setOnFocusChangeListener { _, focused ->
                    item.focused = focused
                    onFocusChanged(item.id, focused)
                }
                content.selectionDetector = { onSelectionChanged(item.id, it) }

                toggle.apply {
                    rotation = if (item.toggled) EXPANDED_ROTATION else COLLAPSED_ROTATION
                    setOnClickListener {
                        if (mode == BlockView.Mode.EDIT)
                            onToggleClicked(item.id)
                    }
                }

                placeholder.apply {
                    isVisible = item.isEmpty && item.toggled
                    setOnClickListener { onTogglePlaceholderClicked(item.id) }
                }
            }
        }

        override fun indentize(item: BlockView.Indentable) {
            line.setGuidelineBegin(item.indent * dimen(R.dimen.indent))
        }

        override fun select(item: BlockView.Selectable) {
            container.isSelected = item.isSelected
        }

        override fun enableReadOnlyMode() {
            super.enableReadOnlyMode()
            mode = BlockView.Mode.READ
        }

        override fun enableEditMode() {
            super.enableEditMode()
            mode = BlockView.Mode.EDIT
        }

        override fun processChangePayload(payloads: List<Payload>, item: BlockView) {
            check(item is BlockView.Toggle) { "Expected a toggle block, but was: $item" }
            super.processChangePayload(payloads, item)
            payloads.forEach { payload ->
                if (payload.changes.contains(TOGGLE_EMPTY_STATE_CHANGED))
                    placeholder.isVisible = item.isEmpty
            }
        }

        companion object {
            /**
             * Rotation value for a toggle icon for expanded state.
             */
            const val EXPANDED_ROTATION = 90f

            /**
             * Rotation value for a toggle icon for collapsed state.
             */
            const val COLLAPSED_ROTATION = 0f
        }
    }

    class Contact(view: View) : BlockViewHolder(view) {

        private val name = itemView.name
        private val avatar = itemView.avatar

        fun bind(item: BlockView.Contact) {
            name.text = item.name
            avatar.bind(item.name)
        }
    }

    class File(view: View) : BlockViewHolder(view), IndentableHolder {

        private val icon = itemView.fileIcon
        private val size = itemView.fileSize
        private val name = itemView.filename
        private val guideline = itemView.fileGuideline
        private val btnMenu = itemView.btnFileMenu

        fun bind(
            item: BlockView.File.View,
            onFileClicked: (String) -> Unit,
            menuClick: (String) -> Unit
        ) {

            indentize(item)

            itemView.isSelected = item.isSelected

            name.text = item.name

            item.size?.let {
                size.text = FileSizeFormatter.formatFileSize(itemView.context, it)
            }

            when (item.mime?.let { MimeTypes.category(it) }) {
                MimeTypes.Category.PDF -> icon.setImageResource(R.drawable.ic_mime_pdf)
                MimeTypes.Category.IMAGE -> icon.setImageResource(R.drawable.ic_mime_image)
                MimeTypes.Category.AUDIO -> icon.setImageResource(R.drawable.ic_mime_music)
                MimeTypes.Category.TEXT -> icon.setImageResource(R.drawable.ic_mime_text)
                MimeTypes.Category.VIDEO -> icon.setImageResource(R.drawable.ic_mime_video)
                MimeTypes.Category.ARCHIVE -> icon.setImageResource(R.drawable.ic_mime_archive)
                MimeTypes.Category.TABLE -> icon.setImageResource(R.drawable.ic_mime_table)
                MimeTypes.Category.PRESENTATION -> icon.setImageResource(R.drawable.ic_mime_presentation)
                MimeTypes.Category.OTHER -> icon.setImageResource(R.drawable.ic_mime_other)
            }
            btnMenu.setOnClickListener { menuClick(item.id) }
            itemView.setOnClickListener { onFileClicked(item.id) }
        }

        override fun indentize(item: BlockView.Indentable) {
            guideline.setGuidelineBegin(
                item.indent * itemView.context.dimen(R.dimen.indent).toInt()
            )
        }

        fun processChangePayload(payloads: List<Payload>, item: BlockView) {
            check(item is BlockView.File.View) { "Expected a file block, but was: $item" }
            payloads.forEach { payload ->
                if (payload.changes.contains(SELECTION_CHANGED)) {
                    itemView.isSelected = item.isSelected
                }
            }
        }

        class Placeholder(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.filePlaceholderRoot
            private val btnMenu = itemView.btnFilePlaceholderMenu

            fun bind(
                item: BlockView.File.Placeholder,
                onAddLocalFileClick: (String) -> Unit,
                menuClick: (String) -> Unit
            ) {
                indentize(item)
                btnMenu.setOnClickListener { menuClick(item.id) }
                itemView.setOnClickListener { onAddLocalFileClick(item.id) }
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Error(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.fileErrorPlaceholderRoot
            private val btnMenu = itemView.btnFileErrorMenu

            fun bind(item: BlockView.File.Error, menuClick: (String) -> Unit) {
                btnMenu.setOnClickListener { menuClick(item.id) }
                indentize(item)
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Upload(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.fileUploadingPlaceholderRoot

            fun bind(item: BlockView.File.Upload) {
                indentize(item)
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }
    }

    class Video(view: View) : BlockViewHolder(view), IndentableHolder {

        private val player = itemView.playerView
        private val btnMenu = itemView.btnVideoMenu

        fun bind(item: BlockView.Video.View, menuClick: (String) -> Unit) {
            btnMenu.setOnClickListener { menuClick(item.id) }
            indentize(item)
            initPlayer(item.url)
        }

        private fun initPlayer(path: String) {
            itemView.playerView.visibility = View.VISIBLE
            val player = SimpleExoPlayer.Builder(itemView.context).build()
            val source = DefaultDataSourceFactory(
                itemView.context,
                Util.getUserAgent(itemView.context, BuildConfig.LIBRARY_PACKAGE_NAME)
            )
            val mediaSource =
                ProgressiveMediaSource.Factory(source).createMediaSource(Uri.parse(path))
            player.playWhenReady = false
            player.seekTo(0)
            player.prepare(mediaSource, false, false)
            itemView.playerView.player = player
        }

        override fun indentize(item: BlockView.Indentable) {
            player.updatePadding(
                left = item.indent * dimen(R.dimen.indent)
            )
        }

        class Placeholder(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.videoPlaceholderRoot
            private val btnMenu = itemView.btnVideoPlaceholderMenu

            fun bind(
                item: BlockView.Video.Placeholder,
                onAddLocalVideoClick: (String) -> Unit,
                menuClick: (String) -> Unit
            ) {
                indentize(item)
                btnMenu.setOnClickListener { menuClick(item.id) }
                itemView.setOnClickListener { onAddLocalVideoClick(item.id) }
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Error(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.videoErrorRoot
            private val btnMenu = itemView.btnVideoErrorMenu

            fun bind(item: BlockView.Video.Error, menuClick: (String) -> Unit) {
                btnMenu.setOnClickListener { menuClick(item.id) }
                indentize(item)
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Upload(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.videoUploadingPlaceholderRoot

            fun bind(item: BlockView.Video.Upload) {
                indentize(item)
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }
    }

    class Page(view: View) : BlockViewHolder(view), IndentableHolder {

        private val untitled = itemView.resources.getString(R.string.untitled)
        private val icon = itemView.pageIcon
        private val emoji = itemView.emoji
        private val title = itemView.pageTitle
        private val guideline = itemView.pageGuideline

        fun bind(
            item: BlockView.Page,
            onPageClicked: (String) -> Unit
        ) {
            indentize(item)

            itemView.isSelected = item.isSelected

            title.text = if (item.text.isNullOrEmpty()) untitled else item.text

            when {
                item.emoji != null -> emoji.text = item.emoji
                item.isEmpty -> icon.setImageResource(R.drawable.ic_block_empty_page)
                else -> icon.setImageResource(R.drawable.ic_block_page_without_emoji)
            }

            title.setOnClickListener { onPageClicked(item.id) }
        }

        override fun indentize(item: BlockView.Indentable) {
            guideline.setGuidelineBegin(
                item.indent * dimen(R.dimen.indent)
            )
        }

        fun processChangePayload(payloads: List<Payload>, item: BlockView) {
            check(item is BlockView.Page) { "Expected a page block, but was: $item" }
            payloads.forEach { payload ->
                if (payload.changes.contains(SELECTION_CHANGED)) {
                    itemView.isSelected = item.isSelected
                }
            }
        }
    }

    class Bookmark(view: View) : BlockViewHolder(view), IndentableHolder {

        private val title = itemView.bookmarkTitle
        private val description = itemView.bookmarkDescription
        private val url = itemView.bookmarkUrl
        private val image = itemView.bookmarkImage
        private val logo = itemView.bookmarkLogo
        private val error = itemView.loadBookmarkPictureError
        private val card = itemView.bookmarkRoot
        private val menu = itemView.bookmarkMenu

        private val listener: RequestListener<Drawable> = object : RequestListener<Drawable> {

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                error.visible()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                error.invisible()
                return false
            }
        }

        fun bind(
            item: BlockView.Bookmark.View,
            onBookmarkMenuClicked: (String) -> Unit,
            onBookmarkClicked: (BlockView.Bookmark.View) -> Unit
        ) {

            indentize(item)

            itemView.isSelected = item.isSelected

            title.text = item.title
            description.text = item.description
            url.text = item.url

            item.imageUrl?.let { url ->
                Glide.with(image)
                    .load(url)
                    .centerCrop()
                    .listener(listener)
                    .into(image)
            }
            item.faviconUrl?.let { url ->
                Glide.with(logo)
                    .load(url)
                    .listener(listener)
                    .into(logo)
            }

            menu.setOnClickListener { onBookmarkMenuClicked(item.id) }
            card.setOnClickListener { onBookmarkClicked(item) }
        }

        override fun indentize(item: BlockView.Indentable) {
            (card.layoutParams as ViewGroup.MarginLayoutParams).apply {
                val default = dimen(R.dimen.dp_12)
                val extra = item.indent * dimen(R.dimen.indent)
                leftMargin = default + extra
            }
        }

        fun processChangePayload(payloads: List<Payload>, item: BlockView) {
            check(item is BlockView.Bookmark.View) { "Expected a bookmark block, but was: $item" }
            payloads.forEach { payload ->
                if (payload.changes.contains(SELECTION_CHANGED)) {
                    itemView.isSelected = item.isSelected
                }
            }
        }

        class Placeholder(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.bookmarkPlaceholderRoot

            fun bind(
                item: BlockView.Bookmark.Placeholder,
                onBookmarkPlaceholderClicked: (String) -> Unit
            ) {
                indentize(item)
                itemView.setOnClickListener { onBookmarkPlaceholderClicked(item.id) }
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Error(view: View) : BlockViewHolder(view), IndentableHolder {

            private val menu = itemView.errorBookmarkMenu
            private val root = itemView.bookmarkErrorRoot
            private val url = itemView.errorBookmarkUrl

            fun bind(
                item: BlockView.Bookmark.Error,
                onErrorBookmarkMenuClicked: (String) -> Unit,
                onBookmarkClicked: (BlockView.Bookmark.Error) -> Unit
            ) {
                indentize(item)
                url.text = item.url
                menu.setOnClickListener { onErrorBookmarkMenuClicked(item.id) }
                root.setOnClickListener { onBookmarkClicked(item) }
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updateLayoutParams {
                    (this as RecyclerView.LayoutParams).apply {
                        leftMargin = dimen(R.dimen.dp_16) + item.indent * dimen(R.dimen.indent)
                    }
                }
            }
        }
    }

    class Picture(view: View) : BlockViewHolder(view), IndentableHolder {

        private val image = itemView.image
        private val root = itemView.pictureRootLayout
        private val error = itemView.error
        private val btnMenu = itemView.btnPicMenu

        private val listener: RequestListener<Drawable> = object : RequestListener<Drawable> {

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                error.visible()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                error.invisible()
                return false
            }
        }

        fun bind(item: BlockView.Picture.View, menuClick: (String) -> Unit) {
            indentize(item)
            btnMenu.setOnClickListener {
                menuClick(item.id)
            }
            Glide.with(image).load(item.url).listener(listener).into(image)
        }

        override fun indentize(item: BlockView.Indentable) {
            root.updatePadding(
                left = item.indent * dimen(R.dimen.indent)
            )
        }

        class Placeholder(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.picturePlaceholderRoot
            private val btnMore = itemView.btnPicPlaceholderMenu

            fun bind(
                item: BlockView.Picture.Placeholder,
                onAddLocalPictureClick: (String) -> Unit,
                menuClick: (String) -> Unit
            ) {
                indentize(item)
                btnMore.setOnClickListener {
                    menuClick(item.id)
                }
                itemView.setOnClickListener {
                    onAddLocalPictureClick(item.id)
                }
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Error(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.pictureErrorRoot
            private val btnMore = itemView.btnPicErrorMenu

            fun bind(item: BlockView.Picture.Error, menuClick: (String) -> Unit) {
                indentize(item)
                btnMore.setOnClickListener {
                    menuClick(item.id)
                }
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }

        class Upload(view: View) : BlockViewHolder(view), IndentableHolder {

            private val root = itemView.pictureUploadRoot

            fun bind(item: BlockView.Picture.Upload) {
                indentize(item)
            }

            override fun indentize(item: BlockView.Indentable) {
                root.updatePadding(
                    left = item.indent * dimen(R.dimen.indent)
                )
            }
        }
    }

    class Divider(view: View) : BlockViewHolder(view)

    class Highlight(view: View) : BlockViewHolder(view), TextHolder, IndentableHolder {

        override val content: TextInputWidget = itemView.highlightContent
        override val root: View = itemView
        private val indent = itemView.highlightIndent
        private val container = itemView.highlightBlockContentContainer

        init {
            content.setSpannableFactory(DefaultSpannableFactory())
        }

        fun bind(
            item: BlockView.Highlight,
            onTextChanged: (String, Editable) -> Unit,
            onFocusChanged: (String, Boolean) -> Unit
        ) {
            //indentize(item)

            if (item.mode == BlockView.Mode.READ) {
                enableReadOnlyMode()
                content.setText(item.text)
            } else {
                enableEditMode()
                content.clearTextWatchers()
                content.setOnFocusChangeListener { _, hasFocus ->
                    onFocusChanged(item.id, hasFocus)
                }
                content.setText(item.text)
                content.addTextChangedListener(
                    DefaultTextWatcher { text ->
                        onTextChanged(item.id, text)
                    }
                )
            }
        }

        override fun select(item: BlockView.Selectable) {
            container.isSelected = item.isSelected
        }

        override fun indentize(item: BlockView.Indentable) {
            indent.updateLayoutParams {
                width = item.indent * dimen(R.dimen.indent)
            }
        }
    }

    class Footer(view: View) : BlockViewHolder(view) {

        private val footer = itemView

        fun bind(
            onFooterClicked: () -> Unit
        ) {
            footer.setOnClickListener { onFooterClicked() }
        }
    }

    companion object {
        const val HOLDER_PARAGRAPH = 0
        const val HOLDER_TITLE = 1
        const val HOLDER_HEADER_ONE = 2
        const val HOLDER_HEADER_TWO = 3
        const val HOLDER_HEADER_THREE = 4
        const val HOLDER_CODE_SNIPPET = 5
        const val HOLDER_CHECKBOX = 6
        const val HOLDER_TASK = 7
        const val HOLDER_BULLET = 8
        const val HOLDER_NUMBERED = 9
        const val HOLDER_TOGGLE = 10
        const val HOLDER_CONTACT = 11
        const val HOLDER_PAGE = 13
        const val HOLDER_DIVIDER = 16
        const val HOLDER_HIGHLIGHT = 17
        const val HOLDER_FOOTER = 18

        const val HOLDER_VIDEO = 19
        const val HOLDER_VIDEO_PLACEHOLDER = 20
        const val HOLDER_VIDEO_UPLOAD = 21
        const val HOLDER_VIDEO_ERROR = 22

        const val HOLDER_PICTURE = 24
        const val HOLDER_PICTURE_PLACEHOLDER = 25
        const val HOLDER_PICTURE_UPLOAD = 26
        const val HOLDER_PICTURE_ERROR = 27

        const val HOLDER_BOOKMARK = 28
        const val HOLDER_BOOKMARK_PLACEHOLDER = 29
        const val HOLDER_BOOKMARK_ERROR = 30

        const val HOLDER_FILE = 31
        const val HOLDER_FILE_PLACEHOLDER = 32
        const val HOLDER_FILE_UPLOAD = 33
        const val HOLDER_FILE_ERROR = 34

        const val FOCUS_TIMEOUT_MILLIS = 16L
        const val KEYBOARD_SHOW_DELAY = 16L
    }
}
