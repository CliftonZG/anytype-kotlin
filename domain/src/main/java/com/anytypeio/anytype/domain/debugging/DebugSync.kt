package com.anytypeio.anytype.domain.debugging

import com.anytypeio.anytype.domain.base.AppCoroutineDispatchers
import com.anytypeio.anytype.domain.base.ResultInteractor
import com.anytypeio.anytype.domain.block.repo.BlockRepository
import kotlinx.coroutines.Dispatchers

class DebugSync(
    private val repo: BlockRepository,
    dispatchers: AppCoroutineDispatchers
) : ResultInteractor<Unit, String>(dispatchers.io) {

    override suspend fun doWork(params: Unit): String = repo.debugSync()
}