package com.anytypeio.anytype.presentation.editor.editor

import com.anytypeio.anytype.core_models.Block
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.presentation.editor.Editor
import com.anytypeio.anytype.presentation.editor.model.LatexUpdate

interface LatexInteractor {
    class LatexTextInteractor(
        private val proxies: Editor.Proxer,
        private val stores: Editor.Storage,
    ) {

        suspend fun consume(update: LatexUpdate, context: Id) {
            if (update is LatexUpdate.Default)
                proxies.latexSaves.send(update)
        }

        private suspend fun replaceBy(
            context: Id,
            target: Id,
            prototype: Block.Prototype
        ) {
            proxies.intents.send(
                Intent.CRUD.Replace(
                    context = context,
                    target = target,
                    prototype = prototype
                )
            )
        }
    }
}