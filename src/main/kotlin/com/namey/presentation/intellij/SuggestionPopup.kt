package com.namey.presentation.intellij

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.ui.components.JBLabel
import com.namey.domain.model.NameSuggestion
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JPanel

object SuggestionPopup {

    fun show(
        project: Project,
        editor: Editor,
        element: PsiElement,
        suggestions: List<NameSuggestion>,
        isOverride: Boolean = false,
    ) {
        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(suggestions)
            .setTitle(if (isOverride) "⚠️ 부모 메서드도 함께 변경됩니다" else "함수명 추천")
            .setRenderer(SuggestionCellRenderer())
            .setItemChosenCallback { chosen ->
                applyRename(project, element, chosen.name)
            }
            .setNamerForFiltering { "${it.name} ${it.reason}" }
            .createPopup()

        popup.showInBestPositionFor(editor)
    }

    private fun applyRename(project: Project, element: PsiElement, newName: String) {
        if (element !is PsiNamedElement) return
        RenameProcessor(project, element, newName, false, false).run()
    }

    private class SuggestionCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>, value: Any?, index: Int,
            isSelected: Boolean, cellHasFocus: Boolean,
        ): Component {
            val suggestion = value as? NameSuggestion ?: return super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus
            )

            val stars = "★".repeat(suggestion.stars) + "☆".repeat(3 - suggestion.stars)
            val panel = JPanel(BorderLayout(8, 0))
            panel.add(JBLabel("$stars  ${suggestion.name}"), BorderLayout.WEST)
            panel.add(JBLabel("<html><font color='gray'>${suggestion.reason}</font></html>"), BorderLayout.CENTER)

            if (isSelected) {
                panel.background = list.selectionBackground
                panel.foreground = list.selectionForeground
            } else {
                panel.background = list.background
            }
            panel.isOpaque = true
            return panel
        }
    }
}
