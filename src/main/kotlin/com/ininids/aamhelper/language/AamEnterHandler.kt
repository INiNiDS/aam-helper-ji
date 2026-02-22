package com.ininids.aamhelper.language

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile

class AamEnterHandler : EnterHandlerDelegate {

    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffsetRef: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): Result {
        if (file.language != AamLanguage) return Result.Continue

        val document = editor.document
        val caretOffset = caretOffsetRef.get()
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(
            com.intellij.openapi.util.TextRange(lineStart, caretOffset)
        )

        // Find the trimmed content of the current line up to caret
        val trimmed = lineText.trimStart()

        // If current line starts with '#', continue the comment on the next line
        if (trimmed.startsWith("#")) {
            val indent = lineText.substring(0, lineText.length - trimmed.length)
            val insertion = "\n${indent}# "
            document.insertString(caretOffset, insertion)
            editor.caretModel.moveToOffset(caretOffset + insertion.length)
            caretOffsetRef.set(caretOffset + insertion.length)
            return Result.Stop
        }

        return Result.Continue
    }

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): Result = Result.Continue
}

