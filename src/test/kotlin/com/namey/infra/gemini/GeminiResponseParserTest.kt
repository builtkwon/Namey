package com.namey.infra.gemini

import com.namey.domain.model.SuggestionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeminiResponseParserTest {

    @Test
    fun `순수 JSON 배열을 정상 파싱한다`() {
        val raw = """[{"name":"findOrdersByUser","stars":3,"reason":"명확함"}]"""
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals("findOrdersByUser", result.suggestions[0].name)
        assertEquals(3, result.suggestions[0].stars)
    }

    @Test
    fun `마크다운 코드블록을 제거하고 파싱한다`() {
        val raw = "```json\n[{\"name\":\"getOrders\",\"stars\":2,\"reason\":\"짧고 명확\"}]\n```"
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals("getOrders", result.suggestions[0].name)
    }

    @Test
    fun `언어 표시 없는 코드블록도 파싱한다`() {
        val raw = "```\n[{\"name\":\"fetchOrders\",\"stars\":1,\"reason\":\"모호함\"}]\n```"
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals("fetchOrders", result.suggestions[0].name)
    }

    @Test
    fun `stars가 3 초과이면 3으로 클램핑한다`() {
        val raw = """[{"name":"doSomething","stars":5,"reason":"테스트"}]"""
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals(3, result.suggestions[0].stars)
    }

    @Test
    fun `stars가 0 미만이면 1로 클램핑한다`() {
        val raw = """[{"name":"doSomething","stars":0,"reason":"테스트"}]"""
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals(1, result.suggestions[0].stars)
    }

    @Test
    fun `stars 필드 누락 시 기본값 1을 사용한다`() {
        val raw = """[{"name":"doSomething","reason":"테스트"}]"""
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals(1, result.suggestions[0].stars)
    }

    @Test
    fun `name이 빈 문자열인 항목은 필터링된다`() {
        val raw = """[{"name":"","stars":3,"reason":"빈이름"},{"name":"validName","stars":2,"reason":"유효"}]"""
        val result = GeminiResponseParser.parse(raw)
        assertIs<SuggestionResult.Success>(result)
        assertEquals(1, result.suggestions.size)
        assertEquals("validName", result.suggestions[0].name)
    }

    @Test
    fun `빈 배열이면 PARSE_ERROR를 반환한다`() {
        val result = GeminiResponseParser.parse("[]")
        assertIs<SuggestionResult.Failure>(result)
        assertEquals(SuggestionResult.FailureType.PARSE_ERROR, result.type)
    }

    @Test
    fun `JSON이 없는 응답이면 PARSE_ERROR를 반환한다`() {
        val result = GeminiResponseParser.parse("죄송합니다. 추천을 생성할 수 없습니다.")
        assertIs<SuggestionResult.Failure>(result)
        assertEquals(SuggestionResult.FailureType.PARSE_ERROR, result.type)
    }
}
