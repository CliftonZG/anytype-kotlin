package com.agileburo.anytype.domain.page

import com.agileburo.anytype.domain.base.BaseUseCase
import com.agileburo.anytype.domain.base.Either
import com.agileburo.anytype.domain.block.repo.BlockRepository
import com.agileburo.anytype.domain.config.MainConfig

class OpenPage(
    private val repo: BlockRepository
) : BaseUseCase<Unit, OpenPage.Params>() {

    override suspend fun run(params: Params) = try {
        repo.openPage(params.id).let {
            Either.Right(it)
        }
    } catch (t: Throwable) {
        Either.Left(t)
    }

    /**
     * @property id page's id
     */
    class Params(val id: String) {
        companion object {
            fun reference() = Params(id = MainConfig.REFERENCE_PAGE_ID)
        }
    }

}