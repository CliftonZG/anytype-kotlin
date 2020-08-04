package com.agileburo.anytype.di.feature

import com.agileburo.anytype.core_utils.di.scope.PerScreen
import com.agileburo.anytype.domain.auth.interactor.GetMnemonic
import com.agileburo.anytype.domain.auth.repo.AuthRepository
import com.agileburo.anytype.presentation.keychain.KeychainPhraseViewModelFactory
import com.agileburo.anytype.ui.profile.KeychainPhraseDialog
import dagger.Module
import dagger.Provides
import dagger.Subcomponent


@Subcomponent(
    modules = [KeychainPhraseModule::class]
)
@PerScreen
interface KeychainPhraseSubComponent {

    @Subcomponent.Builder
    interface Builder {
        fun keychainPhraseModule(module: KeychainPhraseModule): Builder
        fun build(): KeychainPhraseSubComponent
    }

    fun inject(fragment: KeychainPhraseDialog)
}

@Module
object KeychainPhraseModule {

    @JvmStatic
    @Provides
    @PerScreen
    fun provideKeychainPhraseViewModelFactory(
        getMnemonic: GetMnemonic
    ) = KeychainPhraseViewModelFactory(
        getMnemonic = getMnemonic
    )

    @JvmStatic
    @Provides
    @PerScreen
    fun provideGetMnemonicUseCase(
        repository: AuthRepository
    ): GetMnemonic = GetMnemonic(
        repository = repository
    )
}