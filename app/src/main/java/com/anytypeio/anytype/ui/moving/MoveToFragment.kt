package com.anytypeio.anytype.ui.moving

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_ui.features.navigation.DefaultObjectViewAdapter
import com.anytypeio.anytype.core_utils.ext.*
import com.anytypeio.anytype.core_utils.ui.BaseBottomSheetFragment
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.presentation.moving.MoveToViewModel
import com.anytypeio.anytype.presentation.moving.MoveToViewModelFactory
import com.anytypeio.anytype.presentation.search.ObjectSearchView
import com.anytypeio.anytype.ui.search.ObjectSearchFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_object_search.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class MoveToFragment : BaseBottomSheetFragment() {

    private val vm by viewModels<MoveToViewModel> { factory }

    @Inject
    lateinit var factory: MoveToViewModelFactory

    private lateinit var clearSearchText: View
    private lateinit var filterInputField: EditText

    private val block get() = arg<Id>(ARG_BLOCK)

    private val moveToAdapter by lazy {
        DefaultObjectViewAdapter(
            onClick = vm::onObjectClicked
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_object_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFullHeight()
        setTransparent()
        BottomSheetBehavior.from(sheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isHideable = true
            addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            vm.onBottomSheetHidden()
                        }
                    }
                }
            )
        }
        vm.state.observe(viewLifecycleOwner, { observe(it) })
        clearSearchText = searchView.findViewById(R.id.clearSearchText)
        filterInputField = searchView.findViewById(R.id.filterInputField)
        filterInputField.setHint(R.string.search)
        initialize()
    }

    override fun onStart() {
        lifecycleScope.launch {
            jobs += subscribe(vm.commands) { execute(it) }
        }
        super.onStart()
        vm.onStart()
        expand()
    }

    private fun observe(state: ObjectSearchView) {
        when (state) {
            ObjectSearchView.Loading -> {
                recyclerView.invisible()
                tvScreenStateMessage.invisible()
                tvScreenStateSubMessage.invisible()
                progressBar.visible()
            }
            is ObjectSearchView.Success -> {
                progressBar.invisible()
                tvScreenStateMessage.invisible()
                tvScreenStateSubMessage.invisible()
                recyclerView.visible()
                moveToAdapter.submitList(state.objects)
            }
            ObjectSearchView.EmptyPages -> {
                progressBar.invisible()
                recyclerView.invisible()
                tvScreenStateMessage.visible()
                tvScreenStateMessage.text = getString(R.string.search_empty_pages)
                tvScreenStateSubMessage.invisible()
            }
            is ObjectSearchView.NoResults -> {
                progressBar.invisible()
                recyclerView.invisible()
                tvScreenStateMessage.visible()
                tvScreenStateMessage.text = getString(R.string.search_no_results, state.searchText)
                tvScreenStateSubMessage.visible()
            }
            is ObjectSearchView.Error -> {
                progressBar.invisible()
                recyclerView.invisible()
                tvScreenStateMessage.visible()
                tvScreenStateMessage.text = state.error
                tvScreenStateSubMessage.invisible()
            }
            else -> Timber.d("Skipping state: $state")
        }
    }

    private fun execute(command: MoveToViewModel.Command) {
        when (command) {
            MoveToViewModel.Command.Exit -> dismiss()
            is MoveToViewModel.Command.Move -> {
                withParent<OnMoveToAction> {
                    onMoveTo(
                        target = command.target,
                        block = block
                    )
                }
                dismiss()
            }
        }
    }

    private fun initialize() {
        with(tvScreenTitle) {
            text = getString(R.string.move_to)
            visible()
        }
        recyclerView.invisible()
        tvScreenStateMessage.invisible()
        progressBar.invisible()
        clearSearchText.setOnClickListener {
            filterInputField.setText(ObjectSearchFragment.EMPTY_FILTER_TEXT)
            clearSearchText.invisible()
        }
        filterInputField.doAfterTextChanged { newText ->
            if (newText != null) {
                vm.onSearchTextChanged(newText.toString())
            }
            if (newText.isNullOrEmpty()) {
                clearSearchText.invisible()
            } else {
                clearSearchText.visible()
            }
        }
        with(recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moveToAdapter
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(drawable(R.drawable.divider_object_search))
                }
            )
        }
    }

    private fun setupFullHeight() {
        val lp = (root.layoutParams as FrameLayout.LayoutParams)
        lp.height =
            Resources.getSystem().displayMetrics.heightPixels - requireActivity().statusBarHeight
        root.layoutParams = lp
    }

    private fun setTransparent() {
        with(root) {
            background = null
            (parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun injectDependencies() {
        componentManager().moveToComponent.get().inject(this)
    }

    override fun releaseDependencies() {
        componentManager().moveToComponent.release()
    }

    companion object {
        const val ARG_BLOCK = "arg.move_to.blocks"

        fun new(block: Id) = MoveToFragment().apply {
            arguments = bundleOf(
                ARG_BLOCK to block
            )
        }
    }
}

interface OnMoveToAction {
    fun onMoveTo(target: Id, block: Id)
}