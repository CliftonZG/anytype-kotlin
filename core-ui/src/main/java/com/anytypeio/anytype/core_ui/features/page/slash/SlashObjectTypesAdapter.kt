package com.anytypeio.anytype.core_ui.features.page.slash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_ui.features.page.slash.holders.*
import com.anytypeio.anytype.presentation.page.editor.slash.SlashItem
import kotlinx.android.synthetic.main.item_slash_widget_subheader.view.*

class SlashObjectTypesAdapter(
    private var items: List<SlashItem>,
    private val clicks: (SlashItem) -> Unit,
    private val clickBack: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun update(items: List<SlashItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun clear() {
        val size = items.size
        if (size > 0) {
            items = listOf()
            notifyItemRangeRemoved(0, size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_slash_widget_object_type -> {
                ObjectTypeMenuHolder(
                    view = inflater.inflate(viewType, parent, false)
                ).apply {
                    itemView.setOnClickListener {
                        clicks(items[bindingAdapterPosition])
                    }
                }
            }
            R.layout.item_slash_widget_subheader -> {
                SubheaderMenuHolder(
                    view = inflater.inflate(viewType, parent, false)
                ).apply {
                    itemView.flBack.setOnClickListener {
                        clickBack.invoke()
                    }
                }
            }
            else -> throw IllegalArgumentException("Wrong viewtype:$viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is ObjectTypeMenuHolder -> {
                val item = items[position] as SlashItem.ObjectType
                holder.bind(item)
            }
            is SubheaderMenuHolder -> {
                val item = items[position] as SlashItem.Subheader
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is SlashItem.ObjectType -> R.layout.item_slash_widget_object_type
        is SlashItem.Subheader -> R.layout.item_slash_widget_subheader
        else -> throw IllegalArgumentException("Wrong item type:${items[position]} for SlashObjectTypeAdapter")
    }
}