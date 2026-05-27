package com.namey.presentation.intellij

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class NameyConfigurable : Configurable {

    private val apiKeyField = JBPasswordField()

    override fun getDisplayName(): String = "Namey"

    override fun createComponent(): JComponent {
        val settings = NameySettings.getInstance()
        apiKeyField.text = settings.apiKey

        val hint = JBLabel(
            "<html>Gemini API Key 무료 발급: " +
            "<a href='https://aistudio.google.com'>aistudio.google.com</a></html>"
        )

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent("Gemini API Key:", apiKeyField)
            .addComponentToRightColumn(hint)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return JPanel(BorderLayout()).apply { add(form) }
    }

    override fun isModified(): Boolean =
        String(apiKeyField.password) != NameySettings.getInstance().apiKey

    override fun apply() {
        NameySettings.getInstance().apiKey = String(apiKeyField.password)
    }

    override fun reset() {
        apiKeyField.text = NameySettings.getInstance().apiKey
    }
}
