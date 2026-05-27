package com.namey.domain.model

sealed class SuggestionResult {
    data class Success(val suggestions: List<NameSuggestion>) : SuggestionResult()
    data class Failure(val type: FailureType, val message: String) : SuggestionResult()

    enum class FailureType {
        API_KEY_MISSING,
        NETWORK_ERROR,
        RATE_LIMITED,
        PARSE_ERROR,
        UNKNOWN,
    }
}
