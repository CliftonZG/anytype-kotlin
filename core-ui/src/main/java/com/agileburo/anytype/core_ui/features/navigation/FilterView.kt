package com.agileburo.anytype.core_ui.features.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agileburo.anytype.core_ui.R
import com.agileburo.anytype.core_utils.ext.imm
import com.agileburo.anytype.core_utils.ext.toast
import kotlinx.android.synthetic.main.view_page_links_filter.view.*

class FilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val recycler: RecyclerView
    private val search: SearchView
    private val cancel: TextView
    private val sorting: View
    private var links: MutableList<PageLinkView> = mutableListOf()

    var cancelClicked: (() -> Unit)? = null
    var pageClicked: ((String) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_page_links_filter, this, true)
        recycler = recyclerView
        search = searchView
        cancel = btnCancel
        sorting = icSorting
        recycler.layoutManager = LinearLayoutManager(context)
        cancel.setOnClickListener { cancelClicked?.invoke() }
        sorting.setOnClickListener { context.toast("Not implemented yet") }

        with(search) {
            post {
                if (!hasFocus()) {
                    if (requestFocus()) {
                        context.imm().showSoftInput(this, 0)
                    }
                }
            }
            isIconifiedByDefault = false
            queryHint = context.resources.getString(R.string.filter_view_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    search.clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null && recycler.adapter != null) {
                        (recycler.adapter as PageLinksAdapter).let {
                            val filtered = links.filterBy(newText)
                            it.updateLinks(filtered)
                        }
                    }
                    return false
                }
            })
        }
    }

    fun bind(links: MutableList<PageLinkView>) {
        this.links.clear()
        this.links.addAll(links)
        if (recycler.adapter == null) {
            recycler.adapter = PageLinksAdapter(
                data = this.links,
                onClick = { pageId -> pageClicked?.invoke(pageId) }
            )
        } else {
            (recycler.adapter as PageLinksAdapter).updateLinks(this.links)
        }
    }
}