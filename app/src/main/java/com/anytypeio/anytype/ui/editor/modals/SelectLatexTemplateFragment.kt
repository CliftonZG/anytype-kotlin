package com.anytypeio.anytype.ui.editor.modals

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_ui.extensions.color
import com.anytypeio.anytype.core_ui.features.editor.modal.SelectLatexTemplateAdapter
import com.anytypeio.anytype.core_utils.ui.BaseBottomSheetFragment
import com.anytypeio.anytype.databinding.FragmentSelectLatexTemplateBinding
import com.anytypeio.anytype.library_syntax_highlighter.obtainLatexTemplates
import com.google.android.material.bottomsheet.BottomSheetDialog
import timber.log.Timber

class SelectLatexTemplateFragment :
    BaseBottomSheetFragment<FragmentSelectLatexTemplateBinding>() {

    private val selectLatexTemplateAdapter by lazy {
        SelectLatexTemplateAdapter(
            items = requireContext().obtainLatexTemplates()
        ) { template, value ->
            val parent = parentFragment
            Log.v("SelectLatexTemplateFragment", "$parent")
            check(parent is SelectLatexTemplateReceiver)
            parent.onLatexTemplateSelected(target, template, value)
            dismiss()
        }
    }

    private val target: String
        get() = requireArguments()
            .getString(ARG_TARGET)
            ?: throw IllegalStateException(MISSING_TARGET_ERROR)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        dialog?.setOnShowListener { dg ->
            val bottomSheet = (dg as? BottomSheetDialog)?.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.setBackgroundColor(requireContext().color(android.R.color.transparent))
        }
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = selectLatexTemplateAdapter
        }
    }

    override fun injectDependencies() {}
    override fun releaseDependencies() {}

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSelectLatexTemplateBinding = FragmentSelectLatexTemplateBinding.inflate(
        inflater, container, false
    )

    companion object {
        fun new(target: Id) = SelectLatexTemplateFragment().apply {
            arguments = bundleOf(ARG_TARGET to target)
        }

        private const val ARG_TARGET = "arg.select_latex_template.target"
        private const val MISSING_TARGET_ERROR = "Target missing in args"
    }
}

interface SelectLatexTemplateReceiver {
    fun onLatexTemplateSelected(target: Id, key: String, value: String)
}