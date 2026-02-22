package com.ininids.aamhelper.language

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock

class AamFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            AamBlock(
                formattingContext.node,
                Wrap.createWrap(WrapType.NONE, false),
                null,
                createSpaceBuilder(codeStyleSettings)
            ),
            codeStyleSettings
        )
    }

    private fun createSpaceBuilder(settings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(settings, AamLanguage)
            .around(AamTokenTypes.EQUALS).spacing(1, 1, 0, false, 0)
    }
}

class AamBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val mySpacingBuilder: SpacingBuilder,
    private val myIndent: Indent = Indent.getNoneIndent()
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val blocks = ArrayList<Block>()
        val insideSchema = myNode.elementType == AamElementTypes.SCHEMA_DECLARATION
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE && child.textLength > 0) {
                val childIndent = if (insideSchema) {
                    when (child.elementType) {
                        AamTokenTypes.LBRACE,
                        AamTokenTypes.RBRACE,
                        AamTokenTypes.SCHEMA_NAME,
                        AamTokenTypes.SCHEMA_KEYWORD -> Indent.getNoneIndent()
                        else -> Indent.getNormalIndent()
                    }
                } else {
                    Indent.getNoneIndent()
                }
                blocks.add(
                    AamBlock(child, Wrap.createWrap(WrapType.NONE, false), null, mySpacingBuilder, childIndent)
                )
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent = myIndent

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        if (myNode.elementType == AamElementTypes.SCHEMA_DECLARATION) {
            val prev = subBlocks.getOrNull(newChildIndex - 1) as? AamBlock
            if (prev?.node?.elementType != AamTokenTypes.RBRACE) {
                return ChildAttributes(Indent.getNormalIndent(), null)
            }
        }
        return ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? =
        mySpacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = myNode.firstChildNode == null
}