package com.ininids.aamhelper.language

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings

class AamFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        return FormattingModelProvider
            .createFormattingModelForPsiFile(
                formattingContext.psiElement.containingFile,
                AamBlock(formattingContext.node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(), createSpaceBuilder(codeStyleSettings)),
                codeStyleSettings
            )
    }

    private fun createSpaceBuilder(settings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(settings, AamLanguage)
            .before(AamElementTypes.PROPERTY)
            .blankLines(0)
            .before(AamElementTypes.IMPORT_STATEMENT)
            .none()
    }
}

class AamBlock(
    private val myNode: ASTNode,
    private val myWrap: Wrap?,
    private val myAlignment: Alignment?,
    private val mySpacingBuilder: SpacingBuilder
) : Block {

    override fun getTextRange(): TextRange {
        return myNode.textRange
    }

    override fun getSubBlocks(): List<Block> {
        val blocks = ArrayList<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType !== com.intellij.psi.TokenType.WHITE_SPACE) {
                val block = AamBlock(
                    child,
                    Wrap.createWrap(WrapType.NONE, false),
                    Alignment.createAlignment(),
                    mySpacingBuilder
                )
                blocks.add(block)
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getWrap(): Wrap? {
        return myWrap
    }

    override fun getIndent(): Indent? {
        return Indent.getNoneIndent()
    }

    override fun getAlignment(): Alignment? {
        return myAlignment
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return mySpacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }

    override fun isIncomplete(): Boolean {
        return false
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        return ChildAttributes(Indent.getNoneIndent(), null)
    }
}