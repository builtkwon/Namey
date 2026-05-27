package com.namey.usecase

import com.namey.domain.model.FunctionContext
import com.namey.domain.model.NameSuggestion
import com.namey.domain.model.SuggestionResult
import com.namey.domain.port.NamingPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SuggestNameUseCaseTest {

    private val namingPort: NamingPort = mockk()
    private val useCase = SuggestNameUseCase(namingPort)

    private val baseContext = FunctionContext(
        currentName = "temp",
        parameters = listOf(FunctionContext.Parameter("userId", "Long")),
        returnType = "List<Order>",
        body = "return orderRepository.findByUserId(userId);",
        description = null,
        containingClassName = "OrderService",
        parentInterface = null,
        receiverType = null,
        language = FunctionContext.Language.JAVA,
    )

    @Test
    fun `NamingPort Success 응답을 그대로 반환한다`() = runTest {
        val expected = SuggestionResult.Success(
            listOf(NameSuggestion("findOrdersByUser", 3, "명확한 의미"))
        )
        coEvery { namingPort.suggest(any(), any()) } returns expected

        val result = useCase.execute(baseContext)

        assertIs<SuggestionResult.Success>(result)
        assertEquals(expected.suggestions, result.suggestions)
    }

    @Test
    fun `NamingPort Failure 응답을 그대로 반환한다`() = runTest {
        val expected = SuggestionResult.Failure(
            SuggestionResult.FailureType.NETWORK_ERROR, "네트워크 오류"
        )
        coEvery { namingPort.suggest(any(), any()) } returns expected

        val result = useCase.execute(baseContext)

        assertIs<SuggestionResult.Failure>(result)
        assertEquals(SuggestionResult.FailureType.NETWORK_ERROR, result.type)
    }

    @Test
    fun `description만 있는 컨텍스트도 NamingPort에 정상 전달된다`() = runTest {
        val ctx = baseContext.copy(body = null, description = "사용자의 주문 목록을 조회하는 함수")
        coEvery { namingPort.suggest(ctx, 3) } returns SuggestionResult.Success(emptyList())

        useCase.execute(ctx)

        coVerify { namingPort.suggest(ctx, 3) }
    }

    @Test
    fun `count 파라미터가 NamingPort에 전달된다`() = runTest {
        coEvery { namingPort.suggest(any(), 5) } returns SuggestionResult.Success(emptyList())

        useCase.execute(baseContext, count = 5)

        coVerify { namingPort.suggest(any(), 5) }
    }
}
