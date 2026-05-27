package com.namey.infra.gemini

import com.namey.domain.model.FunctionContext
import com.namey.domain.model.SuggestionResult
import com.namey.domain.port.NamingPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class GeminiNamingAdapter(private val apiKey: String) : NamingPort {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    override suspend fun suggest(context: FunctionContext, count: Int): SuggestionResult =
        withContext(Dispatchers.IO) {
            val prompt = buildPrompt(context, count)
            val body = buildRequestBody(prompt)

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$endpoint?key=$apiKey"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build()

            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                handleResponse(response)
            } catch (e: Exception) {
                SuggestionResult.Failure(
                    SuggestionResult.FailureType.NETWORK_ERROR,
                    "네트워크 오류. 연결을 확인해주세요: ${e.message}"
                )
            }
        }

    private fun handleResponse(response: HttpResponse<String>): SuggestionResult {
        return when (response.statusCode()) {
            200 -> {
                val text = extractTextFromResponse(response.body())
                    ?: return SuggestionResult.Failure(
                        SuggestionResult.FailureType.PARSE_ERROR,
                        "Gemini 응답 구조를 파싱할 수 없습니다."
                    )
                GeminiResponseParser.parse(text)
            }
            429 -> SuggestionResult.Failure(
                SuggestionResult.FailureType.RATE_LIMITED,
                "Gemini 요청 한도 초과. 잠시 후 다시 시도해주세요."
            )
            else -> SuggestionResult.Failure(
                SuggestionResult.FailureType.NETWORK_ERROR,
                "API 요청 실패 (상태 코드: ${response.statusCode()})"
            )
        }
    }

    private fun extractTextFromResponse(body: String): String? {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(body)
            root.jsonObject["candidates"]
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("text")
                ?.let { Json.decodeFromJsonElement(kotlinx.serialization.json.JsonPrimitive.serializer(), it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildRequestBody(prompt: String): String {
        val obj = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
        }
        return Json.encodeToString(obj)
    }

    private fun buildPrompt(ctx: FunctionContext, count: Int): String {
        val params = if (ctx.parameters.isEmpty()) "없음"
        else ctx.parameters.joinToString(", ") { "${it.name}: ${it.type}" }

        val optionalLines = buildString {
            ctx.receiverType?.let { appendLine("- 수신 타입: $it") }
            ctx.parentInterface?.let { appendLine("- 부모 인터페이스: $it") }
        }

        val bodySection = when {
            !ctx.body.isNullOrBlank() -> ctx.body
            !ctx.description.isNullOrBlank() -> ctx.description
            else -> "(없음)"
        }

        return """
당신은 Java/Kotlin 함수 네이밍 전문가입니다.
아래 함수에 적합한 이름 ${count}개를 추천해주세요.

규칙:
- 영문 camelCase, 반드시 동사로 시작
- 각 추천에 별점(1~3점)과 한국어 이유 한 줄 포함
- 반드시 아래 JSON 배열 형식으로만 응답 (다른 텍스트 절대 금지)

[{"name":"...", "stars":3, "reason":"..."}]

[함수 정보]
- 언어: ${ctx.language}
- 클래스: ${ctx.containingClassName ?: "없음"}
- 파라미터: $params
- 반환 타입: ${ctx.returnType}
$optionalLines- 바디/설명:
$bodySection
        """.trimIndent()
    }
}
