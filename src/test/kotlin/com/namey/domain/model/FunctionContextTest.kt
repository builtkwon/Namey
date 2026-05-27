package com.namey.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FunctionContextTest {

    private fun context(body: String? = null, description: String? = null) = FunctionContext(
        currentName = "temp",
        parameters = emptyList(),
        returnType = "void",
        body = body,
        description = description,
        containingClassName = null,
        parentInterface = null,
        receiverType = null,
        language = FunctionContext.Language.JAVA,
    )

    @Test
    fun `바디만 있으면 hasContext는 true`() {
        assertTrue(context(body = "return 1;").hasContext())
    }

    @Test
    fun `설명만 있으면 hasContext는 true`() {
        assertTrue(context(description = "사용자 주문 조회").hasContext())
    }

    @Test
    fun `바디와 설명 모두 있으면 hasContext는 true`() {
        assertTrue(context(body = "return 1;", description = "주문 조회").hasContext())
    }

    @Test
    fun `바디와 설명 모두 null이면 hasContext는 false`() {
        assertFalse(context().hasContext())
    }

    @Test
    fun `바디와 설명이 공백만 있으면 hasContext는 false`() {
        assertFalse(context(body = "   ", description = "  ").hasContext())
    }

    @Test
    fun `파라미터 name과 type이 빈 문자열이어도 생성 가능`() {
        val param = FunctionContext.Parameter(name = "", type = "")
        val ctx = context().copy(parameters = listOf(param))
        assertTrue(ctx.parameters.isNotEmpty())
    }
}
