package com.namey.infra.intellij

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.namey.domain.model.FunctionContext
import org.jetbrains.kotlin.psi.KtNamedFunction

class PsiCodeContextAdapter {

    fun extract(element: PsiElement): FunctionContext? = when (element) {
        is PsiMethod -> extractFromJava(element)
        is KtNamedFunction -> extractFromKotlin(element)
        else -> null
    }

    private fun extractFromJava(method: PsiMethod): FunctionContext? {
        if (method.isConstructor) return null

        val params = method.parameterList.parameters.map {
            FunctionContext.Parameter(it.name, it.type.presentableText)
        }

        val returnType = method.returnType?.presentableText ?: "void"
        val body = method.body?.text?.trim()
            ?.removePrefix("{")?.removeSuffix("}")?.trim()
            ?.takeIf { it.isNotBlank() }

        val parentInterface = if (method.findSuperMethods().isNotEmpty()) {
            method.findSuperMethods().firstOrNull()
                ?.containingClass?.name
        } else null

        return FunctionContext(
            currentName = method.name,
            parameters = params,
            returnType = returnType,
            body = body,
            description = null,
            containingClassName = method.containingClass?.name,
            parentInterface = parentInterface,
            receiverType = null,
            language = FunctionContext.Language.JAVA,
        )
    }

    private fun extractFromKotlin(function: KtNamedFunction): FunctionContext? {
        val params = function.valueParameters.map {
            FunctionContext.Parameter(
                it.name ?: "_",
                it.typeReference?.text ?: "Any"
            )
        }

        val returnType = function.typeReference?.text ?: "Unit"
        val body = function.bodyBlockExpression?.statements
            ?.joinToString("\n") { it.text }
            ?.takeIf { it.isNotBlank() }
            ?: function.bodyExpression?.text?.takeIf { it.isNotBlank() }

        val receiverType = function.receiverTypeReference?.text

        return FunctionContext(
            currentName = function.name ?: "unnamed",
            parameters = params,
            returnType = returnType,
            body = body,
            description = null,
            containingClassName = (function.parent as? org.jetbrains.kotlin.psi.KtClass)?.name,
            parentInterface = null,
            receiverType = receiverType,
            language = FunctionContext.Language.KOTLIN,
        )
    }
}
