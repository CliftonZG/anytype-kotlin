package com.anytypeio.anytype.domain.block.interactor


import com.anytypeio.anytype.core_models.Command
import com.anytypeio.anytype.domain.block.repo.BlockRepository
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.domain.base.BaseUseCase
import com.anytypeio.anytype.domain.base.Either

open class UpdateLatex(
    private val repo: BlockRepository
) : BaseUseCase<Unit, UpdateLatex.Params>() {

    override suspend fun run(params: Params) = try {
        print("UpdateLatex, run")
        repo.updateLatex(
            command = Command.UpdateLatex(
                contextId = params.context,
                blockId = params.target,
                text = params.text
            )
        ).let {
            Either.Right(it)
        }
    } catch (t: Throwable) {
        Either.Left(t)
    }

    data class Params(
        val context: Id,
        val target: Id,
        val text: String,
    )
}