package com.agileburo.anytype.di.feature

import com.agileburo.anytype.core_utils.di.scope.PerScreen
import com.agileburo.anytype.domain.database.interactor.DeleteDatabase
import com.agileburo.anytype.domain.database.interactor.DuplicateDatabase
import com.agileburo.anytype.domain.database.repo.DatabaseRepository
import com.agileburo.anytype.presentation.databaseview.modals.EditDatabaseViewModelFactory
import com.agileburo.anytype.ui.database.modals.EditDatabaseFragment
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [EditDatabaseModule::class])
@PerScreen
interface EditDatabaseSubComponent {

    @Subcomponent.Builder
    interface Builder {

        fun editModule(module: EditDatabaseModule): Builder
        fun build(): EditDatabaseSubComponent
    }

    fun inject(fragment: EditDatabaseFragment)
}

@Module
class EditDatabaseModule(
    private val databaseId: String,
    private val databaseName: String
) {

    @Provides
    @PerScreen
    fun provideDuplicateDatabase(databaseRepository: DatabaseRepository) =
        DuplicateDatabase(databaseRepository)

    @Provides
    @PerScreen
    fun provideDeleteDatrabase(databaseRepository: DatabaseRepository) =
        DeleteDatabase(databaseRepository)

    @Provides
    @PerScreen
    fun provideEditDatabaseViewModelFactory(
        duplicateDatabase: DuplicateDatabase,
        deleteDatabase: DeleteDatabase
    ): EditDatabaseViewModelFactory =
        EditDatabaseViewModelFactory(
            databaseName = databaseName,
            databaseId = databaseId,
            deleteDatabase = deleteDatabase,
            duplicateDatabase = duplicateDatabase
        )
}