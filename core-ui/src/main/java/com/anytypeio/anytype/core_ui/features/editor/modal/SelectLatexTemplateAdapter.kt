package com.anytypeio.anytype.core_ui.features.editor.modal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_ui.features.editor.modal.SelectLatexTemplateAdapter.Holder

class SelectLatexTemplateAdapter(
    private val items: List<Pair<String, String>>,
    private val onLatexTemplateSelected: (String, String) -> Unit,
) : RecyclerView.Adapter<Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(
            view = inflater.inflate(
                R.layout.item_select_latex_template,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (key, value) = items[position]
        holder.bind(value) {
            onLatexTemplateSelected(key, value)
        }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val template: TextView = itemView.findViewById(R.id.latex_template)
        fun bind(value: String, onClick: () -> Unit) {
            template.text = value
            itemView.setOnClickListener { onClick() }
        }
    }
}