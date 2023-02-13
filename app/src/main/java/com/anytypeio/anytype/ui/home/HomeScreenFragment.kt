package com.anytypeio.anytype.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_utils.ext.toast
import com.anytypeio.anytype.core_utils.ui.BaseComposeFragment
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.presentation.home.Command
import com.anytypeio.anytype.presentation.home.HomeScreenViewModel
import com.anytypeio.anytype.ui.settings.typography
import com.anytypeio.anytype.ui.widgets.SelectWidgetSourceFragment
import com.anytypeio.anytype.ui.widgets.SelectWidgetTypeFragment
import javax.inject.Inject
import kotlinx.coroutines.launch

class HomeScreenFragment : BaseComposeFragment() {

    @Inject
    lateinit var factory: HomeScreenViewModel.Factory

    private val vm by viewModels<HomeScreenViewModel> { factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme(typography = typography) {
                HomeScreen(
                    widgets = vm.views.collectAsState().value,
                    onExpand = { path -> vm.onExpand(path) },
                    onCreateWidget = {
                        findNavController().navigate(R.id.selectWidgetSourceScreen)
                    },
                    onEditWidgets = { context.toast("Coming soon") },
                    onRefresh = vm::onRefresh,
                    onWidgetMenuAction = { widget: Id, action: DropDownMenuAction ->
                        when(action) {
                            DropDownMenuAction.ChangeWidgetSource -> {
                                vm.onChangeWidgetSourceClicked(widget)
                            }
                            DropDownMenuAction.ChangeWidgetType -> {
                                vm.onChangeWidgetTypeClicked(widget)
                            }
                            DropDownMenuAction.EditWidgets -> {
                                toast("TODO")
                            }
                            DropDownMenuAction.RemoveWidget -> {
                                vm.onDeleteWidgetClicked(widget)
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.commands.collect { command -> proceed(command) }
            }
        }
    }

    private fun proceed(command: Command) {
        when(command) {
            is Command.ChangeWidgetSource -> {
                findNavController().navigate(
                    R.id.selectWidgetSourceScreen,
                    args = SelectWidgetSourceFragment.args(
                        ctx = command.ctx,
                        widget = command.widget,
                        source = command.source,
                        type = command.type
                    )
                )
            }
            is Command.SelectWidgetSource -> {
                findNavController().navigate(R.id.selectWidgetSourceScreen)
            }
            is Command.SelectWidgetType -> {
                findNavController().navigate(
                    R.id.selectWidgetTypeScreen,
                    args = SelectWidgetTypeFragment.args(
                        ctx = command.ctx,
                        widget = command.widget,
                        source = command.source,
                        type = command.type
                    )
                )
            }
        }
    }

    override fun injectDependencies() {
        componentManager().homeScreenComponent.get().inject(this)
    }

    override fun releaseDependencies() {
        componentManager().homeScreenComponent.release()
    }
}