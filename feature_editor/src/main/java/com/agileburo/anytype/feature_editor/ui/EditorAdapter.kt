package com.agileburo.anytype.feature_editor.ui

import android.text.SpannableString
import android.text.Spanned
import android.text.style.BulletSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.agileburo.anytype.core_utils.swap
import com.agileburo.anytype.feature_editor.R
import com.agileburo.anytype.feature_editor.domain.ContentType
import com.agileburo.anytype.feature_editor.presentation.model.BlockView
import com.agileburo.anytype.feature_editor.presentation.util.BlockViewDiffUtil
import kotlinx.android.synthetic.main.item_block_checkbox.view.*
import kotlinx.android.synthetic.main.item_block_code_snippet.view.*
import kotlinx.android.synthetic.main.item_block_editable.view.*
import kotlinx.android.synthetic.main.item_block_header_four.view.*
import kotlinx.android.synthetic.main.item_block_header_one.view.*
import kotlinx.android.synthetic.main.item_block_header_three.view.*
import kotlinx.android.synthetic.main.item_block_header_two.view.*
import kotlinx.android.synthetic.main.item_block_quote.view.*
import kotlinx.android.synthetic.main.item_number_list_item.view.*
import timber.log.Timber
import java.lang.IllegalStateException

class EditorAdapter(
    private val blocks: MutableList<BlockView>,
    private val listener: (BlockView) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun setBlocks(items: List<BlockView>) {
        Timber.d("Set blocks ${items.size}")
        blocks.addAll(items)
        notifyDataSetChanged()
    }

    fun updateBlock(block: BlockView) {
        val index = blocks.indexOfFirst { it.id == block.id }
        if (index >= 0 && index < blocks.size) {
            blocks[index] = block
            notifyItemChanged(index)
        }
    }

    fun update(items : List<BlockView>) {
        val callback = BlockViewDiffUtil(old = blocks, new = items)
        val result = DiffUtil.calculateDiff(callback)
        blocks.clear()
        blocks.addAll(items)
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            HOLDER_PARAGRAPH -> {
                val view = inflater.inflate(R.layout.item_block_editable, parent, false)
                ViewHolder.ParagraphHolder(view)
            }
            HOLDER_HEADER_ONE -> {
                val view = inflater.inflate(R.layout.item_block_header_one, parent, false)
                ViewHolder.HeaderOneHolder(view)
            }
            HOLDER_HEADER_TWO -> {
                val view = inflater.inflate(R.layout.item_block_header_two, parent, false)
                ViewHolder.HeaderTwoHolder(view)
            }
            HOLDER_HEADER_THREE -> {
                val view = inflater.inflate(R.layout.item_block_header_three, parent, false)
                ViewHolder.HeaderThreeHolder(view)
            }
            HOLDER_HEADER_FOUR -> {
                val view = inflater.inflate(R.layout.item_block_header_four, parent, false)
                ViewHolder.HeaderFourHolder(view)
            }
            HOLDER_QUOTE -> {
                val view = inflater.inflate(R.layout.item_block_quote, parent, false)
                ViewHolder.QuoteHolder(view)
            }
            HOLDER_CHECKBOX -> {
                val view = inflater.inflate(R.layout.item_block_checkbox, parent, false)
                ViewHolder.CheckBoxHolder(view)
            }
            HOLDER_CODE_SNIPPET -> {
                val view = inflater.inflate(R.layout.item_block_code_snippet, parent, false)
                ViewHolder.CodeSnippetHolder(view)
            }
            HOLDER_BULLET -> {
                val view = inflater.inflate(R.layout.item_block_editable, parent, false)
                ViewHolder.BulletHolder(view)
            }
            HOLDER_NUMBERED -> {
                val view = inflater.inflate(R.layout.item_number_list_item, parent, false)
                ViewHolder.NumberedHolder(view)
            }

            else -> TODO()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (blocks[position].contentType) {
            is ContentType.P -> HOLDER_PARAGRAPH
            is ContentType.H1 -> HOLDER_HEADER_ONE
            is ContentType.H2 -> HOLDER_HEADER_TWO
            is ContentType.H3 -> HOLDER_HEADER_THREE
            is ContentType.H4 -> HOLDER_HEADER_FOUR
            is ContentType.Quote -> HOLDER_QUOTE
            is ContentType.Check -> HOLDER_CHECKBOX
            is ContentType.Code -> HOLDER_CODE_SNIPPET
            is ContentType.NumberedList -> HOLDER_NUMBERED
            is ContentType.UL -> HOLDER_BULLET
            else -> throw IllegalStateException("Implement Toggle!!!")
        }
    }

    override fun getItemCount() = blocks.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.ParagraphHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.HeaderOneHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.HeaderTwoHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.HeaderThreeHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.HeaderFourHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.QuoteHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.CheckBoxHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.CodeSnippetHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.BulletHolder -> holder.bind(blocks[position], listener)
            is ViewHolder.NumberedHolder -> holder.bind(blocks[position], listener)
        }
    }

    fun onItemMoved(fromPosition: Int, toPosition: Int): Boolean {
        swapPosition(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    private fun swapPosition(fromPosition: Int, toPosition: Int) {
        blocks.swap(fromPosition, toPosition)
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        class ParagraphHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.tvContent.text = block.content.text
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class HeaderOneHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.headerContentText.text = block.content.text
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class HeaderTwoHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.headerTwoContentText.text = block.content.text
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class HeaderThreeHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.headerThreeContentText.text = block.content.text
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class HeaderFourHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.headerFourContentText.text = block.content.text
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class QuoteHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.quoteContent.text = block.content.text
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class CheckBoxHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.checkBoxContent.text = block.content.text
                itemView.checkBoxContent.setOnClickListener {
                    clickListener(block)
                }
            }
        }

        class CodeSnippetHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.codeSnippetContent.text = block.content.text
                itemView.codeSnippetContent.setOnClickListener { clickListener(block) }
            }
        }

        class BulletHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                itemView.tvContent.text = SpannableString(block.content.text).apply {
                    setSpan(BulletSpan(40), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                itemView.setOnClickListener { clickListener(block) }
            }
        }

        class NumberedHolder(itemView: View) : ViewHolder(itemView) {

            fun bind(block: BlockView, clickListener: (BlockView) -> Unit) {
                with(itemView) {
                    positionText.text = "${block.content.param.number}."
                    contentText.text = block.content.text
                    setOnClickListener { clickListener(block) }
                }
            }
        }
    }

    companion object {
        const val HOLDER_PARAGRAPH = 0
        const val HOLDER_HEADER_ONE = 1
        const val HOLDER_HEADER_TWO = 2
        const val HOLDER_HEADER_THREE = 3
        const val HOLDER_QUOTE = 4
        const val HOLDER_CHECKBOX = 5
        const val HOLDER_CODE_SNIPPET = 6
        const val HOLDER_NUMBERED = 7
        const val HOLDER_HEADER_FOUR = 8
        const val HOLDER_BULLET = 9
    }
}