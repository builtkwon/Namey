package com.namey.infra.gemini

import com.namey.domain.model.NameSuggestion
import com.namey.domain.model.SuggestionResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object GeminiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class RawSuggestion(
        val name: String = "",
        val stars: Int = 1,
        val reason: String = "",
    )

    fun parse(raw: String): SuggestionResult {
        val jsonText = extractJson(raw)
            ?: return SuggestionResult.Failure(
                SuggestionResult.FailureType.PARSE_ERROR,
                "AI 응답에서 JSON을 찾을 수 없습니다."
            )

        return try {
            val items = json.decodeFromString<List<RawSuggestion>>(jsonText)
            val suggestions = items
                .filter { it.name.isNotBlank() }
                .map { NameSuggestion(it.name, it.stars.coerceIn(1, 3), it.reason) }

            if (suggestions.isEmpty()) {
                SuggestionResult.Failure(
                    SuggestionResult.FailureType.PARSE_ERROR,
                    "AI가 추천을 생성하지 못했습니다. 함수 바디를 보완해주세요."
                )
            } else {
                SuggestionResult.Success(suggestions)
            }
        } catch (e: Exception) {
            SuggestionResult.Failure(
                SuggestionResult.FailureType.PARSE_ERROR,
                "응답 파싱 실패: ${e.message}"
            )
        }
    }

    // 순수 JSON 배열 또는 ```json ... ``` 블록 모두 처리
    private fun extractJson(raw: String): String? {
        val trimmed = raw.trim()

        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val match = codeBlockRegex.find(trimmed)
        if (match != null) return match.groupValues[1].trim()

        val arrayStart = trimmed.indexOf('[')
        val arrayEnd = trimmed.lastIndexOf(']')
        if (arrayStart != -1 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1)
        }

        return null
    }
}
