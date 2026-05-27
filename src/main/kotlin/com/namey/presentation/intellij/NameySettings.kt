package com.namey.presentation.intellij

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "NameySettings", storages = [Storage("namey.xml")])
class NameySettings : PersistentStateComponent<NameySettings.State> {

    data class State(var dummy: String = "")

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    var apiKey: String
        get() = PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
        set(value) = PasswordSafe.instance.setPassword(credentialAttributes, value)

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("Namey", "GeminiApiKey")
    )

    companion object {
        fun getInstance(): NameySettings =
            ApplicationManager.getApplication().getService(NameySettings::class.java)
    }
}
