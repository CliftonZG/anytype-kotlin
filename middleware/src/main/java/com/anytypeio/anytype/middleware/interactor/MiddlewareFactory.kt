package com.anytypeio.anytype.middleware.interactor

import android.util.Log
import com.anytypeio.anytype.core_models.Block
import com.anytypeio.anytype.middleware.mappers.MBBookmark
import com.anytypeio.anytype.middleware.mappers.MBDiv
import com.anytypeio.anytype.middleware.mappers.MBDivStyle
import com.anytypeio.anytype.middleware.mappers.MBFile
import com.anytypeio.anytype.middleware.mappers.MBLatex
import com.anytypeio.anytype.middleware.mappers.MBLink
import com.anytypeio.anytype.middleware.mappers.MBRelation
import com.anytypeio.anytype.middleware.mappers.MBTableOfContents
import com.anytypeio.anytype.middleware.mappers.MBText
import com.anytypeio.anytype.middleware.mappers.MBlock
import com.anytypeio.anytype.middleware.mappers.MBookmarkState
import com.anytypeio.anytype.middleware.mappers.toMiddlewareModel

class MiddlewareFactory {

    fun create(prototype: Block.Prototype): MBlock {
        return when (prototype) {
            is Block.Prototype.Bookmark.New -> {
                Log.v("MiddlewareFactory", "Bookmark.New")
                val bookmark = MBBookmark()
                MBlock(bookmark = bookmark)
            }
            is Block.Prototype.Bookmark.Existing -> {
                Log.v("MiddlewareFactory", "Bookmark.Existing")
                val bookmark = MBBookmark(
                    targetObjectId = prototype.target,
                    state = MBookmarkState.Done
                )
                MBlock(bookmark = bookmark)
            }
            is Block.Prototype.Text -> {
                Log.v("MiddlewareFactory", "Text")
                val text = MBText(
                    style = prototype.style.toMiddlewareModel(),
                    text = prototype.text.orEmpty()
                )
                MBlock(text = text)
            }
            is Block.Prototype.DividerLine -> {
                Log.v("MiddlewareFactory", "DividerLine")
                val divider = MBDiv(style = MBDivStyle.Line)
                MBlock(div = divider)
            }
            is Block.Prototype.DividerDots -> {
                Log.v("MiddlewareFactory", "DividerDots")
                val divider = MBDiv(style = MBDivStyle.Dots)
                MBlock(div = divider)
            }
            is Block.Prototype.File -> {
                Log.v("MiddlewareFactory", "File")
                val file = MBFile(
                    state = prototype.state.toMiddlewareModel(),
                    type = prototype.type.toMiddlewareModel(),
                    targetObjectId = prototype.targetObjectId.orEmpty()
                )
                MBlock(file_ = file)
            }
            is Block.Prototype.Latex -> {
                Log.v("MiddlewareFactory", "Latex")
                val latex = MBLatex(
                    text = prototype.text.orEmpty(),
                    //latex = prototype.latex.orEmpty()
                )
                MBlock(latex = latex)
            }
            is Block.Prototype.Link -> {
                Log.v("MiddlewareFactory", "Link")
                val link = MBLink(
                    targetBlockId = prototype.target,
                    cardStyle = prototype.cardStyle.toMiddlewareModel(),
                    iconSize = prototype.iconSize.toMiddlewareModel(),
                    description = prototype.description.toMiddlewareModel()
                )
                MBlock(link = link)
            }
            is Block.Prototype.Relation -> {
                Log.v("MiddlewareFactory", "Relation")
                val relation = MBRelation(
                    key = prototype.key
                )
                MBlock(relation = relation)
            }
            is Block.Prototype.TableOfContents -> {
                Log.v("MiddlewareFactory", "TableOfContents")
                val toc = MBTableOfContents()
                MBlock(tableOfContents = toc)
            }
            else -> throw IllegalStateException("Unexpected prototype: $prototype")
        }
    }
}