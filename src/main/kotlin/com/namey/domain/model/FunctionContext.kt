package com.namey.domain.model

data class FunctionContext(
    val currentName: String,
    val parameters: List<Parameter>,
    val returnType: String,
    val body: String?,
    val description: String?,
    val containingClassName: String?,
    val parentInterface: String?,
    val receiverType: String?,
    val language: Language,
) {
    data class Parameter(val name: String, val type: String)

    enum class Language { JAVA, KOTLIN }

    fun hasContext(): Boolean = !body.isNullOrBlank() || !description.isNullOrBlank()
}
