package com.namey.presentation.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.namey.domain.model.SuggestionResult
import com.namey.infra.gemini.GeminiNamingAdapter
import com.namey.infra.intellij.PsiCodeContextAdapter
import com.namey.usecase.SuggestNameUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class SuggestNameAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return

        // 람다/익명 함수 감지
        if (PsiTreeUtil.getParentOfType(element, KtLambdaExpression::class.java) != null) {
            toast(e, "람다/익명 함수는 지원하지 않습니다.")
            return
        }

        val javaMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        val kotlinFunction = PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java)

        when {
            javaMethod == null && kotlinFunction == null -> {
                toast(e, "함수 안에 커서를 놓아주세요.")
                return
            }
            javaMethod?.isConstructor == true -> {
                toast(e, "생성자는 지원하지 않습니다.")
                return
            }
        }

        val targetElement = (javaMethod ?: kotlinFunction)!!
        val adapter = PsiCodeContextAdapter()
        var context = adapter.extract(targetElement) ?: run {
            toast(e, "함수 정보를 추출할 수 없습니다.")
            return
        }

        val isOverride = javaMethod?.findSuperMethods()?.isNotEmpty() == true

        // 바디 없으면 설명 입력
        if (!context.hasContext()) {
            val desc = Messages.showInputDialog(
                project,
                "함수 바디가 없습니다. 이 함수의 역할을 설명해주세요.",
                "함수 설명 입력",
                Messages.getQuestionIcon()
            ) ?: return
            context = context.copy(description = desc)
        }

        // API Key 확인
        val apiKey = NameySettings.getInstance().apiKey
        if (apiKey.isBlank()) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, NameyConfigurable::class.java)
            return
        }

        val useCase = SuggestNameUseCase(GeminiNamingAdapter(apiKey))

        CoroutineScope(Dispatchers.IO).launch {
            val result = useCase.execute(context)
            withContext(Dispatchers.Main) {
                when (result) {
                    is SuggestionResult.Success ->
                        SuggestionPopup.show(project, editor, targetElement, result.suggestions, isOverride)

                    is SuggestionResult.Failure ->
                        Messages.showErrorDialog(project, result.message, "Namey 오류")
                }
            }
        }
    }

    private fun toast(e: AnActionEvent, message: String) {
        val project = e.project ?: return
        val frame = WindowManager.getInstance().getFrame(project) ?: return
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Namey")
            .createNotification(message, com.intellij.notification.NotificationType.WARNING)
            .notify(project)
    }
}
