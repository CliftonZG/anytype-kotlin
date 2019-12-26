package com.agileburo.anytype.domain.database.interactor

import com.agileburo.anytype.domain.base.BaseUseCase
import com.agileburo.anytype.domain.base.Either
import com.agileburo.anytype.domain.database.model.ViewType
import com.agileburo.anytype.domain.database.repo.DatabaseRepository
import java.lang.Exception

class SwitchDisplayView(
    private val databaseRepo: DatabaseRepository
) : BaseUseCase<String, SwitchDisplayView.Params>() {

    override suspend fun run(params: Params): Either<Throwable, String> = try {
        databaseRepo.updateViewType(params.type)
        Either.Right("Switch display view with id:${params.id} to type:${params.type}")
    } catch (e: Throwable) {
        Either.Left(e)
    }

    data class Params(val id: String, val type: ViewType)
}